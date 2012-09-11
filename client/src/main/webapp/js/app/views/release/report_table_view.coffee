"""
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
"""

define (require) ->
  DataTableView = require 'views/base/data_table_view'
  utils = require 'lib/utils'

  'use strict'

  class ReportTableView extends DataTableView
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
      if data.errors
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
        value = if key in ['stddev','avg'] then Number(value).toFixed(2) else value
        sOut += "<dd><strong>#{key}: </strong>#{value}<br></dd>"
        
      sOut

    formatError: (error) ->
      switch error.code
        when "MISSING_VALUE_ERROR"
          """
            <td>Value Missing</td>
            <td>value missing for required field: <strong>#{error.parameters[1]}</strong></td>
          """
        when "RELATION_ERROR"
          """
            <td>Relation Error</td>
            <td>invalid value(s) (<strong>#{error.parameters[0]}</strong>) for field(s) <strong>#{error.parameters[1]}.#{error.parameters[2]}</strong>. Expected to match value(s) in: <strong>#{error.parameters[3]}.#{error.parameters[4]}</strong></td>
          """
        when "RELATION_PARENT_ERROR"
          """
            <td>Relation Parent Error</td>
            <td>no corresponding values in <strong>#{error.parameters[0]}.#{error.parameters[1]}</strong> for value(s) <strong>#{error.parameters[2]}</strong> in <strong>#{error.parameters[3]}.#{error.parameters[4]}</strong></td>
          """
          
        when "STRUCTURALLY_INVALID_ROW_ERROR"
          """
            <td>Structurally Invalid Row</td>
            <td>structurally invalid row: <strong>#{error.parameters[0]}</strong> columns against <strong>#{error.parameters[1]}</strong> declared in the header (row will be ignored by the rest of validation)"</td>
          """
        when "UNIQUE_VALUE_ERROR"
          """
            <td>Unique Value Error</td>
            <td>invalid set of values (<strong>#{error.parameters[0]}</strong>) for fields <strong>#{error.parameters[1]}</strong>. Expected to be unique</td>
          """
        when "UNKNOWN_COLUMNS_WARNING"
          """
            <td>Unkown Column</td>
            <td>value for unknown column: <strong>#{error.parameters[0]}</strong></td>
          """
        when "VALUE_TYPE_ERROR"
          """
            <td>Value Type Error</td>
            <td>invalid value <strong>#{error.parameters[0]}</strong> for field <strong>#{error.parameters[1]}</strong>. Expected type is: <strong>#{error.parameters[2]}</strong></td>
          """
        when "OUT_OF_RANGE_ERROR"
          """
            <td>Out of Range</td>
            <td>number <strong>#{error.parameters[0]}</strong> is out of range for field <strong>#{error.parameters[1]}</strong>. Expected value between <strong>#{error.parameters[2]}</strong> and <strong>#{error.parameters[3]}</strong></td>
          """
        when "NOT_A_NUMBER_ERROR"
          """
            <td>Not a Number</td>
            <td><strong>#{error.parameters[0]}</strong> is not a number for field <strong>#{error.parameters[1]}</strong>. Expected a number</td>
          """
        when "MISSING_VALUE_ERROR"
          """
            <td>Missing Value</td>
            <td>value missing for required field: <strong>#{error.parameters[0]}</strong></td>
          """
        when "CODELIST_ERROR"
          """
            <td>Codelist Error</td>
            <td>invalid value <strong>#{error.parameters[0]}</strong> for field <strong>#{error.parameters[1]}</strong>. Expected code or value from CodeList <strong>#{error.parameters[2]}</strong></td>
          """
        when "DISCRETE_VALUES_ERROR"
          """
            <td>Discrete Values Error</td>
            <td>invalid value <strong>#{error.parameters[0]}</strong> for field <strong>#{error.parameters[1]}</strong>. Expected one of the following values: <strong>#{error.parameters[2]}</strong></td>
          """
        when "TOO_MANY_FILES_ERROR"
          """
            <td>Too many files</td>
            <td>more than one file matches the schema pattern</td>
          """
        when "INVALID_RELATION_ERROR"
          """
            <td>Invalid Relation</td>
            <td>a required schema for this relation was not found</td>
          """
        when "MISSING_SCHEMA_ERROR"
          """
            <td>Missing Schema</td>
            <td>no valid schema found</td>
          """
        else
          "<td><strong>#{error.code}</strong></td><td><strong>#{error.parameters}</strong></td>"

    formatParams: (error) ->
      console.debug "ReportTableView#formatParams", error
      out = []
      errors = []
      
      for key, value of error.parameters
        out.push "<strong>#{key}</strong>: #{value}<br>"
        
      for i in [0 .. error.lines.length - 1]
        e = ''
        e += "<strong>#{error.lines[i]}</strong>"
        if error.values[i]
          e += ": #{error.values[i]}"
        errors.push e
        
      out.push(errors.join ", ")
      out.join ""
      
    formatDetails: (data) ->
      console.debug "ReportTableView#formatDetails", data
      
      sOut = ''
      sErr = ''
      
      if data.errors
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
            sOut += "<td>#{errorObj.errorType}</td><td>#{error.columnName}</td>"
            sOut += "<td>#{error.count}</td><td>#{@formatParams error}</td>"
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
      if @model.get('report').get('schemaReports').length
        dt = @$el.dataTable()
        dt.fnSetColumnVis( 3, true )
        dt.fnSetColumnVis( 4, true )
      super

    createDataTable: ->
      console.debug "ReportTableView#createDataTable", @$el
      aoColumns = [
          {
            sTitle: "File"
            bSortable: false
            bUseRendered: false
            mDataProp: "name"
          }
          {
            sTitle: "Last Updated"
            mDataProp: "lastUpdate"
            sType: "date"
            fnRender: (oObj, sVal) ->
              utils.date sVal
          }
          {
            sTitle: "Size"
            mDataProp: "size"
            bUseRendered: false
            fnRender: (oObj, Sval) ->
              utils.fileSize Sval
          }
          {
            sTitle: "Status"
            mDataProp: null
            bSortable: true
            bVisible: false
            fnRender: (oObj, Sval)->
              if oObj.aData.errors
                errors = 0
                #for es in oObj.aData.errors
                #  console.log es
                #  errors += es.errors.length
                "<span class='invalid'>INVALID</span>"
              else
                "<span class='valid'>VALID</span>"
          }
          {
            sTitle: "Report"
            mDataProp: null
            bSortable: false
            bVisible: false
            sDefaultContent: "<span class='link control'>view</span>"
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
        fnServerData: (sSource, aoData, fnCallback) =>
          fnCallback @collection.toJSON()
