define (require) ->
  View = require 'views/base/view'
  template = require 'text!views/templates/compact_release.handlebars'

  'use strict'

  class CompactReleaseView extends View
    template: template
    template = null
    className: 'release'
    tagName: 'tr'
    
    initialize: ->
      console.debug @model
      
      @$('.modal').modal "show": true
        
      @delegate 'click', '.btn', @completeRelease
      
    completeRelease: (e) -> 
      console.debug "CompactReleaseView#completeRelease", @model, e
      @$('#completeRelease').modal('show')
      