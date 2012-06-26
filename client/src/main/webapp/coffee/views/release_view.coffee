define [
  'views/base/view'
  'text!views/templates/release.handlebars'
], (View, template) ->
  'use strict'

  class ReleaseView extends View
    template: template
    template = null
    className: 'release'
    #autoRender: true
    tagName: 'li'
    #container: 'my-releases'
    
    initialize: ->
      console.debug 'ReleaseView#initialize', @model
      super
      
    render: ->
      console.debug 'ReleaseView#render', @model
      super
    
