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
      
      @subscribeEvent "completeRelease", @fetch
      @delegate 'click', '#complete-release-popup-button', @completeReleasePopup
    
    fetch: ->
      @model.fetch()
    
    completeReleasePopup: (e) ->
      console.debug "ReleaseView#completeRelease"
      @subview('CompleteReleases'
        new CompleteReleaseView {
          @model
        }
      )
      
    render: ->
      console.debug "ReleaseView#render"
      super
      @subview('SubmissionsTable'
        new SubmissionTableView {
          @model
          el: @.$("#submissions-table")
        }
      )
    