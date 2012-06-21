define [
  'views/base/view'
  'text!views/templates/release.handlebars'
], (View, template) ->
  'use strict'

  class ReleaseView extends View

    # Save the template string in a prototype property.
    # This is overwritten with the compiled template function.
    # In the end you might want to used precompiled templates.
    template: template
    template = null

    className: 'release'

    # Automatically append to the DOM on render
    container: '#page-container'
    # Automatically render after initialize
    autoRender: true