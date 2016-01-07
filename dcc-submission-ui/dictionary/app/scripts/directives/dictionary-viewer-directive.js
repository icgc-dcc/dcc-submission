/* globals JSONEditor, dictionaryApp */

'use strict';

angular.module('DictionaryViewerApp')
  .directive('dictionaryViewer', function(){
    return {
      restrict: 'EA',
      templateUrl: 'scripts/views/dictionary-viewer-directive.html',
      scope: {
        baseDictionaryUrl: '@',
        showHeaderNav: '@',
        hideGraphLegend: '@'
      },
      controller: function($scope, $location, $http, $timeout) {
        var _controller = this;

        // Renderer and dictionary logic
        _controller.tableViewer = null;
        _controller.dictUtil = null;

        _controller.shouldShowHeaderNav = $scope.showHeaderNav === 'false' ? false : true;
        _controller.shouldHideGraphLegend = $scope.hideGraphLegend === 'true' ? true : false;

        // params
        _controller.vFrom = '';
        _controller.vTo = '';
        _controller.viewMode = 'graph';
        _controller.q = '';
        _controller.dataType = 'all';

        _controller.hideUnusedCodeLists = true;

        // Query timer
        var qPromise = null;

        var webserviceURL = $scope.baseDictionaryUrl || '/ws';

        // Master sync
        _controller.update = function () {
          var search = $location.search();
          console.log('update', search);

          if (search.vFrom && search.vFrom !== '') {
            _controller.vFrom = search.vFrom;
          }

          if (search.vTo && search.vTo !== '') {
            _controller.vTo = search.vTo;
          }
          //if (search.viewMode) _controller.viewMode = search.viewMode;
          _controller.viewMode = search.viewMode || 'graph';
          _controller.dataType = search.dataType || 'all';
          _controller.q = search.q || '';
          _controller.isReportOpen = search.isReportOpen === 'true' ? true : false;

          _controller.render();
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

            _controller.codeLists = codeLists;
            _controller.dictUtil = new dictionaryApp.DictionaryUtil(dictionaryList);
            _controller.tableViewer = new dictionaryApp.TableViewer(_controller.dictUtil, codelistMap,
                                                                ! _controller.shouldHideGraphLegend);
            _controller.isReportOpen = false;

            // FIXME: need better 'sorting'
            _controller.vFrom = _controller.dictUtil.versionList[1];
            _controller.vTo = _controller.dictUtil.versionList[0];

            // Externalized function
            _controller.tableViewer.toggleNodeFunc = function () {
              $scope.$apply(function () {
                var search = $location.search();
                search.viewMode = _controller.viewMode === 'table' ? 'graph' : 'table';
                search.dataType = _controller.tableViewer.selectedDataType;
                $location.search(search);
              });
            };

            _controller.tableViewer.toggleDataTypeFunc = function () {
              $scope.$apply(function () {
                console.log('asdf'); //very nice
                var search = $location.search();
                search.dataType = _controller.tableViewer.selectedDataType;
                $location.search(search);
              });
            };


            var container = document.getElementById('jsonviewer');
            var options = {
              mode: 'view'
            };
            var editor = new JSONEditor(container, options);
            _controller.jsonEditor = editor;

            startWatcher();
          });

        });

        function startWatcher() {
          $scope.$watch(function () {
            return $location.search();
          }, function () {
            _controller.update();
          }, true);
        }

        _controller.changeView = function () {
          console.log('change view');
          var search = $location.search();
          // search.viewMode = _controller.viewMode === 'table'? 'graph':'table';
          search.viewMode = _controller.viewMode;

          if (search.viewMode === 'graph') {
            delete search.dataType;
          }
          $location.search(search);
        };

        _controller.goto = function (view, type) {
          var search = $location.search();
          search.viewMode = view;
          search.dataType = type;
          delete search.q;
          $location.search(search);
        };


        _controller.switchDictionary = function () {
          var search = $location.search();
          search.vFrom = encodeURIComponent(_controller.vFrom);
          search.vTo = encodeURIComponent(_controller.vTo);
          $location.search(search);
        };

        _controller.doFilter = function () {
          $timeout.cancel(qPromise);
          _controller.tableViewer.filter(_controller.q);

          qPromise = $timeout(function () {
            var search = $location.search();
            var txt = _controller.q;
            search.q = txt;

            _controller.tableViewer.filter(_controller.q);
            $location.search(search);
          }, 300);
        };


        _controller.render = function () {
          var versionFrom = _controller.vFrom;
          var versionTo = _controller.vTo;
          var viewMode = _controller.viewMode;
          var query = _controller.q;
          var dataType = _controller.dataType;

          console.log('Render', versionFrom, versionTo, viewMode, query, dataType);
          if (viewMode === 'table') {
            _controller.tableViewer.showDictionaryTable(versionFrom, versionTo);
            _controller.tableViewer.selectDataType(dataType);
          } else {
            _controller.tableViewer.showDictionaryGraph(versionFrom, versionTo);
          }
          _controller.tableViewer.filter(query);
          _controller.generateChangeList();

          _controller.codeLists.forEach(function (codeList) {
            codeList.coverage = _controller.dictUtil.getCodeListCoverage(codeList.name, versionTo).sort();
          });

          _controller.codeListsFiltered = _controller.codeLists;

          if (_controller.hideUnusedCodeLists === true) {
            _controller.codeListsFiltered = _.filter(_controller.codeLists, function (codeList) {
              return codeList.coverage.length > 0;
            });
          }

          _controller.jsonEditor.set(_controller.dictUtil.getDictionary(versionTo));
        };


        _controller.generateChangeList = function () {
          var versionFrom = _controller.vFrom;
          var versionTo = _controller.vTo;

          _controller.changeReport = _controller.dictUtil.createDiffListing(versionFrom, versionTo);

          _controller.changeReport.changed = _controller.changeReport.fieldsChanged.map(function (field) {
            return field.fileType + '|' + field.fieldName;
          }).join('\n');

          _controller.changeReport.added = _controller.changeReport.fieldsAdded.map(function (field) {
            return field.fileType + '|' + field.fieldName;
          }).join('\n');

          _controller.changeReport.removed = _controller.changeReport.fieldsRemoved.map(function (field) {
            return field.fileType + '|' + field.fieldName;
          }).join('\n');

        };
      },
      controllerAs: 'dictionaryViewerCtrl'
    };

  });