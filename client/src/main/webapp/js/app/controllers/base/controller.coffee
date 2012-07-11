define ['chaplin'], (Chaplin) ->
  'use strict'

  class Controller extends Chaplin.Controller
    # Redirection
    # -----------
    # Should be in chaplin ... maybe using old version

    redirectTo: (arg1, action, params) ->
      @redirected = true
      if arguments.length is 1
        # URL was passed, try to route it
        Chaplin.mediator.publish '!router:route', arg1, (routed) ->
          unless routed
            throw new Error 'Controller#redirectTo: no route matched'
      else
        # Assume controller and action names were passed
        Chaplin.mediator.publish '!startupController', arg1, action, params