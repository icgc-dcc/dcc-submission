define (require) ->
  Chaplin = require 'chaplin'
  Controller = require 'controllers/base/controller'
  
  class AuthController extends Controller
    historyURL: 'auth'

    logout: ->
      localStorage.clear()
      Chaplin.mediator.publish '!logout'
      @redirectTo 'release', 'list'