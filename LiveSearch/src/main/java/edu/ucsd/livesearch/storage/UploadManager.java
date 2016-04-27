package edu.ucsd.livesearch.storage;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.livesearch.util.FormatUtils;

public class UploadManager
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(UploadManager.class);
	private static final long UPLOAD_TIMEOUT = 120000;		// 2 minutes
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	// upload token -> list of pending uploads in that token's batch
	private static Map<String, List<PendingUpload>> pendingUploads;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final boolean queueUpload(
		File file, String id, String token, String user, int fileSize
	) {
		if (file == null || id == null || id.trim().equals("") ||
			token == null || token.trim().equals("") ||
			user == null || FileManager.syncFTPSpace(user) == false ||
			user.equals(getUploadUser(token)) == false)
			return false;
		// verify ownership
		File parent = file.getParentFile();
		if (FileManager.isOwned(parent, user) == false) {
			logger.error("Error queuing file \"" + file.getAbsolutePath() +
				"\": Parent folder is not owned by user \"" + user + "\".");
			return false;
		}
		// create pending upload record for this file
		PendingUpload upload = new PendingUpload(id, file, fileSize);
		// insert this record into the queue
		addUpload(token, upload);
		logger.info("File \"" + file.getAbsolutePath() + "\" is now " +
			"uploading, and is being added to the pending upload queue.");
		return true;
	}
	
	public static final void finishUpload(File file) {
		PendingUpload upload = getUploadByFile(file);
		if (upload == null)
			return;
		// the upload only needs to be completed if it isn't already
		if (upload.isCompleted() == false && removeUpload(upload)) {
			StringBuffer message = new StringBuffer("File \"" +
				upload.getPath() + "\" is now finished uploading, " +
				"and has been removed from the pending upload queue.");
			// now that a pending upload has finished, poll the task queue
			// TODO: might want to use something like an observer
			// pattern to reduce coupling here
			List<Task> launchedTasks = WorkflowUtils.pollQueue();
			if (launchedTasks != null && launchedTasks.isEmpty() == false) {
				message.append(
					"\nThe following tasks are now ready to be run:");
				for (Task task : launchedTasks) {
					message.append("\n  ");
					message.append(task.getFlowName());
					message.append(" [");
					message.append(task.getID());
					message.append("] - ");
					message.append(task.getDescription());
				}
			}
			logger.info(message.toString());
		}
	}
	
	public static final boolean progressUpload(
		String id, String token, int bytesUploaded
	) {
		if (id == null || token == null)
			return false;
/**/
//logger.info("Calling UploadManager.progressUpload(id = \"" + id +
//	"\", token = \"" + token + "\", bytesUploaded = " + bytesUploaded + ");");
/**/
		PendingUpload upload = getUploadById(id, token);
		if (upload == null)
			return false;
		else {
			upload.setBytesUploaded(bytesUploaded);
			return true;
		}
	}
	
	public static final void cancelUpload(String id, String token) {
		if (id == null || token == null)
			return;
		cancelUpload(getUploadById(id, token), getUploadUser(token));
	}
	
	public static final void cancelUpload(PendingUpload upload, String user) {
		if (upload == null || user == null)
			return;
		// the upload only needs to be canceled if it isn't already completed
		if (upload.isCompleted() == false && removeUpload(upload)) {
			String path = upload.getPath();
			StringBuffer message = new StringBuffer("Upload of file \"" +
				path + "\" has been cancelled, and has been removed " +
				"from the pending upload queue.");
			// notify the task queue of this upload cancellation
			// TODO: might want to use something like an observer
			// pattern to reduce coupling here
			List<Task> canceledTasks = WorkflowUtils.cancelUpload(path);
			if (canceledTasks != null && canceledTasks.isEmpty() == false) {
				message.append("\nThe following tasks have failed " +
					"due to this upload cancellation:");
				for (Task task : canceledTasks) {
					message.append("\n  ");
					message.append(task.getFlowName());
					message.append(" [");
					message.append(task.getID());
					message.append("] - ");
					message.append(task.getDescription());
				}
			}
			logger.info(message.toString());
			// attempt to delete this upload's dummy file
			FileManager.deleteFile(user, upload.getFile());
		}
	}
	
	public static final void pollQueue() {
		if (pendingUploads == null)
			return;
		// reorganize uploads by guid, since an upload batch is defined
		// to be all uploads that are associated with a particular guid;
		// there may be many upload tokens from a single guid
		Map<String, String> guids = new HashMap<String, String>();
		for (String token : pendingUploads.keySet()) {
			String guid = getUploadGuid(token);
			String user = getUploadUser(token);
			if (guid != null && user != null)
				guids.put(guid, user);
		}
		// cancel all uploads or upload batches that have timed out
		for (String guid : guids.keySet()) {
			Set<PendingUpload> uploads = getUploadsByGuid(guid);
			if (uploads == null || uploads.isEmpty())
				continue;
			else if (isUploadBatchExpired(guid)) {
				for (PendingUpload upload : uploads)
					cancelUpload(upload, guids.get(guid));
			} else for (PendingUpload upload : uploads) {
				if (isUploadExpired(upload))
					cancelUpload(upload, guids.get(guid));
			}
		}
	}
	
	public static final PendingUpload getUploadByFile(File file) {
		if (file == null)
			return null;
		return getUploadByPath(file.getAbsolutePath());
	}
	
	public static final PendingUpload getUploadByPath(String path) {
		if (path == null || pendingUploads == null)
			return null;
		// search through pending uploads, look for an upload with this path
		for (String token: pendingUploads.keySet()) {
			List<PendingUpload> uploads = pendingUploads.get(token);
			if (uploads != null && uploads.isEmpty() == false)
				for (PendingUpload upload : uploads)
					if (path.equals(upload.getPath()))
						return upload;
		}
		return null;
	}
	
	public static final PendingUpload getUploadById(String id, String token) {
		if (id == null || token == null || pendingUploads == null)
			return null;
		// search through pending uploads with this token
		List<PendingUpload> uploads = pendingUploads.get(token);
		if (uploads == null || uploads.isEmpty())
			return null;
		// look for an upload with this id
		else for (PendingUpload upload : uploads)
			if (id.equals(upload.getId()))
				return upload;
		return null;
	}
	
	public static final Set<PendingUpload> getUploadsByGuid(String guid) {
		if (guid == null || pendingUploads == null)
			return null;
		Set<PendingUpload> foundUploads = new HashSet<PendingUpload>();
		// search through pending uploads, look for uploads with this guid
		for (String token: pendingUploads.keySet())
			if (guid.equals(getUploadGuid(token)))
				foundUploads.addAll(pendingUploads.get(token));
		// return all matching uploads found
		if (foundUploads.isEmpty())
			return null;
		else return foundUploads;
	}
	
	public static final String getUploadToken(String user, String guid) {
		if (user == null || guid == null)
			return null;
		else return String.format(
			"%s-%s-%d", user, guid, System.currentTimeMillis());
	}
	
	public static final String getUploadUser(String token) {
		if (token == null)
			return null;
		int dash = token.indexOf("-");
		if (dash < 1)
			return null;
		else return token.substring(0, dash);
	}
	
	public static final String getUploadGuid(String token) {
		if (token == null)
			return null;
		// keep everything after first dash but before second dash
		int firstDash = token.indexOf("-");
		if (firstDash < 1)
			return null;
		int secondDash = token.substring(firstDash + 1).indexOf("-");
		if (secondDash < 1)
			return null;
		else return token.substring(firstDash + 1, secondDash);
	}
	
	public static final boolean isTokenValid(String token, String user) {
		if (token == null || user == null || pendingUploads == null)
			return false;
		// confirm that this token belongs to this user
		if (user.equals(getUploadUser(token)) == false)
			return false;
		// confirm that the upload set corresponding
		// to this token belongs to this user
		List<PendingUpload> uploads = pendingUploads.get(token);
		if (uploads == null || uploads.isEmpty())
			return false;
		else for (PendingUpload upload : uploads)
			if (FileManager.isOwned(upload.getFile(), user) == false)
				return false;
		return true;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static void addUpload(String token, PendingUpload upload) {
		if (token == null || upload == null)
			return;
		if (pendingUploads == null)
			pendingUploads = new HashMap<String, List<PendingUpload>>();
		List<PendingUpload> uploads = pendingUploads.get(token);
		if (uploads == null)
			uploads = new Vector<PendingUpload>();
		uploads.add(upload);
		pendingUploads.put(token, uploads);
	}
	
	private static boolean removeUpload(PendingUpload upload) {
		if (upload == null)
			return false;
		upload.setCompleted(true);
		return true;
	}
	
	private static boolean isUploadExpired(PendingUpload upload) {
		if (upload == null)
			return false;
		// an upload that is completed is not considered expired
		else if (upload.isCompleted())
			return false;
		// an upload that has not yet started is not considered expired
		else if (upload.getPercentUploaded() <= 0.0)
			return false;
		// an upload is only considered expired if it has started, but
		// hasn't yet finished, and has stalled for more than the timeout
		boolean expired = (System.currentTimeMillis() - upload.getLastUpdated())
			> UPLOAD_TIMEOUT;
		if (expired) {
			StringBuffer message = new StringBuffer("Upload [");
			message.append(upload.dump());
			message.append("] has expired - its upload progress has started, ");
			message.append("but hasn't advanced within the last ");
			message.append(FormatUtils.formatShortTimePeriod(UPLOAD_TIMEOUT));
			message.append(".");
			logger.info(message.toString());
		}
		return expired;
	}
	
	// An upload batch is considered expired only if all of its uploads
	// are stalled, i.e. none of them have any upload progress, and the
	// earliest queued upload has timed out.  If any of the uploads are
	// individually expired, then the rest of the batch is still considered
	// potentially salvageable.
	private static boolean isUploadBatchExpired(String guid) {
		if (guid == null)
			return false;
		Set<PendingUpload> uploads = getUploadsByGuid(guid);
		if (uploads == null || uploads.isEmpty())
			return false;
		PendingUpload firstQueued = null;
		for (PendingUpload upload : uploads) {
			if (isUploadExpired(upload)) {
				// if this is the only upload in this batch,
				// then the batch is expired
				if (uploads.size() == 1) {
					StringBuffer message = new StringBuffer("Upload batch \"");
					message.append(guid);
					message.append(
						"\" has expired, since its only upload has expired:");
					message.append("\n\t");
					message.append(upload.dump());
					logger.info(message.toString());
					return true;
				}
				// however, if there are other uploads in this batch,
				// then we don't say that the whole batch is expired,
				// only this one upload is
				else return false;
			}
			// if any individual upload has some progress,
			// but is not expired, then the batch is good
			else if (upload.getPercentUploaded() > 0.0)
				return false;
			// if this upload has no progress, and it's the
			// earliest queued upload seen so far, then note it
			else if (firstQueued == null ||
				firstQueued.getLastUpdated() > upload.getLastUpdated())
				firstQueued = upload;
		}
		// if no upload in this batch has progress, then the batch might
		// be stalled, so check the earliest queued upload for timeout
		boolean expired = (System.currentTimeMillis() -
			firstQueued.getLastUpdated()) > UPLOAD_TIMEOUT;
		if (expired) {
			StringBuffer message = new StringBuffer("Upload batch \"");
			message.append(guid);
			message.append(
				"\" has expired, since its first queued upload has expired:");
			message.append("\n\t");
			message.append(firstQueued.dump());
			logger.info(message.toString());
		}
		return expired;
	}
	
	/*========================================================================
	 * Convenience classes
	 *========================================================================*/
	public static class PendingUpload
	{
		/*====================================================================
		 * Properties
		 *====================================================================*/
		private String id;
		private File file;
		private int fileSize;
		private int bytesUploaded;
		private long lastUpdated;
		private boolean completed;
		
		/*====================================================================
		 * Constructors
		 *====================================================================*/
		public PendingUpload() {
			this(null, null, 0);
		}
		
		public PendingUpload(String id, File file, int fileSize) {
			setId(id);
			setFile(file);
			setFileSize(fileSize);
			setBytesUploaded(0);
			lastUpdated = System.currentTimeMillis();
			completed = false;
		}
		
		/*====================================================================
		 * Property accessor methods
		 *====================================================================*/
		public final String getId() {
			return id;
		}
		
		public final void setId(String id) {
			this.id = id;
		}
		
		public final File getFile() {
			return file;
		}
		
		public final void setFile(File file) {
			this.file = file;
		}
		
		public final String getName() {
			File file = getFile();
			if (file == null)
				return null;
			else return file.getName();
		}
		
		public final String getPath() {
			File file = getFile();
			if (file == null)
				return null;
			else return file.getAbsolutePath();
		}
		
		public final int getFileSize() {
			return fileSize;
		}
		
		public final void setFileSize(int fileSize) {
			if (fileSize < 0)
				this.fileSize = 0;
			else this.fileSize = fileSize;
		}
		
		public final int getBytesUploaded() {
			return bytesUploaded;
		}
		
		public final void setBytesUploaded(int bytesUploaded) {
			if (bytesUploaded < 0)
				this.bytesUploaded = 0;
			else {
				int fileSize = getFileSize();
				if (bytesUploaded > fileSize)
					this.bytesUploaded = fileSize;
				else this.bytesUploaded = bytesUploaded;
			}
			lastUpdated = System.currentTimeMillis();
		}
		
		public final long getLastUpdated() {
			return lastUpdated;
		}
		
		public final boolean isCompleted() {
			return completed;
		}
		
		public final void setCompleted(boolean completed) {
			this.completed = completed;
		}
		
		public final String getTimeSinceLastUpdate() {
			return FormatUtils.formatShortTimePeriod(
				System.currentTimeMillis() - lastUpdated);
		}
		
		public final double getPercentUploaded() {
			int bytesUploaded = getBytesUploaded();
			int fileSize = getFileSize();
			if (fileSize <= 0 || bytesUploaded >= fileSize)
				return 100.0;
			else return Math.ceil(
				((double)bytesUploaded / (double)fileSize) * 100.0);
		}
		
		public final String dump() {
			StringBuffer dump = new StringBuffer(getId());
			dump.append(" (");
			dump.append(getName());
			dump.append(") : ");
			dump.append(String.format("%.2f", getPercentUploaded()));
			dump.append("% uploaded, ");
			dump.append(getTimeSinceLastUpdate());
			dump.append(" since last update");
			return dump.toString();
		}
	}
}
