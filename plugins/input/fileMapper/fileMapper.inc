<div style="position:relative;">
	<!-- Top-level modal overlay -->
	<div id="${id}_overlay" class="overlay">
		<div id="${id}_overlay_spinner"></div>
	</div>
	<table>
		<tr>
			<td colspan="2" style="text-align:center;">
				<input type="button" id="${id}_populator"
					value="Start Associating Files"/>
			</td>
		</tr>
		<tr style="text-align:center;font-weight:bold">
			<td>Filenames in "Peak List Files"</td>
			<td>Filenames in "Result Files"</td>
		</tr>
		<tr style="height:100px;">
			<td class="selector-box">
				<ol id="${id}_spectra" class="selector singleSelectable"></ol>
			</td>
			<td class="selector-box">
				<div style="position:relative;">
					<ol id="${id}_results" class="selector multipleSelectable"></ol>
					<!-- Checkmark modal overlay -->
					<div id="${id}_checkmark" class="overlay"
						style="background-color:#FFFFFF;">
						<div style="height:100%;display:inline-block;vertical-align:middle"></div>
						<img src="images/icons/checkmark.png"></img>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td colspan="2" style="text-align:center;">
				<input type="button" id="${id}_assigner"
					value="Associate Selected Files with Each Other"/>
			</td>
		</tr>
		<tr>
			<td colspan="2" style="text-align:center;font-weight:bold">
				Mapped Filenames
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<div id="${id}_assignments" class="selector-box"
					style="height:100px;overflow-y:auto;"></div>
			</td>
		</tr>
	</table>
	<input type="hidden" name="${id}"/>
</div>