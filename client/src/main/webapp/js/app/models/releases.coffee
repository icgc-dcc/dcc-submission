define (require) ->
  Collection = require 'models/base/collection'

  "use strict"

  class Releases extends Collection
    urlPath: ->
      "releases/"

    initialize: ->
      console.debug "Releases#initialize"
      # subscribe to completeRelease
      @subscribeEvent "completeRelease", @fetch
      