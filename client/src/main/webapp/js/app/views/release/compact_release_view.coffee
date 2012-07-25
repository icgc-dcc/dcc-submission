define (require) ->
  View = require 'views/base/view'
  template = require 'text!views/templates/release/compact_release.handlebars'

  'use strict'

  class CompactReleaseView extends View
    template: template
    template = null
    className: 'release'
    tagName: 'tr'
    
    initialize: ->
      #console.debug "CompactReleaseView#initialize", @model
      super
      