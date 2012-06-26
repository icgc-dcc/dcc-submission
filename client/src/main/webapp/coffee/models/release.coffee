define (require) ->
  Model = require 'models/base/model'	

  "use strict"

  class Release extends Model
    urlKey: "name"
    urlPath: ->
      "releases/"

    fetch: ->
      console.debug 'Release#fetch', @
      @id = @get "identifier"
      super