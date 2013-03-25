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

angular.module('app.projects.controllers').controller('ProjectsController', [ "$scope", 'ProjectsService', "GenesService", "projects", "gp", function ($scope, ProjectsService, GenesService, projects, gp) {
  var hits2HCpie = function (hits) {
    var r = [];

    for (var i = 0; i < hits.length; ++i) {
      r.push({
        name: hits[i].fields.project_name,
        y: hits[i].fields.total_donor_count,
        type: "project",
        facet: "project_name"
      })
    }

    return r;
  };

  // TODO rework using fields

  var hits2HCstacked = function (hits) {
    var xaxis = [];
    var series = {};
    var r = [];

    for (var i = 0; i < hits.length; ++i) {
      var hit = hits[i];
      //xaxis.push(hit.fields.symbol);
      series[hit.fields.symbol] = {};
      for (var y = 0; y < hit.fields['project.project_name'].length; ++y) {
        var project = hit.fields['project.project_name'][y];
        var count = hit.fields['project.affected_donor_count'][y];
        if (series.hasOwnProperty(project) == false) {
          series[project] = [];
        }
        series[project].push(count);
      }
    }

    for (var s in series) {
      r.push({name: s, data: series[s]})
    }

    return {
      x: xaxis,
      s: r
    }
  };

  $scope.projects = projects;
  $scope.gp = gp;

  $scope.cdata = hits2HCpie(projects.hits);
  var st = hits2HCstacked(gp.hits);
  $scope.stxdata = st.x;
  $scope.stsdata = st.s;

  $scope.refresh = function () {
    ProjectsService.query().then(function (response) {
      $scope.projects = response;
      $scope.cdata = hits2HCpie(response.hits);
    });
  };

  $scope.$on('refresh', $scope.refresh);
}]);

angular.module('app.projects.controllers').controller('ProjectController', [ "$scope", "project", function ($scope, project) {
  $scope.project = project;
  //$scope.donors = donors;
  //$scope.genes = genes;
}]);
