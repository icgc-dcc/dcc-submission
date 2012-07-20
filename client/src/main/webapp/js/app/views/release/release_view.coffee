define (require) ->
  View = require 'views/base/view'
  CompleteReleaseView = require 'views/release/complete_release_view'
  SubmissionTableView = require 'views/release/submission_table_view'
  template = require 'text!views/templates/release/release.handlebars'

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
      
      @delegate 'click', '#complete-release-popup-button', @completeReleasePopup
    
    completeReleasePopup: (e) ->
      console.debug "ReleaseView#completeRelease", e
      @subview('CompleteReleases'
        new CompleteReleaseView()
      ) unless @subview 'CompleteReleases'
        
    render: ->
      super
      @subview(
        'SubmissionsTable'
        new SubmissionTableView {
          collection: @model.get "submissions"
          el: @.$("#submissions-table")
        }
      )