'use strict';

angular.module('DictionaryViewerApp')
  .filter('prettyPrintView', function(DictionaryAppConstants) {
    var prettyPrintViewMap = DictionaryAppConstants.PRETTY_VIEW_MAP;

    return function(input) {
      return angular.isDefined(prettyPrintViewMap[input]) ? prettyPrintViewMap[input] : input;
    };
  })
  .filter('sanitize', ['$sce', function($sce) {
    return function(htmlCode){
      return $sce.trustAsHtml(htmlCode);
    };
  }])
  .filter('findDiffs', function (JSONDiffService) {
    return _.memoize(function (data) {
      return data.map(function (node) {
        var diff = JSONDiffService.formatDifferences(node.from, node.to);
        return _.extend({}, node, { diff : diff});
      });
    }, function resolver (data) {
      return _.pluck(data, 'id');
    })
  });