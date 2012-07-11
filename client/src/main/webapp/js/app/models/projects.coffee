define (require) ->
  Collection = require 'models/base/collection' 

  "use strict"

  class Project extends Collection
    url: ->
      "/ws/projects"
      
    initialize: ->
      console.debug "Projects#initialize"
      super
