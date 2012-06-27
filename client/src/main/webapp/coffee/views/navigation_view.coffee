define (require) -> 
  View = require 'views/base/view'
  template = require 'text!views/templates/navigation.handlebars'
  
  class NavigationView extends View
    template: template
    tagName: 'nav'
    containerMethod: 'html'
    autoRender: true  
    className: 'navigation'
    container: '#navigation-container'
  
    initialize: ->
      console.debug 'NavigationView#initialize', @model
      super
      @modelBind 'change', @render
      @subscribeEvent 'navigation:change', (attributes) =>
        console.debug 'NavigationView#initialize#change', attributes
        @model.clear(silent: yes)
        @model.set attributes