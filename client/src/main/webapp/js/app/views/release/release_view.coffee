define (require) ->
  View = require 'views/base/view'
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
        
    render: ->
      super
      @subview(
        'SubmissionsTable'
        new SubmissionTableView {
          collection: @model.get "submissions"
          el: @.$("#submissions-table")
        }
      )