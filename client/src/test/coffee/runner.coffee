requirejs = require 'requirejs'
wrench = require "wrench"
fs = require "fs"
jsdom = require('jsdom').jsdom
chai = require 'chai'
should = chai.should()
sinonChai = require("sinon-chai");
chai.use sinonChai

global.define = requirejs
global.document = jsdom()
global.window = global.document.createWindow()

requirejs.config
  # This needs to point to the compiled JS files, not the CS files 
  baseUrl: __dirname + '/../../../target/main/webapp/js/app'
  
  # These are copied directly from src/main/webapp/coffee/require-config.coffee
  # and need to be kept up-to-date when that file changes.
  paths:
    jquery: '../vendor/jquery'
    jqSerializeObject: '../vendor/jquery.ba-serializeobject',
    underscore: '../vendor/underscore'
    backbone: '../vendor/backbone'
    chaplin: '../vendor/chaplin'
    bootstrap: '../vendor/bootstrap.min'
    moment: '../vendor/moment'
    text: '../vendor/require-text'
    cs: '../vendor/require-cs'
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

wrench.readdirSyncRecursive(__dirname).map (file) ->
  if fs.lstatSync(__dirname + "/" + file).isFile() and file isnt 'runner.coffee'
    [path, ext] = file.split(".")
    # Uses require-cs plugin so tests do not have to be pre-compiled 
    requirejs ["cs!" + __dirname + "/" + path], -> 
