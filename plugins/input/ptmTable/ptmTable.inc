<table id="${id}_PTMTable" border="0" width="80%" class="ptm" align="center">
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
	<!-- Custom PTM addition UI goes here -->
</table>
<div class="helpbox" id="hResidues" style="left:-5000px;">
	Residues must be a string of abbreviation letters for standard amino acid
	(i.e. <i>ACDEFGHIKLMNOPQRSTUVWY</i>). Fill in this field with an asterisk
	<i>*</i> if any residue will fit in your experiment.
</div>
