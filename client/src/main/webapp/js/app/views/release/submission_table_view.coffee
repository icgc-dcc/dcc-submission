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
  signOffSubmissionView = require 'views/release/signoff_submission_view'
  validateSubmissionView = require 'views/release/validate_submission_view'
  utils = require 'lib/utils'
  
  'use strict'

  class SubmissionTableView extends DataTableView
    template: template
    template = null

    autoRender: true
    
    initialize: ->
      console.debug "SubmissionsTableView#initialize", @model, @el
      @collection = @model.get "submissions"
      
      super
      
      @modelBind 'change', @update
      
      @delegate 'click', '#signoff-submission-popup-button', @signOffSubmissionPopup
      @delegate 'click', '#validate-submission-popup-button', @validateSubmissionPopup
    
    update: ->
      @collection = @model.get "submissions"
      @updateDataTable()
    
    signOffSubmissionPopup: (e) ->
      console.debug "ReleaseView#signOffSubmissionPopup", e
      @subview("signOffSubmissionView"
        new signOffSubmissionView
          "submission": @collection.get $(e.currentTarget).data("submission")
      )
      
    validateSubmissionPopup: (e) ->
      console.debug "ReleaseView#validateSubmissionPopup", e
      submission = @collection.get $(e.currentTarget).data("submission")
      submission.set 'queue', @model.get("queue").length
      
      @subview("validateSubmissionView"
        new validateSubmissionView
          "submission": submission
      )
      
    createDataTable: ->
      console.debug "SubmissionsTableView#createDataTable", @$el, @collection
      aoColumns = [
          {
            sTitle: "Project Key"
            mDataProp: "projectKey"
            fnRender: (oObj, sVal) =>
              "<a href='/releases/#{@collection.release}/submissions/#{sVal}'>#{sVal}</a>"
          }
          {
            sTitle: "State"
            mDataProp: "state"
            fnRender: (oObj, sVal) ->
              sVal.replace '_', ' '
          }
          {
            sTitle: "Last Updated"
            mDataProp: "lastUpdated"
            fnRender: (oObj, sVal) ->
              utils.date sVal
          }
          {
            sTitle: "Report"
            mDataProp: null
            bSortable: false
            fnRender: (oObj) =>
              switch oObj.aData.state
                when "VALID", "SIGNED OFF", "INVALID"
                  """
                    <a href='/releases/#{@collection.release}/submissions/#{oObj.aData.projectKey.replace(/<.*?>/g, '')}#report'>View</a>
                  """
                else ""
          }
          {
            sTitle: ""
            mDataProp: null
            bSortable: false
            sWidth: "75px"
            bVisible: not utils.is_released(@model.get "state")
            fnRender: (oObj) ->
              switch oObj.aData.state
                when "VALID"
                  """
                    <a id="signoff-submission-popup-button"
                       data-submission="#{oObj.aData.projectKey.replace(/<.*?>/g, '')}"
                       data-toggle="modal"
                       href='#signoff-submission-popup'>
                       Sign Off
                    </a>
                  """
                when "NOT VALIDATED", "INVALID"
                  """
                    <a id="validate-submission-popup-button"
                       data-submission="#{oObj.aData.projectKey.replace(/<.*?>/g, '')}"
                       data-toggle="modal"
                       href='#validate-submission-popup'>
                       Validate
                    </a>
                 """
                else ""
          }
        ]
      
      @$el.dataTable
        sDom:
          "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
        bPaginate: false
        oLanguage:
          "sLengthMenu": "_MENU_ submissions per page"
        aaSorting: [[ 2, "desc" ]]
        aoColumns: aoColumns
        sAjaxSource: ""
        sAjaxDataProp: ""
        fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
          cell = $('td:nth-child(2)', nRow)
          switch aData.state
            when "SIGNED OFF"
              cell.css 'color', '#3A87AD'
            when "VALID"
              cell.css 'color', '#468847'
            when "QUEUED"
              cell.css 'color', '#C09853'
            when "INVALID"
              cell.css 'color', '#B94A48'
              
        fnServerData: (sSource, aoData, fnCallback) =>
          fnCallback @collection.toJSON()
