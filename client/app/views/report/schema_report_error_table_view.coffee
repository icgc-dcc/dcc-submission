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
    console.debug "SchemaReportTableView#initialize", @collection
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
        this field: #{source.parameters?.EXPECTED}
        """
    DUPLICATE_HEADER_ERROR:
      name: "Duplicate field name"
      description: (source) ->
        """
        Duplicate field names found in the file header:
        #{source.parameters?.FIELDS}
        """
    RELATION_FILE_ERROR:
      name: "Required file missing"
      description: (source) ->
        """
        "#{source.parameters?.SCHEMA}" file is missing
        """
    REVERSE_RELATION_FILE_ERROR:
      name: "Required file missing"
      description: (source) ->
        """
        "#{source.parameters?.SCHEMA}" file is missing
        """
    RELATION_VALUE_ERROR:
      name: "Relation violation"
      description: (source) ->
        """
        The following "#{source.parameters?.FIELDS}" values do not exist in
        the reference file "#{source.parameters?.SCHEMA}"
        """
    RELATION_PARENT_VALUE_ERROR:
      name: "Relation violation"
      description: (source) ->
        """
        The following "#{source.parameters?.FIELDS}" values from the reference
        file do not exist in the current file "#{source.parameters?.FILE}"
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
      name: "Data type erorr"
      description: (source) ->
        """
        Values for range field are not numerical. Offending lines
        """
    VALUE_TYPE_ERROR:
      name: "Data type error"
      description: (source) ->
        """
        Invalid value types, expected type for this field is
        "#{source.parameters?.EXPECTED}"
        """
    UNIQUE_VALUE_ERROR:
      name: "Value uniqueness error"
      description: (source) ->
        """
        Duplicate values found in field(s) "#{source.parameters?.FIELDS}"
        """
    STRUCTURALLY_INVALID_ROW_ERROR:
      name: "Invalid row structure"
      description: (source) ->
        """
        Field counts in all lines are expected to match that of the file
        header. Offending lines
        """
    TOO_MANY_FILES_ERROR:
      name: "Filename collision"
      description: (source) ->
        """
        More than one file matches the "#{source.parameters?.SCHEMA}"
        filename pattern: #{source.parameters?.FILES}
        """


  details: (source) ->
    if source.errorType in [
      "MISSING_VALUE_ERROR"
      "OUT_OF_RANGE_ERROR"
      "NOT_A_NUMBER_ERROR"
      "STRUCTURALLY_INVALID_ROW_ERROR"
      ]
      return source.lines.join ', '

    out = ""
    #for key, value of source.parameters
    #  out += "<strong>#{key}</strong> : #{value}<br>"

    out += "<br><table class='table table-condensed'>
      <th style='border:none'>Line</th>
      <th style='border:none'>Value</th>"
    for i in source.lines
      out += "<tr><td style='background:none;border:none'>#{i}</td>
      <td style='background:none;border:none'>
      #{source.lineValueMap[i]}</td></tr>"
    out += "</table>"
    out

  update: ->
    console.debug "SchemaReportTableView#update", @collection
    @updateDataTable()

  createDataTable: ->
    console.debug "SchemaReportTableView#createDataTable", @$el, @collection
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
          mData: "columnNames"
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
            out += ":<br>"
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
