define (require) ->
  Release = require 'models/release'

  "use strict"

  class NextRelease extends Release
    urlKey: "id"
    urlPath: ->
      "nextRelease/"

    defaults:
      "submissions": [{"projectKey": "p1", "state": "SIGNED_OFF"}]

    initialize: ->
      console.debug? 'NextRelease#initialize', @
