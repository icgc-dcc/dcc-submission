define (require) ->
  View = require 'views/base/view'
  template = require 'text!views/templates/release.handlebars'

  'use strict'

  class ReleaseView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'html'
    autoRender: false    
    tagName: 'div'
    id: 'release-view'
    
    initialize: ->
      console.debug 'ReleaseView#initialize', @model
      super
      @modelBind 'change', @render