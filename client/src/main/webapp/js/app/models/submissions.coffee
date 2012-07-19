define (require) ->
  Collection = require 'models/base/collection'
  Submission = require 'models/submission'

  "use strict"

  class Submissions extends Collection
    model: Submission
    urlPath: ->
      "releases/#{@release}"
    
    initialize: (models, options)->
      console.debug 'Submissions#initialize', @, models, options
      @release = options.release
      
    parse: (response) ->
      response.submissions
