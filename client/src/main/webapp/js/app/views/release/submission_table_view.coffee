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
    
    container: '#submissions-table'
    containerMethod: 'html'
    tagName: 'table'
    className: "submissions table"
    id: "submissions"
    
    initialize: ->
      console.debug "SubmissionsTableView#initialize", @collection, @el
      super
      
      @subscribeEvent "signOffSubmission", @update
      @subscribeEvent "validateSubmission", @update
      
      @delegate 'click', '#signoff-submission-popup-button', @signOffSubmissionPopup
      @delegate 'click', '#validate-submission-popup-button', @validateSubmissionPopup
    
    signOffSubmissionPopup: (e) ->
      console.debug "ReleaseView#signOffSubmissionPopup", e
      @subview('signOffSubmissionView'
        new signOffSubmissionView()
      ) unless @subview 'signOffSubmissionView'
    
    validateSubmissionPopup: (e) ->
      console.debug "ReleaseView#validateSubmissionPopup", e
      @subview('validateSubmissionView'
        new validateSubmissionView()
      ) unless @subview 'validateSubmissionView'
    
    createDataTable: (collection) ->
      console.debug "SubmissionsTableView#createDataTable"
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
            fnRender: (oObj, sVal) ->
              switch oObj.aData.state
                when "VALID", "SIGNED OFF"
                  """
                    <a href='/release/#{collection.release}/submissions/#{sVal}#report'>View</a>
                  """
          }
          {
            sTitle: ""
            mDataProp: null
            fnRender: (oObj, sVal) ->
              switch oObj.aData.state
                when "VALID"
                  """
                    <a id="signoff-submission-popup-button"
                       data-toggle="modal"
                       href='#signoff-submission-popup'>
                       Sign Off
                    </a>
                  """
                when "NOT VALIDATED", "INVALID"
                  """
                    <a id="validate-submission-popup-button"
                       data-toggle="modal"
                       href='#validate-submission-popup'>
                       Validate
                    </a>
                 """
                else
                  ""  
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
          switch aData.state
            when "SIGNED OFF"
              $(nRow).addClass('alert alert-info')
            when "VALID"
              $(nRow).addClass('alert alert-success')
            when "QUEUED"
              $(nRow).addClass('alert alert-warning')
            when "INVALID"
              $(nRow).addClass('alert alert-error')
              
        fnServerData: (sSource, aoData, fnCallback) ->
          fnCallback collection.toJSON()
