define (require) ->
  Chaplin = require 'chaplin'
  Release = require 'models/release'
  Releases = require 'models/releases'
  ReleaseView = require 'views/release_view'

  'use strict'

  class ReleaseController extends Chaplin.Controller

    title: 'Releases'

    historyURL: (params) ->
      ''

    show: (params) ->
      console.debug 'ReleaseController#show'
      @model = new Release()
      @view = new ReleaseView {@model}
      @model.fetch()
      