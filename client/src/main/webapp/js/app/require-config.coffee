# Configure the AMD module loader
requirejs.config
  # The path where your JavaScripts are located
  baseUrl: '/js/app/'
  # Specify the paths of vendor libraries
  paths:
    jquery: '../vendor/jquery'
    jqSerializeObject: '../vendor/jquery.ba-serializeobject',
    dataTables: '../vendor/jquery.dataTables.min',
    DT_bootstrap: '../vendor/dataTables.bootstrap2',
    underscore: '../vendor/underscore'
    backbone: '../vendor/backbone'
    chaplin: '../vendor/chaplin'
    bootstrap: '../vendor/bootstrap.min'
    moment: '../vendor/moment'
    text: '../vendor/require-text'
    handlebars: '../vendor/handlebars'
    
  # Underscore and Backbone are not AMD-capable per default,
  # so we need to use the AMD wrapping of RequireJS
  shim: 
    backbone:
      deps: ['underscore', 'jquery']
      exports: 'Backbone'
    underscore:
      exports: '_'
    bootstrap:
      deps: ['jquery']
    jqSerializeObject:
      deps: ['jquery']
    dataTables:
      deps: ['jquery']
    DT_bootstrap:
      deps: ['dataTables']
    
  # For easier development, disable browser caching
  # Of course, this should be removed in a production environment
  , urlArgs: 'bust=' +  (new Date()).getTime()


# Add any extra deps that should be loaded with jquery
define "base", [
  'jquery'
  'bootstrap'
  'jqSerializeObject'
  'moment'
  'dataTables'
  'DT_bootstrap'
], ($) -> $

# Bootstrap the application
require ['dcc_submission_application', 'base'], (DccSubmissionApplication) ->
  dcc = new DccSubmissionApplication()
  dcc.initialize()