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
        this field: <em>#{source.parameters?.EXPECTED}</em>
        """
    REGEX_ERROR:
      name: "Invalid value"
      description: (source) ->
        """
        Values do not match the regular expression set for
        this field: <em>#{source.parameters?.EXPECTED}</em>
        """
    SCRIPT_ERROR:
      name: "Failed script expression"
      description: (source) ->
        # Note we don't have an mvel formatter/highlighter, this is
        # currently simulated with javascript formatter and java highlighter
        errorRaw = source.parameters?.EXPECTED
        errorPretty = hljs.highlight('java', js_beautify(errorRaw)).value

        """
        #{source.parameters?.DESCRIPTION}.
        Values do not pass the script expression associated with this
        this field: <br><br>
        <pre><code>#{errorPretty}</code></pre>
        """
    DUPLICATE_HEADER_ERROR:
      name: "Duplicate field name"
      description: (source) ->
        """
        Duplicate field names found in the file header
        <em>#{source.parameters?.FIELDS}</em>
        """
    RELATION_FILE_ERROR:
      name: "Required file missing"
      description: (source) ->
        """
        <em>#{source.parameters?.SCHEMA}</em> file is missing
        """
    REVERSE_RELATION_FILE_ERROR:
      name: "Required file missing"
      description: (source) ->
        """
        <em>#{source.parameters?.SCHEMA}</em> file is missing
        """
    RELATION_VALUE_ERROR:
      name: "Relation violation"
      description: (source) ->
        """
        The following values have no match in the reference schema
        <em>#{source.parameters?.OTHER_SCHEMA}</em>
        (fields <em>#{source.parameters?.OTHER_FIELDS}</em>)
        """
    RELATION_PARENT_VALUE_ERROR:
      name: "Relation violation"
      description: (source) ->
        """
        The following values in referenced schema
        <em>#{source.parameters?.OTHER_SCHEMA}</em>
        (fields <em>#{source.parameters?.OTHER_FIELDS.join ', '}</em>)
        have no corresponding records in the current file,
        yet they are expected to have at least one match each.
        """
    MISSING_VALUE_ERROR:
      name: "Missing value"
      description: (source) ->
        """
        Missing value for required field. Offending lines
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
        <em>#{source.parameters?.EXPECTED}</em>
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
        Field counts in all lines are expected to be #{source.parameters?.EXPECTED}
        """
        #"""
        #Field counts in all lines are expected to match that of the file
        #header. Offending lines
        #"""
    FORBIDDEN_VALUE_ERROR:
      name: "Invalid value"
      description: (source) ->
        """
        Using forbidden value: <em>#{source.parameters?.VALUE}</em>
        """
    TOO_MANY_FILES_ERROR:
      name: "Filename collision"
      description: (source) ->
        """
        The following files are found matching
        <em>#{source.parameters?.SCHEMA}</em> filename pattern, only
        one file is allowed. <br>
        #{source.parameters?.FILES.join '<br>'}
        """
    COMPRESSION_CODEC_ERROR:
      name: "Compression Error"
      description: (source) ->
        """
        File name extension does not match file compression type. Please use
        <em>.gz</em> for gzip, <em>.bz2</em> for bzip2.
        """
    INVALID_CHARSET_ROW_ERROR:
      name: "Row contains invalid charset"
      description: (source) ->
        """
        Charset Invalid, expected charset for the line is
        <em>#{source.parameters?.EXPECTED} with no control character except
         tab as a delimiter </em>. Offending lines:
        """
    FILE_HEADER_ERROR:
      name: "File header error"
      description: (source) ->
        """
        Invalid header line. It is expected to contain the following fields
        in the specified order separated by <em>tab</em>: <br>
        """
        #"""
        #Different from the expected header
        #<em>#{source.parameters?.EXPECTED}</em>
        #<br><br>
        #<em>#{source.parameters?.VALUE}</em>
        #"""
    REFERENCE_GENOME_MISMATCH_ERROR:
      name: "Reference genome error"
      description: (source) ->
        """
        Sequence specified in reference_genome_allele does not match
        the corresponding sequence in the reference genome at:
        chromosome_start - chromosome_end
        """
        #"""
        #Submitted reference genome allele does not match allele in
        # <em>#{source.parameters?.EXPECTED}</em>
        #"""
    REFERENCE_GENOME_INSERTION_ERROR:
      name: "Reference genome error"
      description: (source) ->
        """
        For an insertion, there is no corresponding sequence in the
        reference genome, the only allowed value is a dash: <em>-</em>
        """
    TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR:
      name: "Excessive amount of sensitive data error"
      description: (source) ->
        """
        An abnormal ratio (<em>#{source.parameters?.VALUE}</em> out of
        <em>#{source.parameters?.VALUE2}</em>) of CONTROLLED to OPEN
        observations has been dectected and most likely indicates an error
        in the data. The maximum threshold allowed is
        <em>#{parseFloat(100*source.parameters?.EXPECTED).toFixed(2)}%</em>.
        """
  details: (source) ->

    # There are generally two types of errors: file level errors
    # with no line details, and row level errors
    if source.errorType in [
      "COMPRESSION_CODEC_ERROR"
      "TOO_MANY_FILES_ERROR"
      #"FILE_HEADER_ERROR"
      "RELATION_FILE_ERROR"
      "REVERSE_RELATION_FILE_ERROR"
      "TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR"
      ]
      return ""
    else if source.errorType in [
      "MISSING_VALUE_ERROR"
      "OUT_OF_RANGE_ERROR"
      "NOT_A_NUMBER_ERROR"
      "INVALID_CHARSET_ROW_ERROR"
      #"STRUCTURALLY_INVALID_ROW_ERROR"
      ]
      return source.lines.join ', '
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

    else if source.columnNames[0] is "FileLevelError"
      return ""

    out = ""
    #for key, value of source.parameters
    #  out += "<strong>#{key}</strong> : #{value}<br>"

    out += "<br><table class='table table-condensed'>
      <th style='border:none'>Line</th>
      <th style='border:none'>Value</th>"
    for i in source.lines
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
            source.columnNames.join "<br>"
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
