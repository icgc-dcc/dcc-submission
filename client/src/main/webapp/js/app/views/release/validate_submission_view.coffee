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
      console.debug "ValidateSubmissionView#initialize", @options.submission
      super
      
      @delegate 'click', '#validate-submission-button', @validateSubmission
      
    validateSubmission: (e) ->
      console.debug "ValidateSubmissionView#completeRelease"
      nextRelease = new NextRelease()
      
      @$el.modal 'hide'
      @options.submission.set "state", "QUEUED"
      Chaplin.mediator.publish "validateSubmission"
      
      nextRelease.queue [@options.submission.get "projectKey"]    