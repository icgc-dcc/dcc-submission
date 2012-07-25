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
      console.debug "SignOffSubmissionView#initialize", @options.submission 
      super
         
      @delegate 'click', '#signoff-submission-button', @signOffSubmission
      
    signOffSubmission: (e) ->
      console.debug "SignOffSubmissionView#completeRelease"
      nextRelease = new NextRelease()
      
      @$el.modal 'hide'
      @options.submission.set "state", "SIGNED OFF"
      Chaplin.mediator.publish "validateSubmission"
      
      nextRelease.signOff [@options.submission.get "projectKey"]