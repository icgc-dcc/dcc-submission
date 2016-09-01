'use strict';
var LIVERELOAD_PORT = 35729;
var modRewrite = require('connect-modrewrite');
var lrSnippet = require('connect-livereload')({ port: LIVERELOAD_PORT });
var mountFolder = function (connect, dir) {
  return connect.static(require('path').resolve(dir));
};

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'

module.exports = function (grunt) {
  // load all grunt tasks
  require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);
  require('time-grunt')(grunt);

  var configProvider = require('./dcc-grunt-tasks/ICGC-grunt-config-provider')(grunt);

  // configurable paths
  var yeomanConfig = {
    app: 'app',
    dist: '../target/classes/public/dictionary',
    moduleDist: 'dist',
    tmp: '.tmp',
  };


  try {
    yeomanConfig.app = require('./bower.json').appPath || yeomanConfig.app;
  }
  catch (e) {
  }

  grunt.initConfig({
    'bower-install-simple': configProvider.setConfigForTask('bower-install-simple', function() {

        /**
         * Bower configuration
         * See: https://www.npmjs.com/package/grunt-bower-install-simple
         */

        var config =  {options: { color: true } };

        if (configProvider.isProductionBuild()) {
          config.prod = { options: { production: true, interactive: false, forceLatest: false } };
        }
        else {
          config.dev = { options: { production: false,  interactive: true, forceLatest: false } };
        }

        return config;
      })
      // Gets the default dev config object in this context because
      // we have yet to set a default
      .getConfigForTask('bower-install-simple'),
    yeoman: yeomanConfig,
    watch: {
      livereload: {
        options: {
          livereload: LIVERELOAD_PORT
        },
        files: [
          '<%= yeoman.app %>/**/*.html',
          '{.tmp,<%= yeoman.app %>}/styles/**/*.css',
          '{.tmp,<%= yeoman.app %>}/scripts/**/*.js',
          '<%= yeoman.app %>/images/**/*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      }
    },
    connect: {
      options: {
        port: 9000,
        protocol: 'http',
        // Change this to '0.0.0.0' to access the server from outside.
        hostname: 'localhost'
      },
      livereload: {
        options: {
          middleware: function (connect) {
            return [
              modRewrite([
                '!\\.html|\\images|\\.js|\\.css|\\.png|\\.jpg|\\.woff|\\.ttf|\\.svg ' +
                '/index.html [L]'
              ]),
              lrSnippet,
              mountFolder(connect, '.tmp'),
              mountFolder(connect, yeomanConfig.app)
            ];
          }
        }
      },
      test: {
        options: {
          port: 9009,
          middleware: function (connect) {
            return [
              mountFolder(connect, '.tmp'),
              mountFolder(connect, 'test')
            ];
          }
        }
      },
      dist: {
        options: {
          middleware: function (connect) {
            return [
              modRewrite([
                '!\\.html|\\images|\\.js|\\.css|\\.png|\\.jpg|\\.woff|\\.ttf|\\.svg ' +
                '/' + yeomanConfig.developIndexFile + ' [L]'
              ]),
              mountFolder(connect, yeomanConfig.dist)
            ];
          }
        }
      }
    },
    open: {
      server: {
        url: 'http://localhost:<%= connect.options.port %>'
      }
    },
    clean: {
      options: { force: true },
      dist: {
        files: [
          {
            dot: true,
            src: [
              '<%= yeoman.dist %>/*',
              '!<%= yeoman.dist %>/.git*'
            ]
          }
        ]
      },
      cleanTempBuildFiles: {
        files: [
          {
            dot: true,
            src: [
              '.tmp'
            ]
          }
        ]
      },
      server: '.tmp'
    },
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      all: [
        'Gruntfile.js',
        '<%= yeoman.app %>/scripts/**/*.js',
        '!<%= yeoman.app %>/scripts/vendor/*.js'
      ]
    },
    // not used since Uglify task does concat,
    // but still available if needed
    concat: {
      dist: {
        src: [
          'npm-module-scripts/imports.js',
          '<%= yeoman.app %>/scripts/**/*.js',
          '<%= yeoman.tmp %>/templates.js'
        ],
        dest: '<%= yeoman.moduleDist %>/dictionary.js',
      }
    },
    // Renames files for browser caching purposes
    filerev: {
      dist: {
        src: [
          //'<%= yeoman.dist %>/scripts/{,*/**/}*.js',
          //'<%= yeoman.dist %>/styles/{,*/**/}*.css',
          '<%= yeoman.dist %>/images/{,*/**/}*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      }
    },
    useminPrepare: {
      html: '<%= yeoman.app %>/index.html',
      options: {
        dest: '<%= yeoman.dist %>',
        flow: {
          html: {
            steps: {
              // js: ['concat'],
              css: ['concat', 'cssmin']
            },
            post: {}
          }
        }
      }
    },
    usemin: {
      html: ['<%= yeoman.dist %>/**/*.html'],
      css: [
        '<%= yeoman.dist %>/styles/{,*/**/}*.css'
      ],
      js: [
        '<%= yeoman.dist %>/scripts/**/*.js'
      ],

      options: {
        assetsDirs: ['<%= yeoman.dist %>','<%= yeoman.dist %>/images']
      }
    },
    imagemin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>',
            src:  [
              '/images/**/*.{png,jpg,jpeg}',
            ],
            dest: '<%= yeoman.dist %>/images'
          }
        ]
      }
    },
    cssmin: {
      // By default, your `index.html` <!-- Usemin Block --> will take care of
      // minification. This option is pre-configured if you do not wish to use
      // Usemin blocks.
      // dist: {
      //   files: {
      //     '<%= yeoman.dist %>/styles/main.css': [
      //       '.tmp/styles/{,*/}*.css',
      //       '<%= yeoman.app %>/styles/{,*/}*.css'
      //     ]
      //   }
      // }
    },
    htmlmin: {
      dist: {
        options: {
          /*removeCommentsFromCDATA: true,
           // https://github.com/yeoman/grunt-usemin/issues/44
           //collapseWhitespace: true,
           collapseBooleanAttributes: true,
           removeAttributeQuotes: true,
           removeRedundantAttributes: true,
           useShortDoctype: true,
           removeEmptyAttributes: true,
           removeOptionalTags: true*/
        },
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>',
            src: ['*.html', 'views/**/*.html'],
            dest: '<%= yeoman.dist %>'
          }
        ]
      }
    },
    // Put files not handled in other tasks here
    copy: {
      dist: {
        files: [
          {
            expand: true,
            dot: true,
            cwd: '<%= yeoman.app %>',
            dest: '<%= yeoman.dist %>',
            src: [
              '*.{ico,png,txt}',
              '.htaccess',
              'bower_components/**',
              'images/**/*.{gif,webp,svg,png,jpg}',
              'fonts/*',
              'styles/font-awesome.min.css',
              'scripts/views/**/*.html',
              'scripts/**/*.map'
            ]
          },
          {
            expand: true,
            cwd: '.tmp/images',
            dest: '<%= yeoman.dist %>/images',
            src: [
              'generated/*'
            ]
          },
          {
            expand: true,
            cwd: '<%= yeoman.app %>/bower_components/bootstrap/fonts/',
            dest: '<%= yeoman.dist %>/fonts',
            src: [
              '*.*'
            ]
          },
          {
            expand: true,
            cwd: '<%= yeoman.app %>/bower_components/jsoneditor/dist/img/',
            dest: '<%= yeoman.dist %>/styles/img',
            src: [
              '*.*'
            ]
          }
        ]
      }
    },
    concurrent: {
      server: {
        options: {
          //debugInfo: true
        }
      },
      dist: [
        //'compass:dist',
        'imagemin',
        'htmlmin'
      ]
    },
    karma: {
      unit: {
        configFile: './karma.conf.js',
        singleRun: true
      }
    },
    // ngAnnotate tries to make the code safe for minification automatically by
    // using the Angular long form for dependency injection. It doesn't work on
    // things like resolve or inject so those have to be done manually.
    ngAnnotate: {
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.dist %>/scripts',
          src: '*.js',
          dest: '<%= yeoman.dist %>/scripts'
        }]
      }
    },
    uglify: {
      generated: {
        options: {
          sourceMap: true,
          compress: true,
          mangle: true,
          sourceMapIncludeSources: true
        },
        files: [{
          expand: true,
          cwd: '<%= yeoman.dist %>/scripts',
          src: [
            '*.js'
          ],
          dest: '<%= yeoman.dist %>/scripts',
          ext: '.js'
        }]
      },
    },
    ngtemplates:    {
      app:          {
        cwd:        'app',
        src:        'scripts/views/*.html',
        dest:       '<%= yeoman.tmp %>/templates.js',
        options:    {
          htmlmin:  { collapseWhitespace: true, collapseBooleanAttributes: true },
          bootstrap: function (module, script) {
            return 'angular.module(\'DictionaryViewerApp\').run([\'$templateCache\', function($templateCache) {' +
              script + '} ]);';
          }
        }
      }
    }
  });

  grunt.registerTask('bower-install', ['bower-install-simple']);

  grunt.registerTask('server', function (target) {
    if (target === 'dist') {
      return grunt.task.run(['build',
        'connect:dist:keepalive']);
    }

    grunt.task.run([
      'ICGC-setBuildEnv:development',
      'clean:server',
      'concurrent:server',
      'connect:livereload',
      //'open',
      'watch'
    ]);
  });

  /*grunt.registerTask('test', [
    'clean:server',
    'concurrent:test',
    'connect:test',
    'karma'
  ]);*/

  grunt.registerTask('build', [
    'ICGC-setBuildEnv:production',
    'clean:dist',
    'bower-install',
    'jshint',
    //'karma',
    'useminPrepare',
    'concurrent:dist',
    // 'concat',
    'copy:dist',
    'ngAnnotate',
    'cssmin',
    'uglify',
    'filerev',
    'usemin',
    'clean:cleanTempBuildFiles'
  ]);

  grunt.registerTask('default', [
    //'jshint',
    'test',
    'build'
  ]);
};
