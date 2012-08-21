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

    autoRender: true
    
    initialize: ->
      console.debug "ReportTableView#initialize", @model, @el
      @collection = @model.get "schemaReports"

      super
      
      @anOpen = []
      @delegate 'click', '.control', @rowDetails
      @delegate 'click', '.summary', @rowSummary
    
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
      sOut = "<dt>#{data.type}</dt>"
      for key, value of data.summary
        value = if key is 'stddev' then Number(value).toFixed(2) else value
        sOut += "<dd><strong>#{key}: </strong>#{value}<br></dd>"
        
      sOut

    formatDetails: (data) ->
      console.debug "ReportTableView#formatDetails", data
      
      sOut = ''
      sErr = ''
      console.log data.errors
      if data.errors
        sOut += """
          <table class='table table-striped'>
          <thead>
            <tr>
            <th>Offset</th>
            <th>Error Type</th>
            <th>Parameters</th>
            </tr>
          </thead>
          <tbody>
        """
        
        for errorObj in data.errors
          for error in errorObj.errors
            sOut += "<tr>"
            sOut += """
              <td>#{errorObj.offset}</td>
              <td>#{error.code}</td>
              <td>#{error.parameters}</td>
            """
            sOut += "</tr>"
        sOut += "</tbody></table>"
        
        $(sOut).dataTable
          bPaginate: false
        
      else if data.fieldReports
        sOut += "<table class='sub_report table table-striped'></table>"
        
        $(sOut).dataTable
          sDom:
            "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
          sPagination: 'bootstrap'
          aaData: data.fieldReports
          aoColumns: [
            { sTitle: "Name", mDataProp: "name"}
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
        ]
        
        reportCols = [
          {
            sTitle: "Status"
            mDataProp: null
            bSortable: true
            fnRender: (oObj, Sval)->
              if oObj.aData.errors
                errors = 0
                for es in oObj.aData.errors
                  errors += es.errors.length
                "<span class='invalid'>#{errors} ERRORS</span>"
              else
                "<span class='valid'>VALID</span>"
          }
          {
            sTitle: "Report"
            mDataProp: null
            bSortable: false
            sDefaultContent: "<span class='link control'>view</span>"
          }
        ]
      console.log @model.get 'report'
      if @model.get 'report'
        aoColumns = aoColumns.concat reportCols
      
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
          
