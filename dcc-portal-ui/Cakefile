httpProxy = require "http-proxy"

# ANSI Terminal Colors
bold = "\x1b[0;1m"
green = "\x1b[0;32m"
reset = "\x1b[0m"
red = "\x1b[0;31m"

task "proxy", 'setup proxy', -> startServer()

startServer = ->
  # Setup proxy to redirect /ws/* request to the DCC REST server  
  options =
    router: #"localhost/ws/": "***REMOVED***:5380/ws/"
      "localhost/ws/": "localhost:8080/"
      "localhost": "localhost:3501"

  httpProxy.createServer(options).listen 3502, ->
    timeLog "Development Server listening on http://localhost:3502...\n"


timeLog = (message) -> process.stdout.write "#{bold}#{(new Date).toLocaleTimeString()}#{reset} - #{message}"
