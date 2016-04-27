/**
 * Returns new spectrum that has been adjusted
 *
**/
function sqrt_normalize_spectrum(spectrum){
    output_spectrum = new Array()
    
    accumulated_norm = 0.0
    for(i in spectrum){
        sqrt_value = Math.sqrt(spectrum[i][1])
        output_spectrum.push([spectrum[i][0], sqrt_value])
        accumulated_norm += spectrum[i][1]
    }
    
    normed_valued = Math.sqrt(accumulated_norm)
    for(i in output_spectrum){
        output_spectrum[i][1] = output_spectrum[i][1]/normed_valued
    }
    
    return output_spectrum
}


/**
 * Scores the alignment between two spectra
 *
**/
function score_alignement(spec1_input, spec2_input, pm_1, pm_2, tolerance){
    spec1_intermediate = sqrt_normalize_spectrum(spec1_input)
    spec2_intermediate = sqrt_normalize_spectrum(spec2_input)

    shift = pm_1 - pm_2
    
    zero_shift_alignments = FindMatchPeaksAll2(spec1_intermediate, spec2_intermediate, 0, tolerance)
    real_shift_alignments = FindMatchPeaksAll2(spec1_intermediate, spec2_intermediate, shift, tolerance)
    
    //Array of all possible match scores
    all_possible_match_scores = new Array();
    reported_alignments = new Array();
    
    //Maps to determine if peaks have been used
    spec1_peak_used = new Object();
    spec2_peak_used = new Object();
    
    for(i in zero_shift_alignments){
        match_score = spec1_intermediate[zero_shift_alignments[i][0]][1] * spec2_intermediate[zero_shift_alignments[i][1]][1]
        match_object = new Object();
        match_object.spec1_peak = zero_shift_alignments[i][0]
        match_object.spec2_peak = zero_shift_alignments[i][1]
        match_object.score = match_score
        
        all_possible_match_scores.push(match_object)
    }
    
    for(i in real_shift_alignments){
        match_score = spec1_intermediate[real_shift_alignments[i][0]][1] * spec2_intermediate[real_shift_alignments[i][1]][1]
        match_object = new Object();
        match_object.spec1_peak = real_shift_alignments[i][0]
        match_object.spec2_peak = real_shift_alignments[i][1]
        match_object.score = match_score
        
        all_possible_match_scores.push(match_object)
    }
    
    //Sorting the possible matches in descending order
    all_possible_match_scores.sort(function(a, b){return b.score-a.score});
    
    //Iterating through all match scores
    total_score = 0.0
    for(i in all_possible_match_scores){
        match_object = all_possible_match_scores[i]
        
        if(match_object.spec1_peak in spec1_peak_used || match_object.spec2_peak in spec2_peak_used){
            //At least one of the peaks is used
        }
        else{
            spec1_peak_used[match_object.spec1_peak] = 1
            spec2_peak_used[match_object.spec2_peak] = 1
            reported_alignments.push([match_object.spec1_peak, match_object.spec2_peak])
            
            total_score += match_object.score
        }
    }
    
    return_object = new Object()
    return_object.score = total_score
    return_object.alignments = reported_alignments
    
    return return_object
}

/**
 * Finding all possible matches from spec1 to spec2
 *
**/
function FindMatchPeaksAll2(spec1, spec2, shift, tolerance){
    var low = 0, high = 0; // Index bounds of the peaks in spec2 the lie within tolerance of current peak in spec1

    idx1 = new Array();
    idx2 = new Array();
    
    alignment_mapping = new Array();
    
    for (i = 0; i < spec1.length; i++)
    {
        low = spec2.length - 1
        while (low > 0 && (spec1[i][0] - tolerance - 0.000001) < (spec2[low][0] + shift)){
            low--;
        }
        while ((low < spec2.length) && (spec1[i][0] - tolerance - 0.000001) > (spec2[low][0] + shift)){
            low++;
        }
        while ((high < spec2.length) && (spec1[i][0] + tolerance + 0.000001) >= (spec2[high][0] + shift)){
            high++;  // high is index of first unreachable peak
        }
        
        for (j = low; j < high; j++){
            idx1.push(i);
            idx2.push(j);
            alignment_mapping.push([i,j])
        }
    }
    
    return alignment_mapping
}