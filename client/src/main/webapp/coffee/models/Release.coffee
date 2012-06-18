define (require, exports, module) ->
	Model = require 'models/base/model'	

	"use strict"

	class Release extends Model
		urlKey: "name"

		urlPath: ->
			"releases/"