'use strict';

angular.module('DictionaryViewerApp')
  .filter('prettyPrintView', function(DictionaryAppConstants) {
    var prettyPrintViewMap = DictionaryAppConstants.PRETTY_VIEW_MAP;

    return function(input) {
      return angular.isDefined(prettyPrintViewMap[input]) ? prettyPrintViewMap[input] : input;
    };
  });