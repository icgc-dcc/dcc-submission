define (require) ->
  Chaplin = require 'chaplin'
  View = require 'views/base/view'
  NextRelease = require 'models/next_release'
  template = require 'text!views/templates/release/validate_submission.handlebars'

  'use strict'

  class ValidateSubmissionView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'append'
    autoRender: true
    tagName: 'div'
    className: "modal fade"
    id: 'validate-submission-popup'
    
    initialize: ->
      console.debug "ValidateSubmissionView#initialize"
      
      @model = new NextRelease()
      @model.fetch()
      @modelBind 'change', @render
      
      @.$('.modal').modal "show": true
      
      @delegate 'click', '#validate-submission-button', @completeRelease
      
    completeRelease: ->
      console.debug "ValidateSubmissionView#completeRelease"
      nextRelease = new NextRelease()
      nextRelease.queue  [@options.projectKey],
        success: (data) ->
          @.$('.modal').modal 'hide'
          Chaplin.mediator.publish "validateSubmission", data
          
        error: (model, error) ->
          err = error.statusText + error.responseText
          alert = @.$('.alert.alert-error')
          
          if alert.length
            alert.text(err)
          else
            @.$('fieldset').before("<div class='alert alert-error'>#{err}</div>")