/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

'use strict';

var mod = angular.module('app.common.loading', []);

mod.factory('loadingService', function () {
  var service = {
    requestCount: 0,
    isLoading: function () {
      return service.requestCount > 0;
    }
  };
  return service;
});

mod.factory('onStartInterceptor', function (loadingService) {
  console.log('onStartInterceptor');
  return function (data, headersGetter) {
    loadingService.requestCount++;
    return data;
  };
});

// This is just a delay service for effect!
mod.factory('delayedPromise', function ($q, $timeout) {
  return function (promise, delay) {
    var deferred = $q.defer();
    var delayedHandler = function () {
      $timeout(function () {
        deferred.resolve(promise);
      }, delay);
    };
    promise.then(delayedHandler, delayedHandler);
    return deferred.promise;
  };
});

mod.factory('onCompleteInterceptor', function (loadingService, delayedPromise) {
  console.log('onStartInterceptor');
  return function (promise) {
    var decrementRequestCount = function (response) {
      loadingService.requestCount--;
      return response;
    };
    // Normally we would just chain on to the promise but ...
    //return promise.then(decrementRequestCount, decrementRequestCount);
    // ... we are delaying the response by 2 secs to allow the loading to be seen.
    return promise.then(decrementRequestCount, decrementRequestCount);
    //return delayedPromise(promise, 1000).then(decrementRequestCount, decrementRequestCount);
  };
});

mod.config(function ($httpProvider) {
  $httpProvider.responseInterceptors.push('onCompleteInterceptor');
});

mod.run(function ($http, onStartInterceptor) {
  $http.defaults.transformRequest.push(onStartInterceptor);
});

mod.controller('LoadingCtrl', function ($scope, loadingService) {
  $scope.$watch(
      function () {
        return loadingService.isLoading();
      },
      function (value) {
        $scope.loading = value;
        console.log("Loading... ", $scope.loading);
      });
});
