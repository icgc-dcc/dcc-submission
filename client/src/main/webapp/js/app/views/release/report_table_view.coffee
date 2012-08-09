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
  template = require 'text!views/templates/release/report_table.handlebars'
  
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
      dT = @.$('table').dataTable()
      
      data = dT.fnGetData nTr
      if data.errors
        style = 'alert-danger'
      else
        style = 'alert-info'

      if nTr in @anOpen
        @anOpen = _.without @anOpen, nTr
        $(control).text 'show'
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
      
      if data.errors
        sOut += """
          <table class='table table-striped'>
          <thead>
            <tr>
            <th style="text-align:center"></th>
            </tr>
          </thead>
          <tbody>
        """
        for error in data.errors
          sOut += "<tr><td>"
          switch error['code']
            when "MISSING_VALUE_ERROR"
              sOut += "Value missing for column #{error['parameters'][1]}"
            when "CODELIST_ERROR"
              sOut += "CodeList error #{error['parameters'][0]} #{error['parameters'][1]} #{error['parameters'][2]}"
            when "MISSING_RELATION_ERROR"
              sOut += "Missing Relation error #{error['parameters'][0]} #{error['parameters'][1]} #{error['parameters'][2]}"
          sOut += "</td></tr>"
        sOut += "</tbody></table>"
        
        $(sOut).dataTable
          bPaginate: false
        
      else if data.fieldReports
        sOut += "<table class='sub_report table table-striped'></table>"
        
        $(sOut).dataTable
          bPaginate: false
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
                  "<span class='summary signed'>show</span></td>"
                else
                  ""
            }
          ]
          aaSorting: [[ 1, "asc" ]]
          

    createDataTable: (collection) ->
      console.debug "ReportTableView#createDataTable", @.$('table')
      aoColumns = [
          {
            bSortable: false
            mDataProp: "name"
            fnRender: (oObj, sVal) ->
              out = sVal
              if oObj.aData.errors
                out += " <span class='invalid'>(#{oObj.aData.errors.length} errors)</span>" 
              
              """
                #{out}
              """
          }
          {
            mDataProp: null
            bSortable: false
            sWidth: "100px"
            sDefaultContent: "<span class='signed control'>show</span>"
          }
        ]
      
      @.$('table.report').dataTable
        sDom:
          "<'row-fluid'<'span6'l>HHH<'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
        bPaginate: false
        aaSorting: [[ 1, "asc" ]]
        aoColumns: aoColumns
        sAjaxSource: ""
        sAjaxDataProp: ""
        fnServerData: (sSource, aoData, fnCallback) ->
          fnCallback collection.toJSON()
          
      @.$('table.report').removeClass('table')
