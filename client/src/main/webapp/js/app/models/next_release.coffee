define (require) ->
  Release = require 'models/release'

  "use strict"

  class NextRelease extends Release
    urlKey: "id"
    urlPath: ->
      "nextRelease/"

    defaults:
      "submissions": [
        {"projectKey": "project1", "state": "SIGNED_OFF"}
        {"projectKey": "project2", "state": "VALID"}
        {"projectKey": "project3", "state": "NOT_VALIDATED"}
        {"projectKey": "project4", "state": "INVALID"}
        {"projectKey": "project5", "state": "NOT_VALIDATED"}
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