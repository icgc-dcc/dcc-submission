define [
  'chaplin'
], (Chaplin) ->

  class Collection extends Chaplin.Collection
    # Place your application-specific collection features here
    
    fetch: (options) ->
      console.debug 'Collection#fetch'
      
      @trigger 'loadStart'
      (options ?= {}).success = =>
        @trigger 'load'
      options.beforeSend = utils.sendAuthorization

      super(options)