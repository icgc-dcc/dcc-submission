var angular = require('angular');
require('angular-sanitize');
require('angular-animate');
require('angular-loading-bar');
var _ = require('underscore');
var RegexColorizer = require('regex-colorizer');
var d3 = require('d3');
require('d3-tip')(d3);
var js_beautify = require('js-beautify').js_beautify;
var JSONEditor = require('jsoneditor/dist/jsoneditor.js');
var multiselect = require('bootstrap-multiselect/dist/js/bootstrap-multiselect.js');
global.jsondiffpatch = require('jsondiffpatch');
global.jsondiffpatch.formatters = {
  html: require('jsondiffpatch/src/formatters/html')
};
