define [
  'views/base/collection_view'
  'text!views/templates/projects.handlebars'
  'models/project'
  'views/project_view'
], (CollectionView, template, Project, ProjectView) ->

  'use strict'

  class ProjectsView extends CollectionView
    template: template
    template = null
    
    container: '#page-container'
    autoRender: true    
    tagName: 'div'
    id: 'my-projects'
    listSelector: 'ol'
    fallbackSelector: '.fallback'
    loadingSelector: '.loading'
          
    initialize: ->
      console.debug 'ProjectsView#initialize', @collection
      super
      
    getView: (item) -> 
      console.debug 'ProjectsView#getView', item
      new ProjectView model: item