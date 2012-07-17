define (require) ->
  Chaplin = require 'chaplin'
  View = require 'views/base/view'
  NextRelease = require 'models/next_release'
  template = require 'text!views/templates/release/complete_release.handlebars'

  'use strict'

  class CompleteReleaseView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'append'
    autoRender: true
    tagName: 'div'
    className: "modal fade"
    id: 'complete-release-popup'
    
    initialize: ->
      console.debug "CompleteReleaseView#initialize"
      @.$('.modal').modal "show": true
      
      @delegate 'click', '#complete-release-button', @completeRelease
      
    completeRelease: ->
      console.debug "CompleteReleaseView#completeRelease"
      nextRelease = new NextRelease()
      nextRelease.save {name: @.$('#nextRelease').val()}
        success: (data) ->
          @.$('.modal').modal('hide')
          # publish completeRelease here
          Chaplin.mediator.publish "completeRelease"
          
        error: (model, error) ->
          err = error.statusText + error.responseText
          alert = @.$('.alert.alert-error')
          
          if alert.length
            alert.text(err)
          else
            @.$('fieldset').before("<div class='alert alert-error'>#{err}</div>")