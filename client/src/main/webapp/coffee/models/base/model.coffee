define (require) ->
	Chaplin = require 'chaplin'

	"use strict"

	class Model extends Chaplin.Model
		# Place your application-specific model features here
		apiRoot: "http://localhost:3001/ws/"
		urlKey: "_id"

		urlPath: ->
			console.debug 'Model#urlPath'
			''

		urlRoot: ->
			console.debug 'Model#urlRoot'
			urlPath = @urlPath()
			if urlPath
				@apiRoot + urlPath
			else if @collection
				@collection.url()
			else
				throw new Error('Model must redefine urlPath')

		url: ->
			console.debug 'Model#url'
			base = @urlRoot()
			base

		fetch: (options) ->
			console.debug 'Model#fetch'
			@trigger 'loadStart'
			(options ?= {}).success = =>
				@trigger 'load'
			super