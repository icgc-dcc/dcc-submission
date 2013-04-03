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

angular.module('app.advanced.controllers', []);

angular.module('app.advanced.controllers').controller('AdvancedController', [ "$scope", 'donors', 'genes', 'gp', 'DonorsService', 'GenesService', 'HighchartsService', 'GenesProjectsService', 'httpService', function ($scope, donors, genes, gp, DonorsService, GenesService, HighchartsService, GenesProjectsService, httpService) {
  $scope.donors = donors;
  $scope.genes = genes;
  $scope.gp = gp;

  $scope.cdata_genes_gt = HighchartsService.termFacets2HCpie("gene", "biotype", genes.facets.biotype.terms);
  // $scope.cdata_donors_p = HighchartsService.termFacets2HCpie("donor", "project_name", donors.facets.project_name.terms);
  // $scope.cdata_donors_ps = HighchartsService.termFacets2HCpie("donor", "primary_site", donors.facets.primary_site.terms);
  $scope.cdata_donors_t = HighchartsService.termFacets2HCpie("donor", "donor_tumour_stage_at_diagnosis", donors.facets.donor_tumour_stage_at_diagnosis.terms);

  $scope.stdata = HighchartsService.hits2HCstacked(gp.hits, 'project.project_name', 'project._summary._ssm_donor_count');

  $scope.refresh = function () {
    GenesService.query().then(function (response) {
      $scope.genes = response;
      $scope.cdata_genes_gt = HighchartsService.termFacets2HCpie("gene", "biotype", response.facets.biotype.terms);
    });
    DonorsService.query().then(function (response) {
      $scope.donors = response;
      //   $scope.cdata_donors_p = HighchartsService.termFacets2HCpie("donor", "project_name", response.facets.project_name.terms);
      //   $scope.cdata_donors_ps = HighchartsService.termFacets2HCpie("donor", "primary_site", response.facets.primary_site.terms);
      $scope.cdata_donors_t = HighchartsService.termFacets2HCpie("donor", "donor_tumour_stage_at_diagnosis", response.facets.donor_tumour_stage_at_diagnosis.terms);
    });
    GenesProjectsService.query().then(function (response) {
      $scope.stdata = HighchartsService.hits2HCstacked(response.hits, 'project.project_name', 'project._summary._ssm_donor_count');
    });
  };


  /* Pagination Stuff */
  var dso = httpService.getCurrentSearch().donors ? JSON.parse(httpService.getCurrentSearch().donors) : "{}";
  $scope.dso = {};
  $scope.dso.sort = dso.sort ? dso.sort : '_score';
  $scope.dso.order = dso.order ? dso.order : 'desc';
  $scope.dGetOrder = function () {
    if ($scope.dso.order == 'desc') {
      $scope.dso.order = 'asc';
    } else {
      $scope.dso.order = 'desc';
    }
    return $scope.dso.order;
  };
  $scope.dToggleSort = function (header) {
    var search = httpService.getCurrentSearch();
    $scope.dso.sort = header;
    $scope.dGetOrder();

    search.donors = '{"sort":"' + $scope.dso.sort + '","order":"' + $scope.dso.order + '"}';

    httpService.updateSearch(search);
    $scope.refresh();
  };
  /* /pagination */

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

    if (search.genes) {
      var g = JSON.parse(search.genes);
      g.sort = $scope.gso.sort;
      g.order = $scope.gso.order;
      search.genes = JSON.stringify(g);
    } else {
      search.genes = '{"sort":"' + $scope.gso.sort + '","order":"' + $scope.gso.order + '"}';
    }
    httpService.updateSearch(search);
    $scope.refresh();
  };

  $scope.gSelectPage = function (page) {
    console.log(genes.pagination.size * page);
    var search = httpService.getCurrentSearch();

    if (search.genes) {
      var g = JSON.parse(search.genes);
      g.from = genes.pagination.size * page;
      search.genes = JSON.stringify(g);
    } else {
      search.genes = '{"from":"' + genes.pagination.size * page + '"}';
    }
    httpService.updateSearch(search);
    $scope.refresh();
  };
  /* /pagination */


  $scope.$on('refresh', $scope.refresh);
}]);


