define (require) ->
  Chaplin = require 'chaplin'
  BaseController = require 'controllers/base/controller' 
  Release = require 'models/release'
  Releases = require 'models/releases'
  ReleaseView = require 'views/release_view'
  ReleasesView = require 'views/releases_view'

  'use strict'

  class ReleaseController extends BaseController

    title: 'Releases'

    historyURL: (params) ->
      ''

    show: (params) ->
      console.debug 'ReleaseController#show', params
      @title = params.id
      @model = new Release {name: params.name}
      @view = new ReleaseView {@model}
      @model.fetch()

    list: (params) ->
      console.debug 'ReleaseController#list'
      @collection = new Releases()
      @collection.fetch()
      console.debug 'ReleaseController#list', @collection
      @view = new ReleasesView {@collection}