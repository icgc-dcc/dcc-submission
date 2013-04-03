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

angular.module('app', [
  'ui.directives',
  'ui.bootstrap',
  'highcharts',
  'app.controllers',
  'app.common',
  'app.http',
  'app.facets',
  'app.projects',
  'app.donors',
  'app.genes',
  'app.variants',
  'app.advanced']);

angular.module('app').config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
  $routeProvider
      .when('/', {templateUrl: 'views/home.html', controller: 'ApplicationController'})
      .when('/browser', {templateUrl: 'views/browser.html', controller: 'ApplicationController'})
      .otherwise({redirectTo: '/'});
  //$locationProvider.html5Mode(true);
  //$locationProvider.hashPrefix('!');
}]);

angular.module('app.controllers', []);

angular.module('app.controllers').controller('ApplicationController', [ "$rootScope", "$scope", "ProjectsService", "HighchartsService", function ($rootScope, $scope, ProjectsService, HighchartsService) {
  ProjectsService.query().then(function (projects) {
    $scope.projects = projects;
    $scope.dpspndata = HighchartsService.hits2HCdonut("project", "primary_site", "project_name", projects.hits, "_summary._total_donor_count");
  });


  $rootScope.$on("$routeChangeStart", function (event, next, current) {
    //console.log("$routeChangeStart")
  });
  $rootScope.$on("$routeChangeSuccess", function (event, current, previous) {
    //console.log("$routeChangeSuccess")
  });
  $rootScope.$on("$routeChangeError", function (event, current, previous, rejection) {
    //console.log("$routeChangeError")
  });
}]);

