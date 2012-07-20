define (require) ->
  Release = require 'models/release'

  "use strict"

  class NextRelease extends Release
    urlKey: "id"
    urlPath: ->
      "nextRelease/"

    defaults:
      "submissions": [
        {"projectKey": "p1", "state": "SIGNED_OFF"}
        {"projectKey": "p2", "state": "VALID"}
        {"projectKey": "p3", "state": "QUEUED"}
        {"projectKey": "p4", "state": "INVALID"}
        {"projectKey": "p5", "state": "NOT_VALIDATED"}
      ]

    initialize: ->
      console.debug? 'NextRelease#initialize', @

    queue: (attributes, options)->
      @urlPath = ->
        "nextRelease/queue"
      
      @attributes = attributes
      
      @save(attributes, options)

    signOff: (attributes, options)->
      @urlPath = ->
        "nextRelease/signed"
      
      @attributes = attributes
      
      @save(attributes, options)