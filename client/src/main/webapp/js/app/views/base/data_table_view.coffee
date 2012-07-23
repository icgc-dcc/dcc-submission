define (require) ->
  Chaplin = require 'chaplin'
  View = require 'views/base/view'

  'use strict'

  class DataTableView extends View

    initialize: ->
      # console.debug "DataTableView#initialize", @collection, @el
      super
      @update()

    update: ->
      # console.debug "DataTableView#update", @collection
      @collection.fetch {
        success: (collection,response)=>
          @renderAsDataTable(collection)
      }
      
    renderAsDataTable: (collection) ->
      # console.debug "DataTableView#renderAsDataTable"
      if @.$('table.dataTable').length
        @updateDataTable collection
      else
        @createDataTable collection
    
    updateDataTable: (collection) ->
      #console.debug "DataTableView#updateDataTable"
      dt = @.$('table').dataTable()
      dt.fnClearTable()
      dt.fnAddData collection.toJSON()
      
    createDataTable: ->
      throw new Error( 
        "The DataTableView#createDataTable function must be overridden"
      )
    
