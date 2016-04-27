<div style="width:410px; position:absolute; top:28px; left:2px;">
	<table class="mainform">
		<tr style="white-space:nowrap;">
			<th style="width:63%; text-align:right;">Select Upload Folder</th>
			<th class="inner">
				<span class="help"
					onmouseover="showTooltip(this, event, 'load:${id}_hUpload');"
					style="float:right;">
					<img src="images/help.png"/>
				</span>
			</th>
		</tr>
		<tr>
			<td style="text-align:center; width:80%">
				<button id="${id}_createButton" title="New Folder"
					style="width:60px;">
					<img src="images/folder.png" alt="New Folder"/>
				</button>
				<button id="${id}_renameButton" title="Rename Selected"
					style="width:60px;">
					<img src="images/rename.png" alt="Rename Selected"/>
				</button>
				<button id="${id}_deleteButton" title="Delete Selected"
					style="width:60px;">
					<img src="images/delete.png" alt="Delete Selected"/>
				</button>
			</td>
			<td/>
		</tr>
		<tr>
			<td colspan="2" style="max-width:396px;">
				<div id="${id}_folderTree"></div>
			</td>
		</tr>
		<tr><td class="folderBottom" colspan="2"/></tr>
		<tr style="white-space:nowrap;">
			<th style="text-align:center;" colspan="2">Uploading Files</th>
		</tr>
		<tr>
			<td style="padding:0 40px;" colspan="2">
				<br/>
				For most file uploads, FTP is recommended. Please see
				<a href="https://bix-lab.ucsd.edu/display/PS/ProteoSAFe+FTP"
					target="_blank" style="color:blue;">here</a>
				for detailed instructions on how to upload your files to
				ProteoSAFe via FTP.<br/><br/>
				However, for small files (20 MB or less), a drag-and-drop file
				uploader is provided to the right for your convenience.
				Please see
				<a href="https://bix-lab.ucsd.edu/display/Public/Drag+And+Drop+File+Upload"
					target="_blank" style="color:blue;">here</a>
				for detailed instructions on how to use this web uploader.
			</td>
		</tr>
		<tr><td class="folderBottom" colspan="2"/></tr>
	</table>
</div>
<div style="width:400px; position:absolute; top:32px; left:420px;">
	<table id="${id}_uploader" class="mainform">
		<tr style="white-space:nowrap;">
			<th colspan="2">
				<span id="${id}_statusHeader"></span>
				<span id="${id}_divStatus">File Drag and Drop</span>
			</th>
		</tr>
		<!--
		<tr>
			<td colspan="2">
				<span id="${id}_spanButtonPlaceHolder"></span>
				<button id="${id}_btnCancel" disabled="disabled">
					Cancel All Uploads
				</button>
			</td>
		</tr>
		<tr>
			<td valign="top" colspan="2">
				<div id="${id}_uploadWarning"
					style="color: red; padding-bottom: 10px;"></div>
				<div class="fieldset flash" id="${id}_fsUploadProgress">
					<span class="legend">Upload Queue</span>
				</div>
			</td>
		</tr>
		-->
	</table>
</div>
<div class="helpbox" id="${id}_hUpload" style="left:-5000px;">
	<div class="helpbox-main">
		This folder-tree displays files previously uploaded to your account.
	</div>
	<div class="helpbox-bottom">&nbsp;</div>
</div>
