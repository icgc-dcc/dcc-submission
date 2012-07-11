define ->
  'use strict'

  # The routes for the application. This module returns a function.
  # `match` is match method of the Router
  (match) ->

    # Releases
    match 'releases/:name', 'release#show'
    match 'releases/:name/', 'release#show'
    match 'releases', 'release#list'
    match 'releases/', 'release#list'
    
    # Logout
    match 'logout', 'auth#logout'
    match 'logout/', 'auth#logout'
