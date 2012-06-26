define [
  'handlebars'
  'chaplin'
  'lib/utils'
  'lib/view_helper' # Just load the view helpers, no return value
], (Handlebars, Chaplin, utils) ->
  'use strict'

  class View extends Chaplin.View
  
    getTemplateData: ->
      Model = require 'models/base/model'
      serialize = (object) ->
        result = {}
        for key, value of object
          result[key] = if value instanceof Model
            serialize value.getAttributes()
          else
            value
        result
  
      modelAttributes = @model and @model.getAttributes()
      templateData = if modelAttributes
        # Return an object which delegates to the returned attributes
        # object so a custom getTemplateData might safely add and alter
        # properties (at least primitive values).
        utils.beget serialize modelAttributes
      else
        {}
  
      # If the model is a Deferred, add a flag to get the Deferred state
      if @model and typeof @model.state is 'function'
        templateData.resolved = @model.state() is 'resolved'
  
      templateData
  
    getTemplateFunction: ->

      # Template compilation
      # --------------------

      # This demo uses Handlebars templates to render views.
      # The template is loaded with Require.JS and stored as string on
      # the view prototype. On rendering, it is compiled on the
      # client-side. The compiled template function replaces the string
      # on the view prototype.
      #
      # In the end you might want to precompile the templates to JavaScript
      # functions on the server-side and just load the JavaScript code.
      # Several precompilers create a global JST hash which stores the
      # template functions. You can get the function by the template name:
      #
      # templateFunc = JST[@templateName]

      template = @template

      if typeof template is 'string'
        # Compile the template string to a function and save it
        # on the prototype. This is a workaround since an instance
        # shouldn’t change its prototype normally.
        templateFunc = Handlebars.compile template
        @constructor::template = templateFunc
      else
        templateFunc = template

      templateFunc
