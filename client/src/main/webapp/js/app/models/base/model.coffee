define (require) ->
  Backbone = require 'backbone'
  Chaplin = require 'chaplin'
  utils = require 'lib/utils'
  
  "use strict"

  class Model extends Chaplin.Model
    # Place your application-specific model features here
    apiRoot: "http://localhost:3001/ws/"
    urlKey: "_id"

    urlPath: ->
      console.debug? 'Model#urlPath'
      ''

    urlRoot: ->
      console.debug? 'Model#urlRoot'
      urlPath = @urlPath()
      if urlPath
        @apiRoot + urlPath
      else if @collection
        @collection.url()
      else
        throw new Error('Model must redefine urlPath')

    url: ->
      console.debug? 'Model#url'
      base = @urlRoot()
      url = if @get(@urlKey)?
        base + encodeURIComponent(@get(@urlKey))
      else
        base
      url
    
    sync: (method, model, options) ->
      console.debug? 'Model#sync', method, model, options
      
      options.beforeSend = utils.sendAuthorization

      Backbone.sync(method, model, options)