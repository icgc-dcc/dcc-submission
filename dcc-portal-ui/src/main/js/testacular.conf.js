// Testacular configuration

// base path, that will be used to resolve files and exclude
basePath = '';

// list of files / patterns to load in the browser
files = [
  JASMINE,
  JASMINE_ADAPTER,
  'app/scripts/vendor/angular.js',
  'test/vendor/angular-mocks.js',
  'app/scripts/*.js',
  'app/scripts/**/*.js',
  'test/unit/**/*.js'
];

// list of files to exclude
exclude = [
  'app/scripts/vendor/bootstrap.min.js'
];

// web server port
port = 9203;

// cli runner port
runnerPort = 9303;

// enable / disable colors in the output (reporters and logs)
colors = true;
growl = true;

// level of logging
// possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
logLevel = LOG_INFO;

// enable / disable watching file and executing tests whenever any file changes
autoWatch = true;

// Start these browsers, currently available:
// - Chrome
// - ChromeCanary
// - Firefox
// - Opera
// - Safari
// - PhantomJS
//browsers = ['PhantomJS', 'Chrome', 'ChromeCanary', 'Firefox', 'Safari'];
browsers = ['PhantomJS'];

// If browser does not capture in given timeout [ms], kill it
// CLI --capture-timeout 5000
captureTimeout = 5000;

// Auto run tests on start (when browsers are captured) and exit
// CLI --single-run --no-single-run
singleRun = false;

// report which specs are slower than 500ms
// CLI --report-slower-than 500
reportSlowerThan = 500;

// use dots reporter, as travis terminal does not support escaping sequences
// possible values: 'dots', 'progress', 'junit'
// CLI --reporters progress
reporters = ['dots', 'junit'];

junitReporter = {
  // will be resolved to basePath (in the same way as files/exclude patterns)
  outputFile: 'test/unit.xml'
};
