define (require, exports, module) ->
  Model = require 'models/base/model' 

  "use strict"

  class Project extends Model
    urlKey: "name"

    url: ->
      "ws/projects/"
      
    fetch: ->
      console.debug "Project#fetch", @
      @id = @get "identifier"
      super
