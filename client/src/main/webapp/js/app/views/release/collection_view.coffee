define (require) ->
  DataTableView = require 'views/base/data_table_view'
  CompactReleaseView = require 'views/release/compact_release_view'
  template = require 'text!views/templates/release/collection.handlebars'
  utils = require 'lib/utils'
  
  'use strict'

  class ReleaseCollectionView extends DataTableView
    template: template
    template = null
    autoRender: true
    
    container: '#releases-table'
    containerMethod: 'html'
    tagName: 'table'
    className: "releases table table-striped"
    id: "releases"
    
    initialize: ->
      console.debug "ReleasesCollectionView#initialize", @collection, @el
      super
      
      @subscribeEvent "completeRelease", @update
    
    createDataTable: (collection) ->
      console.debug "ReleasesCollectionView#createDataTable"
      aoColumns = [
          {
            sTitle: "Name"
            mDataProp: "name"
            fnRender: (oObj, sVal) ->
              "<a href='/releases/#{sVal}'>#{sVal}</a>"
          }
          { sTitle: "State", mDataProp: "state" }
          {
            sTitle: "Release Date"
            mDataProp: "releaseDate"
            fnRender: (oObj, sVal) ->
              if sVal then utils.date(sVal) else "<em>Unreleased</em>"
          }
          { sTitle: "Projects", mDataProp: "submissions.length" }
          {
            sTitle: ""
            mDataProp: null
            bSortable: false
            bVisable: utils.is_admin
            fnRender: (oObj) ->
              if not utils.is_released(oObj.aData.state)
                """
                  <a
                    id="complete-release-popup-button"
                    data-toggle="modal"
                    href="#complete-release-popup">
                    Complete
                  </a>
                """
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

    #getView: (item) ->
    #  #console.debug 'ReleaseCollectionView#getView', item
    #  new CompactReleaseView model: item