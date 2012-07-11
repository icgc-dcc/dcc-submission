define (require) ->
  Chaplin = require 'chaplin'
  BaseController = require 'controllers/base/controller'
  Project = require 'models/project'
  Projects = require 'models/projects'
  ProjectView = require 'views/project_view'
  ProjectsView = require 'views/projects_view'

  'use strict'

  class ProjectController extends BaseController

    title: 'Projects'

    historyURL: (params) ->
      ''

    show: (params) ->
      console.debug 'ProjectController#show'
      @model = new Project()
      @view = new ProjectView {@model}
      @model.fetch()
      
    list: (params) ->
      console.debug 'ProjectController#list'
      @collection = new Projects()
      @collection.fetch()
      console.debug 'ProjectController#list', @collection
      @view = new ProjectsView {@collection}
