define (require) -> 
  Controller = require 'controllers/base/controller'
  Navigation = require 'models/navigation'
  NavigationView = require 'views/navigation_view'
  
  class NavigationController extends Controller
    initialize: ->
      super
      @model = new Navigation()
      @view = new NavigationView({@model})