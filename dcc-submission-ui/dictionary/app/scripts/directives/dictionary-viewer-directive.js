/* globals JSONEditor, dictionaryApp */

'use strict';

angular.module('DictionaryViewerApp')
  .constant('DictionaryViewerConstants', {
    EVENTS: {
      RENDER_COMPLETE: 'event.dictionary-viewer.rendered'
    },
    SCROLL_OFFSET: 60
  })
  .directive('dictionaryViewer', function($http, $location, $anchorScroll,
                                          $templateCache, $compile, DictionaryAppConstants) {
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
            _firstRun = true,
            _previousVersion = {from: null, to: null};

        // Renderer and dictionary logic
        _controller.tableViewer = null;
        _controller.dictUtil = null;
        _controller.getCurrentView = DictionaryService.getCurrentViewType;
        _controller.changeReport = null;

        _controller.shouldShowHeaderNav = $scope.showHeaderNav === 'false' ? false : true;
        _controller.shouldHideGraphLegend = $scope.hideGraphLegend === 'true' ? true : false;

        var searchParams = $location.search();

        // params
        _controller.vFrom = searchParams.vFrom || '';
        _controller.vTo = searchParams.vTo ||'';
        _controller.q = $scope.searchQuery || '';
        _controller.dataType = $scope.filterDataType || 'all';
        _controller.selectedDetailFormatType = DictionaryAppConstants.DETAIL_FORMAT_TYPES.table;

        _controller.detailFormatTypes = DictionaryAppConstants.DETAIL_FORMAT_TYPES;

        _controller.hideUnusedCodeLists = searchParams.hideUnusedCodeLists === 'false' ? false : true;

        // Query timer
        var qPromise = null;

        _controller.viewTypes = DictionaryService.getViewTypes();

        // Master sync
        _controller.update = function (shouldForceUpdate) {
          var search = $location.search();
          console.log('update', search);

          if (search.vFrom && search.vFrom !== '') {
            _controller.vFrom = search.vFrom;
          }

          if (search.vTo && search.vTo !== '') {
            _controller.vTo = search.vTo;
          }

          if (search.hideUnusedCodeLists === 'false') {
            _controller.hideUnusedCodeLists = false;
          }
          else {
            _controller.hideUnusedCodeLists = true;
          }

          //if (search.viewMode) _controller.viewMode = search.viewMode;
          _controller.dataType = search.dataType || 'all';
          _controller.q = search.q || '';
          _controller.isReportOpen = search.isReportOpen === 'true' ? true : false;

          _controller.render(shouldForceUpdate || false);
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

          var handleGraphToggle = function () {
            $scope.$apply(function () {
              var search = $location.search();
              search.viewMode = 'table';
              search.dataType = _controller.tableViewer.selectedDataType;
              $location.search(search);
            });
          };

          // Externalized function
          _controller.tableViewer.toggleNodeFunc = handleGraphToggle;

          _controller.tableViewer.toggleDataTypeFunc = handleGraphToggle;

          _controller.filterChangesReport = function(changeObj) {
            var query = _controller.q || '',
                shouldIncludeObj = true;

            if (! query) {
              return shouldIncludeObj;
            }

            var normalizeStr = function(s) {
              return s.trim().toLowerCase().replace(/[\s_]+/g, '').replace(/\s{2,}/g, ' ');
            }, 
            normalizedQuery = normalizeStr(query);

            // Ignore strings with only spaces
            if (! normalizedQuery) {
              return shouldIncludeObj;
            }

            // Now for the check default to not including in the filter
            shouldIncludeObj = false;

            ['fileType','fieldName'].map(function (key) {

              if ( typeof changeObj[key] === 'string' &&
                   normalizeStr(changeObj[key]).indexOf(normalizedQuery) >= 0 ) {
                shouldIncludeObj = true;
              }
            });


            return shouldIncludeObj;
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
            var searchTriggers = {};

            angular.copy($location.search(), searchTriggers);

            // A view change should never re-trigger a render event
            delete searchTriggers.viewMode;

            return searchTriggers;
          }, function () {
            _controller.update(true);
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

        $scope.$watch(function() {
            return _controller.hideUnusedCodeLists;
          },
          function(newVal, oldVal) {

            if (newVal === oldVal) {
              return;
            }

            var search = $location.search();

            if (_controller.hideUnusedCodeLists) {
              search.hideUnusedCodeLists = 'true';
            }
            else {
              search.hideUnusedCodeLists = 'false';
            }

            $location.search(search);
        });

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


        _controller.render = function (shouldForceRender) {
          var versionFrom = _controller.vFrom;
          var versionTo = _controller.vTo;
          var viewMode = _controller.getCurrentView();
          var query = _controller.q;
          var dataType = _controller.dataType;

          if (shouldForceRender !== true &&
              _previousVersion.from === versionFrom &&
              _previousVersion.to === versionTo) {
            console.log('No Version Change Render Aborting...');
            return;
          }

          _previousVersion.from = versionFrom;
          _previousVersion.to = versionTo;


          // Ensure the Dictionary Service has the correct version ranges before rendering
          DictionaryService.dictionaryVersionRange(versionFrom, versionTo);

          console.log('Render', versionFrom, versionTo, viewMode, query, dataType);

          _controller.tableViewer.showDictionaryTable(versionFrom, versionTo);
          _controller.tableViewer.selectDataType(dataType);
          _controller.tableViewer.showDictionaryGraph(versionFrom, versionTo);


          _controller.tableViewer.filter(query);
          _controller.generateChangeList();
          
          _controller.dictUtil.getDictionary(versionTo).then(function (dictTo) {
            _controller.codeLists.forEach(function (codeList) {
              codeList.coverage = _controller.dictUtil.getCodeListCoverage(codeList.name, dictTo).sort();
            });

            _controller.codeListsFiltered = _controller.codeLists;

            if (_controller.hideUnusedCodeLists === true) {
              _controller.codeListsFiltered = _.filter(_controller.codeLists, function (codeList) {
                return codeList.coverage.length > 0;
              });
            }
          });

          if (_controller.jsonEditor) {
            var dictionaryJSON = {};
            _controller.dictUtil.getDictionary(versionTo).then(function (dictionariesJSON) {
              if (_controller.dataType !== 'all' &&
                dictionariesJSON && angular.isDefined(dictionariesJSON.files)) {

                angular.copy(dictionariesJSON, dictionaryJSON);

                // Filter the JSON based on the data type
                var dictionaryFiles = [];

                for (var i = 0; i < dictionaryJSON.files.length; i++) {
                  var file = dictionaryJSON.files[i];

                  if (file.name === dataType) {
                    dictionaryFiles = dictionaryFiles.concat(dictionaryFiles, file);
                  }

                }

                dictionaryJSON.files = dictionaryFiles;
              }
              else {
                dictionaryJSON = dictionariesJSON;
              }

              _controller.jsonEditor.set(dictionaryJSON);

              if (dictionaryJSON.files.length === 1) {
                _controller.jsonEditor.expandAll();
              }
            });

          }



          $rootScope.$broadcast(DictionaryViewerConstants.EVENTS.RENDER_COMPLETE, null);

          // Skip the rest if our view mode isn't table

          if (_controller.getCurrentView() !== 'table') {
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
          DictionaryService.generateChangeList().then(function (report) {
            $scope.$applyAsync(function () {
              _controller.changeReport = report;
            });
          });
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