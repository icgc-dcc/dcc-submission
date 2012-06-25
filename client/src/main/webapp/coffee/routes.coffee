define ->
  'use strict'

  # The routes for the application. This module returns a function.
  # `match` is match method of the Router
  (match) ->

    match 'releases/:release_name', 'release#show'
    match 'releases', 'release#list'
    match 'projects', 'project#list'
    match 'logout', 'auth#logout'
