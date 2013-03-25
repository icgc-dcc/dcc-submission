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

angular.module('app.advanced.controllers').controller('AdvancedController', [ "$scope", 'donors', 'genes', 'DonorsService', 'GenesService', function ($scope, donors, genes, DonorsService, GenesService) {
  var termFacets2HCpie = function (type, facet, terms) {
    var r = [];
    for (var i = 0; i < terms.length; ++i) {
      r.push({
        name: terms[i].term,
        y: terms[i].count,
        type: type,
        facet: facet
      })
    }

    return r;
  };

  $scope.donors = donors;
  $scope.genes = genes;

  $scope.cdata_genes_gt = termFacets2HCpie("gene", "gene_type", genes.facets.gene_type.terms);
  $scope.cdata_donors_p = termFacets2HCpie("donor", "project_name", donors.facets.project_name.terms);
  $scope.cdata_donors_ps = termFacets2HCpie("donor", "primary_site", donors.facets.primary_site.terms);
  $scope.cdata_donors_t = termFacets2HCpie("donor", "donor_tumour_stage_at_diagnosis", donors.facets.donor_tumour_stage_at_diagnosis.terms);

  $scope.refresh = function () {
    GenesService.query().then(function (response) {
      $scope.genes = response;
      $scope.cdata_genes_gt = termFacets2HCpie("gene", "gene_type", response.facets.gene_type.terms);
    });
    DonorsService.query().then(function (response) {
      $scope.donors = response;
      $scope.cdata_donors_p = termFacets2HCpie("donor", "project_name", response.facets.project_name.terms);
      $scope.cdata_donors_ps = termFacets2HCpie("donor", "primary_site", response.facets.primary_site.terms);
      $scope.cdata_donors_t = termFacets2HCpie("donor", "donor_tumour_stage_at_diagnosis", response.facets.donor_tumour_stage_at_diagnosis.terms);

    });
  };

  $scope.$on('refresh', $scope.refresh);
}]);


