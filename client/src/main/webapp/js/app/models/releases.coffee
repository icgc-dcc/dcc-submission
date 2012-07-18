define (require) ->
  Collection = require 'models/base/collection'
  Release = require 'models/release'

  "use strict"

  class Releases extends Collection
    model: Release
    urlPath: ->
      "releases/"

    initialize: ->
      console.debug "Releases#initialize"
      # subscribe to completeRelease
      #@subscribeEvent "completeRelease", @fetch
    
    comparator: (release) ->
        -release.get('releaseDate')