define (require) ->
  View = require 'views/base/view'
  ReleaseTableView = require 'views/release/release_table_view'
  CompleteReleaseView = require 'views/release/complete_release_view'
  template = require 'text!views/templates/release/releases.handlebars'
 
  'use strict'

  class ReleasesView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'html'
    autoRender: true
    tagName: 'div'
    id: 'releases-view'
    
    initialize: ->
      console.debug "ReleasesView#initialize", @collection
      super
      @delegate 'click', '#complete-release-popup-button', @completeReleasePopup

    completeReleasePopup: (e) ->
      console.debug "ReleaseView#completeRelease", e
      @subview('CompleteReleases'
        new CompleteReleaseView()
      ) unless @subview 'CompleteReleases'
      
    render: ->
      super
      @subview(
        'ReleasesTable'
        new ReleaseTableView {
          @collection
          el: @.$("#releases-table")
        }
      )