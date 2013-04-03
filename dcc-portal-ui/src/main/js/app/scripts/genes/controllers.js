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
angular.module('app.genes.controllers', ['app.genes.services']);

angular.module('app.genes.controllers').controller('GenesController', [ "$scope", 'GenesService', "genes", function ($scope, GenesService, genes) {
  $scope.genes = genes;

  $scope.refresh = function () {
    GenesService.query().then(function (response) {
      $scope.genes = response;
    });
  };

  $scope.geneList = {
    width: "100%",
    placeholder: "Search for gene symbols",
    tags: [],
    tokenSeparators: [",", " "]
  };

  $scope.$on('refresh', $scope.refresh);
}]);

angular.module('app.genes.controllers').controller('GeneController', [ "$scope", "gene", "GenesProjectsService", function ($scope, gene, GenesProjectsService) {
  $scope.gene = gene;

  GenesProjectsService.get(gene.id).then(function (response) {
    $scope.projects = response;
  });

  $scope.refresh = function () {
    GenesProjectsService.get(gene.id).then(function (response) {
      $scope.projects = response;
    });
  };
}]);

angular.module('app.genes.controllers').controller('EmbGenesController', [ "$scope", 'GenesProjectsService', 'httpService', function ($scope, GenesProjectsService, httpService) {
  GenesProjectsService.queryByProject($scope.project.id).then(function (response) {
    $scope.genes = response;
  });

  $scope.refresh = function () {
    GenesProjectsService.queryByProject($scope.project.id).then(function (response) {
      $scope.genes = response;
    });
  };

  /* Pagination Stuff */
  var gso = httpService.getCurrentSearch().genes ? JSON.parse(httpService.getCurrentSearch().genes) : "{}";
  $scope.gso = {};
  $scope.gso.sort = gso.sort ? gso.sort : '_score';
  $scope.gso.order = gso.order ? gso.order : 'desc';
  $scope.gGetOrder = function () {
    if ($scope.gso.order == 'desc') {
      $scope.gso.order = 'asc';
    } else {
      $scope.gso.order = 'desc';
    }
    return $scope.gso.order;
  };
  $scope.gToggleSort = function (header) {
    var search = httpService.getCurrentSearch();
    $scope.gso.sort = header;
    $scope.gGetOrder();

    search.genes = '{"sort":"' + $scope.gso.sort + '","order":"' + $scope.gso.order + '"}';

    httpService.updateSearch(search);
    $scope.refresh();
  };
  /* /pagination */

  $scope.$on('refresh', $scope.refresh);
}]);
