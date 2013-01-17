Application = require 'application'

# Initialize the application on DOM ready event.
$(document).on 'ready', ->
  $.cookie.defaults.secure = false
  #date = new Date()
  #date.setTime(date.getTime() + (30 * 60 * 1000))
  #$.cookie.defaults.expires = date

  app = new Application()
  app.initialize()
