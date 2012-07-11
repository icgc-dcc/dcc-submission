define (require) ->
	Collection = require 'models/base/collection'	

	"use strict"

	class Releases extends Collection
		url: ->
		  "ws/releases/"