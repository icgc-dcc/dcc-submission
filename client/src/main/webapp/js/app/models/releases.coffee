define (require) ->
	Collection = require 'models/base/collection'	

	"use strict"

	class Releases extends Collection
		url: ->
		  "ws/releases/"
		  
    initialize: ->
      console.debug "Release#initialize"
      # subscribe to completeRelease