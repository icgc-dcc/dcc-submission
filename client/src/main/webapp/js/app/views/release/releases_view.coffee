define (require) ->
  View = require 'views/base/view'
  ReleaseCollectionView = require 'views/release/collection'
  template = require 'text!views/templates/release/releases.handlebars'
 
  'use strict'

  class ReleasesView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'html'
    autoRender: true
    tagName: 'div'
    id: 'releases-view'
    
    initialize: ->
      console.debug "ReleasesView#initialize", @collection
      super
      #@delegate 'click', '.btn.complete', @completeRelease
    
    render: ->
      super
      @subview(
        'ReleasesCollection'
        new ReleaseCollectionView {
          @collection
          el: @.$("#releases-table")
        }
      )