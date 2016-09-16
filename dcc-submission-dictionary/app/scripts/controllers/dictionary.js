
////////////////////////////////////////////////////////////////////////////////
// DCC Dictionary viewer
// Browse and compare ICGC data dictionaries
//
// Note:
// - The viewer does not support dictionary versions before 0.6c, this also means
//   the viewer assumes a fixed column order
// - Comparing A=>B will not yield the same result as B=>A due to how new/remove
//   items are calculated
//
// Dependencies:
// - Core: D3, Underscore
// - Wrapper: angularJS
// - Styles: HighlightJS, JS-Beautify, regex-colorizer JS, Bootstrap
//
////////////////////////////////////////////////////////////////////////////////
'use strict';

var dictionaryApp = dictionaryApp || {};

(function() {

  angular.module('DictionaryViewerApp', [])
    .constant('DictionaryBaseURLConstants' , {
      DEV: 'http://localhost:5380',
      BETA: '',
      PROD: 'https://submissions.dcc.icgc.org'
    })
    .constant('DictionaryAppConstants', {
      VIEWS: ['graph', 'table', 'codelist', 'report'],
      PRETTY_VIEW_MAP: {
        graph: 'Overview', table: 'Details', codelist: 'Codelists', report: 'Changes Report'
      },
      DETAIL_FORMAT_TYPES: {
        table: 'Table',
        json: 'JSON'
      }
    })
    .controller('DictionaryViewerController', function (DictionaryBaseURLConstants) {
      this.DictionaryBaseURLConstants = DictionaryBaseURLConstants;
  });
})();