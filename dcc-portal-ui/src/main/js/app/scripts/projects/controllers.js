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

angular.module('app.projects.controllers').controller('ProjectsController', [ "$scope", 'ProjectsService', "projects", function ($scope, ProjectsService, projects) {
  $scope.projects = projects;

  $scope.refresh = function () {
    ProjectsService.query().then(function (response) {
      $scope.projects = response;
    });
  };

  $scope.$on('refresh', $scope.refresh);
}]);

angular.module('app.projects.controllers').controller('ProjectController', [ "$scope", "project", "donors", "genes", function ($scope, project, donors, genes) {
  $scope.project = project;
  $scope.donors = donors;
  $scope.genes = genes;
}]);
