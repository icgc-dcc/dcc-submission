define [
  'views/base/collection_view'
  'text!views/templates/releases.handlebars'
  'models/release'
  'views/release_view'
], (CollectionView, template, Release, ReleaseView) ->

  'use strict'

  class ReleasesView extends CollectionView
    template: template
    template = null
    
    container: '#page-container'
    autoRender: true    
    tagName: 'div'
    id: 'my-releases'
    listSelector: 'ol'
    fallbackSelector: '.fallback'
    loadingSelector: '.loading'
          
    initialize: ->
      console.debug 'ReleasesView#initialize', @collection
      super
      
    getView: (item) -> 
      console.debug 'ReleasesView#getView', item
      new ReleaseView model: item
    