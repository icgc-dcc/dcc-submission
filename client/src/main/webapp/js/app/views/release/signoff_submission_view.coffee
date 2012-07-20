define (require) ->
  Chaplin = require 'chaplin'
  View = require 'views/base/view'
  NextRelease = require 'models/next_release'
  template = require 'text!views/templates/release/signoff_submission.handlebars'

  'use strict'

  class SignOffSubmissionView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'append'
    autoRender: true
    tagName: 'div'
    className: "modal fade"
    id: 'signoff-submission-popup'
    
    initialize: ->
      console.debug "SignOffSubmissionView#initialize"
      
      @.$('.modal').modal "show": true
      
      @delegate 'click', '#signoff-submission-button', @signOffSubmission
      
    signOffSubmission: ->
      console.debug "SignOffSubmissionView#signOffSubmission"
      nextRelease = new NextRelease()
      nextRelease.signOff ["p1"],
        success: (data) ->
          @.$('.modal').modal 'hide'
          Chaplin.mediator.publish "signOffSubmission", data
          
        error: (model, error) ->
          console.log "here"
          err = error.statusText + error.responseText
          alert = @.$('.alert.alert-error')
          
          if alert.length
            alert.text err
          else
            @.$('fieldset').before "<div class='alert alert-error'>#{err}</div>"