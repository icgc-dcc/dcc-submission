/* globals JSONEditor, dictionaryApp */

'use strict';

angular.module('DictionaryViewerApp')
  .constant('DictionaryViewerConstants', {
    EVENTS: {
      RENDER_COMPLETE: 'event.dictionary-viewer.rendered'
    },
    SCROLL_OFFSET: 60
  })
  .directive('dictionaryViewer', function($http, $location, $anchorScroll, $templateCache, $compile){
    return {
      restrict: 'EA',
      //templateUrl: 'scripts/views/dictionary-viewer-directive.html',
      scope: {
        baseDictionaryUrl: '@',
        showHeaderNav: '@',
        hideGraphLegend: '@',
        // Configurable Params
        searchQuery: '=',
        filterDataType: '='
        //
      },
      controller: function($rootScope, $scope, DictionaryService, $timeout, DictionaryViewerConstants) {
        var _controller = this,
            _firstRun = true;

        // Renderer and dictionary logic
        _controller.tableViewer = null;
        _controller.dictUtil = null;

        _controller.shouldShowHeaderNav = $scope.showHeaderNav === 'false' ? false : true;
        _controller.shouldHideGraphLegend = $scope.hideGraphLegend === 'true' ? true : false;

        // params
        _controller.vFrom =  $scope.fromDictVersion || '';
        _controller.vTo = $scope.toDictVersion ||'';
        _controller.viewMode = $scope.viewMode || 'graph';
        _controller.q = $scope.searchQuery || '';
        _controller.dataType = $scope.filterDataType || 'all';

        _controller.hideUnusedCodeLists = true;

        // Query timer
        var qPromise = null;

        _controller.viewTypes = DictionaryService.getViewTypes();

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
          _controller.viewMode = DictionaryService.getCurrentViewType();
          _controller.dataType = search.dataType || 'all';
          _controller.q = search.q || '';
          _controller.isReportOpen = search.isReportOpen === 'true' ? true : false;

          _controller.render();
        };

        // Init
        DictionaryService.init($scope.baseDictionaryUrl || '').then(function(dictionaryData) {
          var codeLists = dictionaryData.codeList;

          console.log('Done AJAX calls');

          var codelistMap = {};
          codeLists.forEach(function (c) {
            codelistMap[c.name] = c;
          });

          _controller.codeLists = codeLists;
          _controller.dictUtil = DictionaryService.getDictionaryUtils();
          _controller.dictionaryVersionList = DictionaryService.getDictionaryVersionList();
          _controller.tableViewer = new dictionaryApp.TableViewer(_controller.dictUtil, codelistMap,
            ! _controller.shouldHideGraphLegend);
          _controller.isReportOpen = false;

          // FIXME: need better 'sorting'
          var versionRange = DictionaryService.dictionaryVersionRange();
          _controller.vFrom = versionRange.from;
          _controller.vTo = versionRange.to;

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




        function startWatcher() {
          $scope.$watch(function () {
            return $location.search();
          }, function () {
            _controller.update();
          }, true);

          if (angular.isDefined($scope.searchQuery)) {
            $scope.$watch(function() {
                return $scope.searchQuery;
              },
              function(searchVal, oldSearchVal) {
                if (searchVal !== oldSearchVal) {
                  _controller.q = searchVal;
                  _controller.doFilter();
                }
            });
          }
        }

        _controller.setView = DictionaryService.setView;

        _controller.goto = function (view, type) {
          var search = $location.search();
          search.viewMode = view;
          search.dataType = type;
          delete search.q;
          $location.search(search);
        };


        _controller.switchDictionary = DictionaryService.setDictionaryFilterRange;

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

          $rootScope.$broadcast(DictionaryViewerConstants.EVENTS.RENDER_COMPLETE, null);

          // Skip the rest if our view mode isn't table

          if (viewMode !== 'table') {
            return;
          }


          if (_firstRun) {
            _initListeners(versionFrom, versionTo, viewMode, query, dataType);

            _firstRun = false;

            if ($location.hash()) {
              $anchorScroll.yOffset = DictionaryViewerConstants.SCROLL_OFFSET;
              $anchorScroll();
            }
          }


          // Grab the links (for the table views) and initialize the appropriate hover/focus listeners
          var anchors = jQuery('.header-text-link');

          if (anchors.length) {
            anchors.off('hover.dictionary').hover(function () {
              _bindAnchors.call(this, versionFrom, versionTo, viewMode, query, dataType);
            },
              function () {
            });
            anchors.off('focus.dictionary').focus(function () {
              _bindAnchors.call(this, versionFrom, versionTo, viewMode, query, dataType);
            });
          }
        };

        _controller.generateChangeList = function() {
          _controller.changeReport = DictionaryService.generateChangeList();
        };

        function _bindAnchors(versionFrom, versionTo, viewMode, query, dataType) {
          var hoveredEl = jQuery(this); // jshint ignore:line



          var href = '#?viewMode=' + viewMode + '&q=' + query +
                     '&dataType=' + (dataType || 'all') + '&vFrom=' + versionFrom +
                     '&vTo=' + versionTo + '#' + hoveredEl.attr('id');


          hoveredEl.attr('href', href);

        }

        function _initListeners() {
          var wrapperEl = jQuery('.vis-wrapper');

          wrapperEl.click(function(e) {
            var clickedEl = jQuery(e.target);

            if (! clickedEl.hasClass('header-text-link')) {
              return;
            }

            var id = clickedEl.attr('id');

            if ( ! id) {
              return;
            }

            $location.hash(id);
            $anchorScroll.yOffset = DictionaryViewerConstants.SCROLL_OFFSET;
            $anchorScroll();

          });


        }
      },
      controllerAs: 'dictionaryViewerCtrl',
      link: function(scope, element, attrs) {
        var relTemplateURL = 'scripts/views/dictionary-viewer-directive.html',
            baseURL = '';

        if (angular.isDefined(attrs.templateUrl)) {
          baseURL = attrs.templateUrl;
        }
        else if (angular.isDefined(attrs.baseDictionaryUrl)) {
          baseURL = attrs.baseDictionaryUrl;
        }

        if (baseURL) {
          baseURL += '/';
        }

        $http.get(baseURL + relTemplateURL, {cache: $templateCache}).success(function(tplContent){
          element.replaceWith($compile(tplContent)(scope));
        });
      }
    };

  });