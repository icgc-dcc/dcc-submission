define [
  'chaplin'
  'models/Release'
  'views/hello_world_view'
], (Chaplin, Release, HelloWorldView) ->
  'use strict'

  class HelloWorldController extends Chaplin.Controller

    title: 'Hello World'

    historyURL: (params) ->
      ''

    show: (params) ->
      console.debug 'HelloWorldController#show'
      @model = new Release()
      console.debug @model
      @model.fetch()
      console.debug @model
      @view = new HelloWorldView model: @model
      