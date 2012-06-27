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
      # This should be a Handlebars helper in lib/view_helper
      # Along with the 'Complete' button check: ex {{completeRelease state}}
      releaseDate = if @model.get "releaseDate"
        moment(@model.get "releaseDate").format("YYYY-M-D")
      else
        "<em>Unreleased</em>"
      @model.set "releaseDate", releaseDate
      