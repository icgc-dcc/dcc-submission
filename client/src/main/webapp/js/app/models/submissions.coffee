define (require) ->
  Collection = require 'models/base/collection'
  Submission = require 'models/submission'

  "use strict"

  class Submissions extends Collection
    model: Submission
    urlPath: ->
      "releases/"
      
    parse: (response) ->
      response = response.submissions
      response