define (require) ->
  CollectionView = require 'views/base/collection_view'
  Release = require 'models/release'
  CompactReleaseView = require 'views/compact_release_view'
  template = require 'text!views/templates/releases.handlebars'
 
  'use strict'

  class ReleasesView extends CollectionView
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'html'
    autoRender: true    
    tagName: 'div'
    id: 'releases-view'
    listSelector: 'tbody'
    
    getView: (item) -> 
      console.debug 'ReleasesView#getView', item
      new CompactReleaseView model: item
    