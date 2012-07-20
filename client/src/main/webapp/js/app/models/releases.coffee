define (require) ->
  Collection = require 'models/base/collection'
  Release = require 'models/release'

  "use strict"

  class Releases extends Collection
    model: Release
    urlPath: ->
      "releases/"
    
    comparator: (release) ->
        -release.get('releaseDate')