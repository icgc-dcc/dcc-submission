define (require) ->
  Model = require 'models/base/model'

  "use strict"

  class Submission extends Model
    idAttribute: "projectKey"
