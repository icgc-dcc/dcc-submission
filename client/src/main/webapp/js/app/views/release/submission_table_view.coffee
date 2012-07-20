define (require) ->
  DataTableView = require 'views/base/data_table_view'
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
    className: "submissions table table-striped"
    id: "submissions"
    
    initialize: ->
      console.debug "SubmissionsTableView#initialize", @collection, @el
      super
      
      @subscribeEvent "completeRelease", @update
    
    createDataTable: (collection) ->
      console.debug "SubmissionsTableView#createDataTable"
      aoColumns = [
          {
            sTitle: "Project Key"
            mDataProp: "projectKey"
            fnRender: (oObj, sVal) ->
              "<a href='/projects/#{sVal}'>#{sVal}</a>"
          }
          { sTitle: "State", mDataProp: "state" }
          {
            sTitle: "Report"
            mDataProp: (oObj) ->
              if (oObj.state == "VALID" || oObj.state == "SIGNED_OFF")
                """
                  <button
                    class="btn btn-mini btn-primary"
                    id="queue-button"
                    data-toggle="modal"
                    href="">
                    View
                  </button>
                """
              else
                ""
          }
          {
            sTitle: "Actions"
            mDataProp: (oObj) ->
              if(oObj.state == "NOT_VALIDATED")
                """
                  <button
                    class="btn btn-mini btn-primary"
                    id="queue-button"
                    data-toggle="modal"
                    href="">
                    Queue
                  </button>
                """
              else if (oObj.state == "VALID")
                """
                  <button
                    class="btn btn-mini btn-primary"
                    id="sign-off-button"
                    data-toggle="modal"
                    href="">
                    Sign Off
                  </button>
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
        fnServerData: (sSource, aoData, fnCallback) ->
          fnCallback collection.toJSON()
