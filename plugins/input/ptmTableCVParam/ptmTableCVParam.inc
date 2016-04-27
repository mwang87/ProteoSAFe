<table id="${id}_PTMTableCVParam" class="ptm"
	style="border:0;width:80%;text-align:center;">
	<tr>
		<th/>
		<th>Mass (<i>Da</i>)</th>
		<th>
			<div class="help"
				onmouseover="showTooltip(this, event, 'load:hResidues');">
				Residues:
			</div>
		</th>
		<th>Type</th>
	</tr>
	<!-- Fixed PTM radio button fields go here -->
</table>
<div class="helpbox" id="hResidues" style="left:-5000px;">
	Residues must be a string of abbreviation letters for standard amino acids
	(i.e. <i>ACDEFGHIKLMNOPQRSTUVWY</i>). Fill in this field with an asterisk
	<i>*</i> if any residue will fit in your experiment.
</div>
