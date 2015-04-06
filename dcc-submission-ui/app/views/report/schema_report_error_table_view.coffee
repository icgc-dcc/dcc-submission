"""
* Copyright 2012(c) The Ontario Institute for Cancer Research.
* All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the GNU Public License v3.0.
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
"""


DataTableView = require 'views/base/data_table_view'

module.exports = class SchemaReportErrorTableView extends DataTableView
  template: template
  template = null


  container: "#schema-report-container"
  containerMethod: 'html'

  autoRender: true

  initialize: ->
    #console.debug "SchemaReportTableView#initialize", @collection
    super

    @modelBind 'change', @update

  errors:
    PCAWG_SAMPLE_STUDY_MISMATCH:
      name: "PanCancer sample study mismatch"
      description: (source) ->
        """
        Inconsistent sample study status between ICGC DCC and
        <a href="http://pancancer.info" target="_blank">pancancer.info</a>,
        see <a href="https://docs.icgc.org/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_SAMPLE_MISSING:
      name: "PanCancer sample missing"
      description: (source) ->
        """
        Missing ICGC DCC sample that exists in
        <a href="http://pancancer.info" target="_blank">pancancer.info</a>,
        see <a href="https://docs.icgc.org/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_CLINICAL_FIELD_REQUIRED:
      name: "PanCancer clinical field required"
      description: (source) ->
        """
        Clinical field is required for PanCancer,
        see <a href="https://docs.icgc.org/pancancer-clinical-data-requirements" target="_blank">
        submission documentation</a> for details.
        """
    PCAWG_CLINICAL_ROW_REQUIRED:
      name: "PanCancer clinical row required"
      description: (source) ->
        """
        Clinical row is required for PanCancer in file
        <code>#{source.parameters?.VALUE.toLowerCase().replace(/_type$/, "")}</code>,
        see <a href="https://docs.icgc.org/pancancer-clinical-data-requirements" target="_blank">
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
        <br><br><pre><code>#{errorPretty}</code></pre>
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
  details: (source) ->

    # There are generally two types of errors: file level errors
    # with no line details, and row level errors
    if source.errorType in [
      "COMPRESSION_CODEC_ERROR"
      "TOO_MANY_FILES_ERROR"
      #"FILE_HEADER_ERROR"
      "RELATION_FILE_ERROR"
      "MISSING_ROWS_ERROR"
      "REVERSE_RELATION_FILE_ERROR"
      "TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR"
      ]
      return ""
    else if source.errorType in [
      "MISSING_VALUE_ERROR"
      "OUT_OF_RANGE_ERROR"
      "NOT_A_NUMBER_ERROR"
      "INVALID_CHARSET_ROW_ERROR"
      "LINE_TERMINATOR_MISSING_ERROR"
      #"STRUCTURALLY_INVALID_ROW_ERROR"
      ]
      return source.lineNumbers.join ', '
    else if source.errorType is 'FILE_HEADER_ERROR'
      console.log ">>>", source
      expected = source.parameters.EXPECTED
      actual = source.parameters.VALUE
      displayLength = Math.max(expected.length, actual.length)

      out = ""
      out += "<br><table class='table table-condensed'>
        <th style='border:none'>Expected</th>
        <th style='border:none'>Actual</th>"

      console.log expected
      console.log actual
      for i in [0..displayLength - 1] by 1
        expectedVal = expected[i] || '-'
        actualVal = actual[i] || '-'
        out += "<tr>
          <td style='background:none;border:none'>
          #{expectedVal}</td>
          <td style='background:none;border:none'>
          #{actualVal}</td></tr>"
      out += "</table>"
      return out

    else if source.fieldNames[0] is "FileLevelError"
      return ""

    out = ""

    out += "<br><table class='table table-condensed'>
      <th style='border:none'>Line</th>
      <th style='border:none'>Value</th>"
    for i in source.lineNumbers
      if i==-1
        display = "N/A"
      else
        display = i
      out += "<tr><td style='background:none;border:none'>#{display}</td>
      <td style='background:none;border:none'>
      #{source.lineValueMap[i]}</td></tr>"
    out += "</table>"
    out

  update: ->
    #console.debug "SchemaReportTableView#update", @collection
    @updateDataTable()

  createDataTable: ->
    #console.debug "SchemaReportTableView#createDataTable", @$el, @collection
    aoColumns = [
        {
          sTitle: "Error Type"
          mData: (source) =>
            if @errors[source.errorType]
              @errors[source.errorType].name
            else source.errorType
        }
        {
          sTitle: "Columns"
          mData: (source) ->
            source.fieldNames.join "<br>"
        }
        {
          sTitle: "Count of occurrences"
          mData: "count"
        }
        {
          sTitle: "Details"
          mData: (source) =>
            out = "#{@errors[source.errorType]?.description(source)}"
            if source.count > 50
              out += " <em>(Showing first 50 errors)</em> "
            out += "<br>"
            out += @details source
            out
        }
      ]

    @$el.dataTable
      sDom:
        "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
      bPaginate: false
      oLanguage:
        "sLengthMenu": "_MENU_ submissions per page"
      aaSorting: [[ 1, "desc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @collection.toJSON()
