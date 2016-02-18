/* globals JSONEditor */

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

  angular.module('ViewerApp', [])
    .controller('ViewerController', function ($scope, $location, $http, $timeout) {

      // Renderer and dictionary logic
      $scope.tableViewer = null;
      $scope.dictUtil = null;

      // params
      $scope.vFrom = '';
      $scope.vTo = '';
      $scope.viewMode = 'graph';
      $scope.q = '';
      $scope.dataType = 'all';

      $scope.hideUnusedCodeLists = true;

      // Query timer
      var qPromise = null;

      var webserviceURL = 'http://hsubmission-dcc.oicr.on.ca:5380/ws';

      // Master sync
      $scope.update = function () {
        var search = $location.search();
        console.log('update', search);

        if (search.vFrom && search.vFrom !== '') {
          $scope.vFrom = search.vFrom;
        }

        if (search.vTo && search.vTo !== '') {
          $scope.vTo = search.vTo;
        }
        //if (search.viewMode) $scope.viewMode = search.viewMode;
        $scope.viewMode = search.viewMode || 'graph';
        $scope.dataType = search.dataType || 'all';
        $scope.q = search.q || '';
        $scope.isReportOpen = search.isReportOpen === 'true' ? true : false;

        $scope.render();
      };

      // Init
      $http.get(webserviceURL + '/dictionaries').success(function (dictionaryList) {
        // Grab the codelist
        $http.get(webserviceURL + '/codeLists').success(function (codeLists) {
          console.log('Done AJAX calls');

          var codelistMap = {};
          codeLists.forEach(function (c) {
            codelistMap[c.name] = c;
          });

          $scope.codeLists = codeLists;
          $scope.dictUtil = new dictionaryApp.DictionaryUtil(dictionaryList);
          $scope.tableViewer = new dictionaryApp.TableViewer($scope.dictUtil, codelistMap);
          $scope.isReportOpen = false;

          // FIXME: need better 'sorting'
          $scope.vFrom = $scope.dictUtil.versionList[1];
          $scope.vTo = $scope.dictUtil.versionList[0];

          // Externalized function
          $scope.tableViewer.toggleNodeFunc = function () {
            $scope.$apply(function () {
              var search = $location.search();
              search.viewMode = $scope.viewMode === 'table' ? 'graph' : 'table';
              search.dataType = $scope.tableViewer.selectedDataType;
              $location.search(search);
            });
          };

          $scope.tableViewer.toggleDataTypeFunc = function () {
            $scope.$apply(function () {
              console.log('asdf'); //very nice
              var search = $location.search();
              search.dataType = $scope.tableViewer.selectedDataType;
              $location.search(search);
            });
          };


          var container = document.getElementById('jsonviewer');
          var options = {
            mode: 'view'
          };
          var editor = new JSONEditor(container, options);
          $scope.jsonEditor = editor;

          startWatcher();
        });

      });

      function startWatcher() {
        $scope.$watch(function () {
          return $location.search();
        }, function () {
          $scope.update();
        }, true);
      }

      $scope.changeView = function () {
        console.log('change view');
        var search = $location.search();
        // search.viewMode = $scope.viewMode === 'table'? 'graph':'table';
        search.viewMode = $scope.viewMode;

        if (search.viewMode === 'graph') {
          delete search.dataType;
        }
        $location.search(search);
      };

      $scope.goto = function (view, type) {
        var search = $location.search();
        search.viewMode = view;
        search.dataType = type;
        delete search.q;
        $location.search(search);
      };


      $scope.switchDictionary = function () {
        var search = $location.search();
        search.vFrom = encodeURIComponent($scope.vFrom);
        search.vTo = encodeURIComponent($scope.vTo);
        $location.search(search);
      };

      $scope.doFilter = function () {
        $timeout.cancel(qPromise);
        $scope.tableViewer.filter($scope.q);

        qPromise = $timeout(function () {
          var search = $location.search();
          var txt = $scope.q;
          search.q = txt;

          $scope.tableViewer.filter($scope.q);
          $location.search(search);
        }, 300);
      };


      $scope.render = function () {
        var versionFrom = $scope.vFrom;
        var versionTo = $scope.vTo;
        var viewMode = $scope.viewMode;
        var query = $scope.q;
        var dataType = $scope.dataType;

        console.log('Render', versionFrom, versionTo, viewMode, query, dataType);
        if (viewMode === 'table') {
          $scope.tableViewer.showDictionaryTable(versionFrom, versionTo);
          $scope.tableViewer.selectDataType(dataType);
        } else {
          $scope.tableViewer.showDictionaryGraph(versionFrom, versionTo);
        }
        $scope.tableViewer.filter(query);
        $scope.generateChangeList();

        $scope.codeLists.forEach(function (codeList) {
          codeList.coverage = $scope.dictUtil.getCodeListCoverage(codeList.name, versionTo).sort();
        });

        $scope.codeListsFiltered = $scope.codeLists;

        if ($scope.hideUnusedCodeLists === true) {
          $scope.codeListsFiltered = _.filter($scope.codeLists, function (codeList) {
            return codeList.coverage.length > 0;
          });
        }

        $scope.jsonEditor.set($scope.dictUtil.getDictionary(versionTo));
      };


      $scope.generateChangeList = function () {
        var versionFrom = $scope.vFrom;
        var versionTo = $scope.vTo;

        $scope.changeReport = $scope.dictUtil.createDiffListing(versionFrom, versionTo);

        $scope.changeReport.changed = $scope.changeReport.fieldsChanged.map(function (field) {
          return field.fileType + '|' + field.fieldName;
        }).join('\n');

        $scope.changeReport.added = $scope.changeReport.fieldsAdded.map(function (field) {
          return field.fileType + '|' + field.fieldName;
        }).join('\n');

        $scope.changeReport.removed = $scope.changeReport.fieldsRemoved.map(function (field) {
          return field.fileType + '|' + field.fieldName;
        }).join('\n');

      };
  });
})();