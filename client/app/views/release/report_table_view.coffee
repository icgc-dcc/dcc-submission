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
utils = require 'lib/utils'

module.exports = class ReportTableView extends DataTableView
  template: template
  template = null

  autoRender: false

  initialize: ->
    console.debug "ReportTableView#initialize", @model, @el
    @report = @model.get "report"
    @collection = @report.get "schemaReports"

    super

    @modelBind 'change', @update

    @anOpen = []
    @delegate 'click', '.control', @rowDetails
    @delegate 'click', '.summary', @rowSummary

  update: ->
    console.debug "ReportTableView#update", @model
    @report = @model.get "report"
    @collection = @report.get "schemaReports"
    @updateDataTable()

  rowDetails: (e) ->
    #console.debug "ReportTableView#rowDetails", e, @anOpen
    control = e.target
    nTr = control.parentNode.parentNode
    dT = @$el.dataTable()

    data = dT.fnGetData nTr
    if data.errors.length != 0
      style = 'alert-danger'
    else
      style = 'alert-info'

    if nTr in @anOpen
      @anOpen = _.without @anOpen, nTr
      $(control).text 'view'
      dT.fnClose(nTr)
    else
      @anOpen.push nTr
      $(control).text 'hide'
      dT.fnOpen(nTr, @formatDetails(data), "details #{style}")

  rowSummary: (e) ->
    console.debug "ReportTableView#rowSummary", e, @anOpen
    control = e.target
    nTr = control.parentNode.parentNode
    dT = @.$(nTr.parentNode.parentNode).dataTable()

    data = dT.fnGetData nTr
    style = 'signed'

    if nTr in @anOpen
      @anOpen = _.without @anOpen, nTr
      $(control).text 'show'
      dT.fnClose(nTr)
    else
      @anOpen.push nTr
      $(control).text 'hide'
      dT.fnOpen(nTr, @summaryDetails(data), "summary_details well")

  summaryDetails: (data) ->
    console.debug "ReportTableView#summaryDetails", data
    type = switch data.type
      when "AVERAGE" then "Summary Statistics"
      when "FREQUENCY" then "Value Frequencies"

    sOut = "<dt>#{type}</dt>"
    for key, value of data.summary
      value =
        if key in ['stddev','avg']
          Number(value).toFixed(2)
        else
          value
      sOut += "<dd><strong>#{key}: </strong>#{value}<br></dd>"

    sOut

  formatErrorKey: (key) ->
    switch key
      when "expectedValue" then "Expected Value"
      when "expectedType" then "Expected Type"
      when "firstOffset" then "First Occurence"
      when "minRange" then "Min Accepted Value"
      when "maxRange" then "Max Accetped Value"
      when "actualNumColumns" then "Expected Number of Columns"
      when "relationSchema" then "Relation Schema"
      when "relationColumnNames" then "Relation Columns"

  formatParams: (error) ->
    console.debug "ReportTableView#formatParams", error
    out = ""
    for key, value of error.parameters
      out += "<strong>#{@formatErrorKey(key)}</strong> : #{value}<br>"

    out += "<table class='table'>
      <th style='border:none'>Line</th>
      <th style='border:none'>Value</th>"
    map = {}
    for i in [0 .. error.lines.length - 1]
      map[error.lines[i]] = error.values[i]
    lines = error.lines.sort((a,b)-> a - b)
    for i in lines
      out += "<tr><td style='border:none'>#{i}</td>
      <td style='border:none'>#{map[i]}</td></tr>"
    out += "</table>"
    out

  formatDetails: (data) ->
    console.debug "ReportTableView#formatDetails", data

    sOut = ''
    sErr = ''

    if data.errors.length != 0
      sOut += """
        <table class='table table-striped'>
        <thead>
          <tr>
          <th>Error Type</th>
          <th>Column Name</th>
          <th>Count</th>
          <th>Parameters</th>
          </tr>
        </thead>
        <tbody>
      """

      for errorObj in data.errors
        console.log errorObj
        for error in errorObj.columns
          console.log errorObj, error
          sOut += "<tr>"
          sOut += "<td>#{errorObj.errorType}</td>
            <td>#{error.columnNames.join ', '}</td>"
          sOut += "<td>#{error.count}</td>"
          if errorObj.errorType is "MISSING_VALUE_ERROR"
            sOut += "<td>#{error.lines.sort((a,b)-> a - b).join(', ')}</td>"
          else
            sOut += "<td>#{@formatParams error}</td>"
          sOut += "</tr>"
      sOut += "</tbody></table>"

      $(sOut).dataTable
        bPaginate: false

    else if data.fieldReports
      sOut += "<table class='sub_report table table-striped'></table>"

      $(sOut).dataTable
        bPaginate: false
        aaData: data.fieldReports
        aoColumns: [
          { sTitle: "Field Name", mDataProp: "name"}
          { sTitle: "Completeness<br>(%)", mDataProp: "completeness"}
          { sTitle: "Populated<br>(# rows)", mDataProp: "populated"}
          { sTitle: "Missing<br>(# rows)", mDataProp: "missing"}
          { sTitle: "Nulls<br>(# rows)", mDataProp: "nulls"}
          {
            sTitle: "Summary"
            mDataProp: "summary"
            bSortable: false
            bUseRendered: false
            fnRender: (oObj, sVal) ->
              if not $.isEmptyObject sVal
                "<span class='summary link'>show</span></td>"
              else
                ""
          }
        ]
        aaSorting: [[ 1, "asc" ]]


  updateDataTable: ->
    srs = @model.get('report').get('schemaReports')
    errors = (item for item in srs.pluck("errors") when item?)
    fieldReport = (item for item in srs.pluck("fieldReports") when item?)
    if errors or fieldReports
      dt = @$el.dataTable()
      dt.fnSetColumnVis( 3, true )
      dt.fnSetColumnVis( 4, true )
    super

  createDataTable: ->
    console.debug "ReportTableView#createDataTable", @$el
    aoColumns = [
        {
          sTitle: "File"
          mData: "name"
        }
        {
          sTitle: "Last Updated"
          mData: (source) -> utils.date source.lastUpdate
          sType: "date"
        }
        {
          sTitle: "Size"
          bUseRendered: false
          mData: (source, type) ->
            if type is "display"
              return utils.fileSize source.size
            source.size
        }
        {
          sTitle: "Status"
          bVisible: false
          mData: (source) ->
            if source.matchedSchemaName
              if source.errors.length != 0
                "INVALID"
              else if source.fieldReports.length != 0
                "VALID"
              else
                "NOT VALIDATED"
            else
              "SKIPPED"
        }
        {
          sTitle: "Report"
          bSortable: false
          bVisible: false
          mData: (source) ->
            if source.errors.length != 0 or source.fieldReports != 0
              "<span class='link control'>view</span>"
            else
              ""
        }
      ]

    @$el.dataTable
      sDom:
        "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
      bPaginate: false
      aaSorting: [[ 1, "asc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
        cell = $('td:nth-child(4)', nRow)
        switch cell.html()
          when "VALID"
            cell.css 'color', '#468847'
          when "INVALID", "ERROR"
            cell.css 'color', '#B94A48'
          when "SKIPPED"
            $(nRow).css {'color': '#999', 'font-style': 'italic'}
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @collection.toJSON()
