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

angular.module('app.projects.controllers', ['app.projects.services']);

angular.module('app.projects.controllers').controller('ProjectsController', [ "$scope", 'ProjectsService', "GenesProjectsService", "projects", "gp", "HighchartsService", 'httpService', function ($scope, ProjectsService, GenesProjectsService, projects, gp, HighchartsService, httpService) {
  $scope.projects = projects;
  $scope.gp = gp;

  $scope.dpspndata = HighchartsService.hits2HCdonut("project", "primary_site", "project_name", projects.hits, "_summary._total_donor_count");
  $scope.dcpndata = HighchartsService.hits2HCdonut("project", "countries", "project_name", projects.hits, "_summary._total_donor_count");

  $scope.stdata = HighchartsService.hits2HCstacked(gp.hits, 'project.project_name', 'project._summary._ssm_donor_count');

  $scope.refresh = function () {
    ProjectsService.query().then(function (projects) {
      $scope.projects = projects;
      $scope.dpspndata = HighchartsService.hits2HCdonut("project", "primary_site", "project_name", projects.hits, "_summary._total_donor_count");
      $scope.dcpndata = HighchartsService.hits2HCdonut("project", "countries", "project_name", projects.hits, "_summary._total_donor_count");
    });
    GenesProjectsService.query().then(function (response) {
      $scope.stdata = HighchartsService.hits2HCstacked(response.hits, 'project.project_name', 'project._summary._ssm_donor_count');
    });
  };


  /* Pagination Stuff */
  var pso = httpService.getCurrentSearch().projects ? JSON.parse(httpService.getCurrentSearch().projects) : "{}";
  $scope.pso = {};
  $scope.pso.sort = pso.sort ? pso.sort : '_summary._total_donor_count';
  $scope.pso.order = pso.order ? pso.order : 'desc';
  $scope.pGetOrder = function () {
    if ($scope.pso.order == 'desc') {
      $scope.pso.order = 'asc';
    } else {
      $scope.pso.order = 'desc';
    }
    return $scope.pso.order;
  };
  $scope.pToggleSort = function (header) {
    var search = httpService.getCurrentSearch();
    $scope.pso.sort = header;
    $scope.pGetOrder();

    search.projects = '{"sort":"' + $scope.pso.sort + '","order":"' + $scope.pso.order + '"}';

    httpService.updateSearch(search);
    $scope.refresh();
  };
  /* /pagination */

  $scope.$on('refresh', $scope.refresh);
}]);

angular.module('app.projects.controllers').controller('ProjectController', [ "$scope", "project", function ($scope, project) {
  $scope.project = project;
  //$scope.donors = donors;
  //$scope.genes = genes;
}]);
