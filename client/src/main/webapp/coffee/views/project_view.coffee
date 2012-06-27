define [
  'views/base/view'
  'text!views/templates/project.handlebars'
], (View, template) ->
  'use strict'

  class ReleaseView extends View

    # Save the template string in a prototype property.
    # This is overwritten with the compiled template function.
    # In the end you might want to used precompiled templates.
    template: template
    template = null

    className: 'project'

    # Automatically append to the DOM on render
    container: '#page-container'
    # Automatically render after initialize
    autoRender: true
    tagName: 'li'
    
    initialize: ->
      console.debug 'Projects#initialize', @model
      super
      
    render: ->
      console.debug 'Projects#render', @model
      super
