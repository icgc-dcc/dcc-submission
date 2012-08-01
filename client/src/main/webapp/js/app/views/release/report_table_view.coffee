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
    
    rowDetails: (e) ->
      console.debug "ReportTableView#rowDetails", e, @anOpen
      control = e.target
      nTr = control.parentNode.parentNode
      dT = @.$('table').dataTable()
      
      if nTr in @anOpen
        @anOpen = _.without @anOpen, nTr
        dT.fnClose(nTr)
        $(control).find('i')
                  .removeClass('icon-chevron-down')
                  .addClass('icon-chevron-right')
        $(nTr).removeClass 'well'
      else
        @anOpen.push nTr
        data = dT.fnGetData nTr
        
        $(control).find('i')
                  .removeClass('icon-chevron-right')
                  .addClass('icon-chevron-down')
        $(nTr).addClass 'well'
        
        dT.fnOpen(nTr, @formatDetails(data), 'details well')

    formatDetails: (data) ->
      console.debug "ReportTableView#formatDetails", data
      
      sOut = ''
      sErr = ''
      
      if data.errors
        sOut += '<ol style="color:#B94A48">'
        for error in data.errors
          sOut += "<li>" + error + "</li>"
        sOut += "</ol>"
        sOut
      else if data.fieldReports  
        sOut += """
          <table class='table table-stripedd'>
          <thead>
            <tr>
            <th>Name</th>
            <th>Completeness</th>
            <th>Populated</th>
            <th>Nulls</th>
            <th>Summary</th>
            </tr>
          </thead>
          <tbody>
          """

        for report in data.fieldReports
          sOut += """        
            <tr>
            <td>#{report.name}</td>
            <td>#{report.completeness}</td>
            <td>#{report.populated}</td>
            <td>#{report.nulls}</td>
            <td>
          """
          for key, value of report.summary
            sOut += "<dd>#{key}: #{value}</dt>"
            

        sOut += """
          </td>
          </tr>
          </tbody>
          </table>
          """
        $(sOut).dataTable
          bPaginate: false
    
      
              
    createDataTable: (collection) ->
      console.debug "ReportTableView#createDataTable", @.$('table')
      aoColumns = [
          {
            sTitle: "Filename"
            mDataProp: "name"
            fnRender: (oObj, sVal) ->
              """
                <a href="#" class="control">#{sVal}</a>
              """
          }
          {
            mDataProp: "errors"
            bSortable: false
            sWidth: "200px"
            bUseRendered: false
            fnRender: (oObj, sVal) ->
              if sVal
                "#{sVal.length} errors"
              else ""
          }
        ]
      
      @.$('table.report').dataTable
        sDom:
          "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
        #sPaginationType: "bootstrap"
        bPaginate: false
        oLanguage:
          "sLengthMenu": "_MENU_ Schema Reports per page"
        aaSorting: [[ 1, "asc" ]]
        aoColumns: aoColumns
        sAjaxSource: ""
        sAjaxDataProp: ""
        
        fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
          if aData.errors
            $(nRow).css 'color', '#B94A48'
        
        fnServerData: (sSource, aoData, fnCallback) ->
          fnCallback collection.toJSON()
          
      #@.$('table.report').removeClass('table')
