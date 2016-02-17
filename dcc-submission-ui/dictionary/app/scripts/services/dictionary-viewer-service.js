/* globals dictionaryApp */
'use strict';

angular.module('DictionaryViewerApp')
  .service('DictionaryService', function($location, $http, $q, DictionaryAppConstants) {

    var _service = this,
        _dictionaryList = null,
        _codeList = null,
        _dictionaryUtils = null,
        _versionRange = {from: null, to: null},
        _latestDictionaryVersion = '',
        _DEFAULT_FROM_VERSION_INDEX = 0,
        _DEFAULT_TO_VERSION_INDEX = 0;

    _service.init = _init;
    _service.getCachedDictionaryList = _getCachedDictionaryList;
    _service.getCachedCodeList = _getCachedCodeList;
    _service.getViewTypes = _getViewTypes;
    _service.setDictionaryFilterRange = _setDictionaryFilterRange;
    _service.setView = _setView;
    _service.getDictionaryUtils = _getDictionaryUtils;
    _service.getDictionaryVersionList = _getDictionaryVersionList;
    _service.getCurrentViewType = _getCurrentViewType;
    _service.dictionaryVersionRange = _dictionaryVersionRange;
    _service.generateChangeList = _generateChangeList;
    _service.getLatestDictionaryVersion = _getLatestDictionaryVersion;

    function _init(baseURL) {
      return _getDictionaries(baseURL);
    }


    function _getDictionaries(baseURL) {
      var deferred = $q.defer(),
          webserviceURL = (baseURL || '') + '/ws';


      $http.get(webserviceURL + '/dictionaries')
        .then(function (dictionaryList) {

          // Grab the code list
          $http.get(webserviceURL + '/codeLists')
            .then(function (codeList) {
                _dictionaryList = dictionaryList.data;
                _codeList = codeList.data;

                _dictionaryUtils = new dictionaryApp.DictionaryUtil(_dictionaryList);

                // The version list is sorted in descending order (latest version first)
                // use the latest current version as the default.
                _dictionaryVersionRange(
                  _dictionaryUtils.versionList[_DEFAULT_FROM_VERSION_INDEX],
                  _dictionaryUtils.versionList[_DEFAULT_TO_VERSION_INDEX]
                );

                _latestDictionaryVersion = _dictionaryUtils.versionList[0];

                deferred.resolve({dictionaryList: _dictionaryList, codeList: _codeList});
              },
              function(reason) {
                deferred.reject(reason);
              }
            );

        },
        function(reason) {
          deferred.reject(reason);
        });

      return deferred.promise;
    }

    function _getLatestDictionaryVersion() {
      return _latestDictionaryVersion;
    }

    function _getCachedDictionaryList() {
      return _dictionaryList;
    }

    function _getCachedCodeList() {
      return _codeList;
    }

    function _getViewTypes() {
      return DictionaryAppConstants.VIEWS;
    }

    function _setDictionaryFilterRange(fromVersion, toVersion) {
      var search = $location.search();
      search.vFrom = encodeURIComponent(fromVersion);
      search.vTo = encodeURIComponent(toVersion);

      _dictionaryVersionRange(fromVersion, toVersion);
      $location.search(search);
    }

    function _setView(view) {
        console.log('change view');
        var search = $location.search();

        search.viewMode = view;

        $location.search(search);
    }

    function _getDictionaryUtils() {
      return _dictionaryUtils;
    }

    function _getDictionaryVersionList() {
      var versionList = [];

      if (_dictionaryUtils) {
        versionList = _dictionaryUtils.versionList;
      }

      return versionList;
    }

    function _getCurrentViewType() {
      var search = $location.search();

      return angular.isString(search.viewMode) ? search.viewMode : 'graph';
    }

    function _dictionaryVersionRange(from, to) {

      if (arguments.length === 2) {
        _versionRange.from = from;
        _versionRange.to = to;
      }

      return _versionRange;
    }

    function _generateChangeList() {

      var changeReport = null;

      if (! _dictionaryUtils) {
        return changeReport;
      }

      changeReport = _dictionaryUtils.createDiffListing(_versionRange.from, _versionRange.to);

      changeReport.changed = changeReport.fieldsChanged;

      changeReport.added = changeReport.fieldsAdded;

      changeReport.removed = changeReport.fieldsRemoved;

      return changeReport;
    }

  });