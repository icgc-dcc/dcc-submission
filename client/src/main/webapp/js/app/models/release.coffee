define (require) ->
  Model = require 'models/base/model'	

  "use strict"

  class Release extends Model
    urlKey: "name"
    urlPath: ->
      "releases/"

    initialize: ->
      #console.debug? 'Release#show', @
