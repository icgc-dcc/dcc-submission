define (require) ->
  CollectionView = require 'views/base/collection_view'
  CompactReleaseView = require 'views/release/compact_release_view'
  template = require 'text!views/templates/release/collection.handlebars'
 
  'use strict'

  class ReleaseCollectionView extends CollectionView
    template: template
    template = null
    autoRender: false
    
    container: '#releases-table'
    containerMethod: 'html'
    tagName: 'table'
    className: "releases table table-striped"
    id: "releases"
    listSelector: 'tbody'
    
    initialize: ->
      console.debug "ReleasesCollectionView#initialize", @collection, @el
      super
      @collection.fetch()
      
    getView: (item) ->
      console.debug 'ReleasesView#getView', item
      new CompactReleaseView model: item