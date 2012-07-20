define (require) ->
  Model = require 'models/base/model'
  Submissions = require 'models/submissions'

  "use strict"

  class Release extends Model
    urlKey: "name"
    urlPath: ->
      "releases/"
    
    parse: (response) ->
      if response?.submissions
        response.submissions = new Submissions response.submissions, {"release": response.name}
      response
