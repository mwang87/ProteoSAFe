<div style="width:410px; position:absolute; top:28px; left:2px;">
	<table class="mainform">
		<tr style="white-space:nowrap;">
			<th style="width:63%; text-align:center;">Share Files With User</th>
			<th class="inner">
				<span class="help"
					onmouseover="showTooltip(this, event, 'load:${id}_hShare');"
					style="float:right;">
					<img src="images/help.png"/>
				</span>
			</th>
		</tr>
		<tr>
			<td style="text-align:center;" colspan="2">
				<form name="${id}_sharingForm">
					<input name="user" type="text"/>
					<input id="${id}_sharingButton" value="Share"
						type="button"/>
				</form>
			</td>
		</tr>
		<tr><td class="folderBottom" colspan="2"/></tr>
		<tr style="white-space:nowrap;">
			<th style="text-align:center;">Import Data Share</th>
			<th class="inner">
				<span class="help"
					onmouseover="showTooltip(this, event, 'load:${id}_hImport');"
					style="float:right;">
					<img src="images/help.png"/>
				</span>
			</th>
		</tr>
		<tr>
			<td style="text-align:center;" colspan="2">
				<form name="${id}_importForm">
					<input name="user" type="text"/>
					<input id="${id}_importButton" value="Import"
						type="button"/>
				</form>
			</td>
		</tr>
		<tr><td class="folderBottom" colspan="2"/></tr>
	</table>
</div>
<div style="width:400px; position:absolute; top:32px; left:420px;">
	<table class="mainform">
		<tr style="white-space:nowrap;"><th>Shared Users</th></tr>
		<tr><td><div id="${id}_sharedUsers"></div></td></tr>
		<tr><td class="folderBottom"/></tr>
		<tr style="white-space:nowrap;"><th>Imported Data Shares</th></tr>
		<tr><td><div id="${id}_importedUsers"></div></td></tr>
		<tr><td class="folderBottom"/></tr>
	</table>
</div>
<div class="helpbox" id="${id}_hShare" style="left:-5000px;">
	<div class="helpbox-main">
		Enter a valid ProteoSAFe user ID to share access to your files
		with that user.
	</div>
	<div class="helpbox-bottom">&nbsp;</div>
</div>
<div class="helpbox" id="${id}_hImport" style="left:-5000px;">
	<div class="helpbox-main">
		Enter a valid MassIVE dataset ID to import its contents into your
		searchable user space.
	</div>
	<div class="helpbox-bottom">&nbsp;</div>
</div>
