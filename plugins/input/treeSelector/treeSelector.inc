<div style="width:410px; position:absolute; top:28px; left:2px;">
	<table class="mainform">
		<tr style="white-space:nowrap;">
			<th style="width:63%; text-align:right;">Select Input Files</th>
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
			<td id="${id}_fileSelectors" style="float: right;"></td>
		</tr>
		<tr>
			<td colspan="2" style="max-width:396px;">
				<div id="${id}_folderTree"></div>
			</td>
		</tr>
		<tr><td class="folderBottom" colspan="2"/></tr>
	</table>
</div>
<div style="width:400px; position:absolute; top:32px; left:420px;">
	<table id="${id}_selectionTrees" class="mainform">
		<tr style="white-space:nowrap;"><th colspan="2">Selected Files</th></tr>
		<!-- Splice in selection tree rows here -->
		<tr>
			<td align="center" class="folderBottom" colspan="2">
				<button style="width:150px; vertical-align:baseline;"
					onclick="ProteoSAFeResourceUtils.clearAllSelections();">
					Clear Selection
				</button>
				<button style="width:150px; vertical-align:baseline;"
					onclick="ProteoSAFeResourceUtils.blurResourceWindow();">
					Finish Selection
				</button>
			</td>
		</tr>
	</table>
</div>
<div class="helpbox" id="${id}_hUpload" style="left:-5000px;">
	<div class="helpbox-main">
		This folder-tree displays files previously uploaded to your account.
	</div>
	<div class="helpbox-bottom">&nbsp;</div>
</div>
