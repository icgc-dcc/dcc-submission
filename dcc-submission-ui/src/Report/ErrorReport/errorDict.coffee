# from schema_report_error_table_view.coffee L42-313

hljs = require('highlight.js');
js_beautify = require('js-beautify').js_beautify;

module.exports = 
    PCAWG_DONOR_MISSING:
      name: "PanCancer donor missing"
      description: (source) ->
        """
        The following donors have data submitted to PCAWG, but have not been included in this DCC submission.
         See <a href="http://docs.icgc.org/submission/pcawg/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_SPECIMEN_MISSING:
      name: "PanCancer specimen missing"
      description: (source) ->
        """
        The following specimen have data submitted to PCAWG, but have not been included in this DCC submission.
         See <a href="http://docs.icgc.org/submission/pcawg/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_SPECIMEN_TYPE_INVALID:
      name: "PanCancer specimen type invalid"
      description: (source) ->
        """
        Specimen type has an inconsistent value with respect to PCAWG.
         See <a href="http://docs.icgc.org/submission/pcawg/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_SAMPLE_MISSING:
      name: "PanCancer sample missing"
      description: (source) ->
        """
        The following samples have data submitted to PCAWG, but have not been included in this DCC submission.
         See <a href="http://docs.icgc.org/submission/pcawg/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_SAMPLE_STUDY_MISSING:
      name: "PanCancer sample study missing"
      description: (source) ->
        """
        Sample data submitted to PCAWG, however it is not marked as in PCAWG study in this DCC submission.
         See <a href="http://docs.icgc.org/submission/pcawg/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_SAMPLE_STUDY_INVALID:
      name: "PanCancer sample study invalid"
      description: (source) ->
        """
        Sample is marked as in PCAWG study in this submission, however it does not actually exist in PCAWG.
         See <a href="http://docs.icgc.org/submission/pcawg/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    LINE_TERMINATOR_MISSING_ERROR:
      name: "Missing line terminators"
      description: (source) ->
        """
        Lines must terminated with <code>\\n</code>.
        """
    UNSUPPORTED_COMPRESSED_FILE:
      name: "Unsupported compressed file"
      description: (source) ->
        """
        The compressed file should not be concatenated or the block header is corrupted
        """
    CODELIST_ERROR:
      name: "Invalid value"
      description: (source) ->
        """
        Values do not match any of the allowed values for this field
        """
    DISCRETE_VALUES_ERROR:
      name: "Invalid value"
      description: (source) ->
        """
        Values do not match any of the following allowed values for
        this field: <code>#{source.parameters?.EXPECTED}</code>
        """
    REGEX_ERROR:
      name: "Invalid value"
      description: (source) ->
        """
        Values do not match the regular expression set for
        this field: <code>#{source.parameters?.EXPECTED}</code>
        """
    SCRIPT_ERROR:
      name: "Failed script-based validation"
      description: (source) ->

        # Note we don't have an mvel formatter/highlighter, this is
        # currently simulated with javascript formatter and java highlighter
        errorRaw = source.parameters?.EXPECTED
        errorPretty = hljs.highlight('java', js_beautify(errorRaw)).value

        """
        Data row failed script-based validation check, see
        <a href="http://docs.icgc.org/" target="_blank">
        submission documentation</a> for details.
        <br><br><p>#{source.parameters?.DESCRIPTION}</p><pre><code>#{errorPretty}</code></pre>
        """

    DUPLICATE_HEADER_ERROR:
      name: "Duplicate field name"
      description: (source) ->
        """
        Duplicate field names found in the file header
        <code>#{source.parameters?.FIELDS}</code>
        """
    RELATION_FILE_ERROR:
      name: "Required file missing"
      description: (source) ->
        """
        A <code>#{source.parameters?.SCHEMA}</code> file is missing
        """
    REVERSE_RELATION_FILE_ERROR:
      name: "Required file missing"
      description: (source) ->
        """
        A <code>#{source.parameters?.SCHEMA}</code> file is missing
        """
    RELATION_VALUE_ERROR:
      name: "Relation violation"
      description: (source) ->
        """
        The following values have no match in the reference schema
        <code>#{source.parameters?.OTHER_SCHEMA}</code>
        (fields <code>#{source.parameters?.OTHER_FIELDS}</code>)
        """
    RELATION_PARENT_VALUE_ERROR:
      name: "Relation violation"
      description: (source) ->
        """
        The following values in referenced schema
        <code>#{source.parameters?.OTHER_SCHEMA}</code>
        (fields <code>#{source.parameters?.OTHER_FIELDS.join ', '}</code>)
        have no corresponding records in the current file,
        yet they are expected to have at least one match each.
        """
    MISSING_VALUE_ERROR:
      name: "Missing value"
      description: (source) ->
        """
        Missing value for required field. Offending lines
        """
    MISSING_ROWS_ERROR:
      name: "Missing rows"
      description: (source) ->
        """
        There are no records in this file. At least one record is required.
        """
    OUT_OF_RANGE_ERROR:
      name: "Value out of range"
      description: (source) ->
        """
        Values are out of range: [#{source.parameters?.MIN},
        #{source.parameters?.MAX}] (inclusive). Offending lines
        """
    NOT_A_NUMBER_ERROR:
      name: "Data type error"
      description: (source) ->
        """
        Values for range field are not numerical. Offending lines
        """
    VALUE_TYPE_ERROR:
      name: "Data type error"
      description: (source) ->
        """
        Invalid value types, expected type for this field is
        <code>#{source.parameters?.EXPECTED}</code>
        """
    UNIQUE_VALUE_ERROR:
      name: "Value uniqueness error"
      description: (source) ->
        """
        Duplicate values found
        """
    STRUCTURALLY_INVALID_ROW_ERROR:
      name: "Invalid row structure"
      description: (source) ->
        """
        Field counts in all lines are expected to be
        #{source.parameters?.EXPECTED}
        """
    FORBIDDEN_VALUE_ERROR:
      name: "Invalid value"
      description: (source) ->
        """
        Using forbidden value: <code>#{source.parameters?.VALUE}</code>
        """
    TOO_MANY_FILES_ERROR:
      name: "Filename collision"
      description: (source) ->
        """
        The following files are found matching
        <code>#{source.parameters?.SCHEMA}</code> filename pattern, only
        one file is allowed. <br>
        #{source.parameters?.FILES.join '<br>'}
        """
    COMPRESSION_CODEC_ERROR:
      name: "Compression Error"
      description: (source) ->
        """
        File name extension does not match file compression type. Please use
        <code>.gz</code> for gzip, <code>.bz2</code> for bzip2.
        """
    INVALID_CHARSET_ROW_ERROR:
      name: "Row contains invalid charset"
      description: (source) ->
        """
        Expected charset is <code>#{source.parameters?.EXPECTED}</code>
        with no control characters except for <code>tab</code> as field
        delimiter. Lines must be terminated with a <code>\\n</code> only and
        not <code>\\r</code>, <code>\\r\\n</code> or <code>\\n\\r</code>. Line terminator must be present at
        the end of every line, includeing the last line.
        Offending lines:
        """
    FILE_HEADER_ERROR:
      name: "File header error"
      description: (source) ->
        """
        Invalid header line. It is expected to contain the following fields
        in the specified order separated by <code>tab</code>: <br>
        """
    REFERENCE_GENOME_MISMATCH_ERROR:
      name: "Reference genome error"
      description: (source) ->
        """
        Sequence specified in reference_genome_allele does not match
        the corresponding sequence in the reference genome at:
        chromosome_start - chromosome_end
        """
    REFERENCE_GENOME_INSERTION_ERROR:
      name: "Reference genome error"
      description: (source) ->
        """
        For an insertion, there is no corresponding sequence in the
        reference genome, the only allowed value is a dash: <code>-</code>
        """
    TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR:
      name: "Excessive amount of SSMs need to be masked"
      description: (source) ->
        val1 = source.parameters.VALUE
        val2 = source.parameters.VALUE2
        expected = source.parameters.EXPECTED
        """
        The percentage (#{parseFloat(100*val1/val2).toFixed(2)}%) of SSMs that
        needs to be masked exceeded the reasonable level (currently the
        threshold is set as #{parseFloat(100*expected).toFixed(2)}% ).
        More details about SSM masking can be found
        <a href="http://docs.icgc.org/" target="_blank">here</a>.
        """
    SAMPLE_TYPE_MISMATCH:
      name: "Sample type mismatch error"
      description: (source) ->
        expected = source.parameters.EXPECTED
        fieldName = source.fieldNames[0]

        """
        Sample types should be consistent between clinical and experimental meta files.
        Expected <code>#{fieldName}</code> to be <code>#{expected}</code>
        """
    REFERENCE_SAMPLE_TYPE_MISMATCH:
      name: "Reference sample type mismatch error"
      description: (source) ->
        # This is reversed, expected is the only value NOT valid
        unexpectedCode = source.parameters.EXPECTED
        fieldName = source.fieldNames[0]
        unexpectedField = unexpectedCode
        if unexpectedCode == "matched"
          unexpectedField = "matched normal"

        """
        Reference sample types should be consistent between clinical and experimental meta files.
        The field <code>#{fieldName}</code> cannot be <code>#{unexpectedField}</code> for the following
        analyzed_sample_id(s)
        """
    FILE_ACCESSION_MISSING:
      name: "At least one file accession is required"
      description: (source) ->
        """
        At least one EGA file accession is required for <span class="mono-font">analysis_id</span>: #{source.parameters.VALUE}.
        Please refer to <a href='http://docs.icgc.org/submission/guide/ega_file_validation/'>http://docs.icgc.org/submission/guide/ega_file_validation/ </a> for more information.
        """
    FILE_ACCESSION_INVALID:
      name: "Could not find accession in remote repository"
      description: (source) ->
        """
        Could not find accession #{source.fieldNames[0]} in remote repository:
        <blockquote style="font-size: 12px;">#{source.parameters.VALUE}</blockquote>
        """
