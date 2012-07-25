define (require) ->
  DataTableView = require 'views/base/data_table_view'
  signOffSubmissionView = require 'views/release/signoff_submission_view'
  validateSubmissionView = require 'views/release/validate_submission_view'
  template = require 'text!views/templates/release/submissions_table.handlebars'
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
        
      @subscribeEvent "signOffSubmission", @update
      @subscribeEvent "validateSubmission", @update
      
      @delegate 'click', '#signoff-submission-popup-button', @signOffSubmissionPopup
      @delegate 'click', '#validate-submission-popup-button', @validateSubmissionPopup
    
    signOffSubmissionPopup: (e) ->
      console.debug "ReleaseView#signOffSubmissionPopup", e
      @subview("signOffSubmissionView"
        new signOffSubmissionView
          "submission": @collection.get $(e.currentTarget).data("submission")
      )
      
    validateSubmissionPopup: (e) ->
      console.debug "ReleaseView#validateSubmissionPopup", e
      @subview("validateSubmissionView"
        new validateSubmissionView
          "submission": @collection.get $(e.currentTarget).data("submission")
      )
      
    createDataTable: (collection) ->
      console.debug "SubmissionsTableView#createDataTable", @.$('table')
      aoColumns = [
          {
            sTitle: "Project Key"
            mDataProp: "projectKey"
            fnRender: (oObj, sVal) ->
              "<a href='/release/#{collection.release}/submissions/#{sVal}'>#{sVal}</a>"
          }
          {
            sTitle: "State"
            mDataProp: "state"
            fnRender: (oObj, sVal) ->
              sVal.replace '_', ' '
          }
          {
            sTitle: "Report"
            mDataProp: null
            bSortable: false
            fnRender: (oObj) ->
              switch oObj.aData.state
                when "VALID", "SIGNED OFF"
                  """
                    <a href='/release/#{collection.release}/submissions/#{oObj.aData.projectKey.replace(/<.*?>/g, '')}#report'>View</a>
                  """
                else ""
          }
          {
            sTitle: ""
            mDataProp: null
            bSortable: false
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
      
      @.$('table').dataTable
        sDom:
          "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
        sPaginationType: "bootstrap"
        oLanguage:
          "sLengthMenu": "_MENU_ releases per page"
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
              
        fnServerData: (sSource, aoData, fnCallback) ->
          fnCallback collection.toJSON()
