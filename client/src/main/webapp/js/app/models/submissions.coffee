define (require) ->
  Collection = require 'models/base/collection'
  Submission = require 'models/submission'

  "use strict"

  class Submissions extends Collection
    model: Submission
    urlPath: ->
      "releases/#{@release}"
    
    initialize: (release) ->
      console.debug 'Submissions#initialize', @
      @release = release
      
    parse: (response) ->
      response = response.submissions
      response