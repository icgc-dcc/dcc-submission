exports.config =
  # See http://brunch.readthedocs.org/en/latest/config.html for documentation.
  paths:
    public: 'target/classes/public'
  files:
    javascripts:
      joinTo:
        'javascripts/app.js': /^app/
        'javascripts/vendor.js': /^vendor/
        'test/javascripts/test.js': /^test(\/|\\)(?!vendor)/
        'test/javascripts/test-vendor.js': /^test(\/|\\)(?=vendor)/
      order:
        # Files in `vendor` directories are compiled before other files
        # even if they aren't specified in order.before.
        before: [
          'vendor/scripts/console-helper.js',
          'vendor/scripts/jquery-1.8.2.js',
          'vendor/scripts/underscore-1.4.0.js',
          'vendor/scripts/backbone-0.9.2.js'
        ]

    stylesheets:
      joinTo:
        'stylesheets/app.css': /^(app|vendor)/
        'test/stylesheets/test.css': /^test/
      order:
        before: [
          'vendor/styles/style.less',
          'vendor/styles/m-styles.min.css'
        ]
        after: ['vendor/styles/helpers.css']

    templates:
      defaultExtension: 'handlebars'
      joinTo: 'javascripts/app.js'

  conventions:
    ignored: /^vendor(\/|\\)styles(\/|\\)bootstrap/

  coffeelint:
    pattern: /^app\/.*\.coffee$/
    options:
      max_line_length:
        value: 120
        level: "error"
        
  framework: 'chaplin'
