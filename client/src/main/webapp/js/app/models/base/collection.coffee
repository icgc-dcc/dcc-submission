define (require) ->
  Backbone = require 'backbone'
  Chaplin = require 'chaplin'
  utils = require 'lib/utils'

  class Collection extends Chaplin.Collection
    # Place your application-specific collection features here
    apiRoot: "http://localhost:3001/ws/"

    urlPath: ->
      console.debug 'Collection#urlPath'
      ''

    url: ->
      console.debug 'Collection#url'
      @apiRoot + @urlPath()
      
    sync: (method, model, options) ->
      console.debug 'Collection#sync', method, model, options
      
      options.beforeSend = utils.sendAuthorization

      Backbone.sync(method, model, options)