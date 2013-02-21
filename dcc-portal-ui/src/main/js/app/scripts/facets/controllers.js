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

angular.module('app.facets', ['app.facets.controllers']);

angular.module('app.facets.controllers', ['app.facets.services']);

angular.module('app.facets.controllers').controller('FacetsController', [ "$scope", "httpService", function ($scope, httpService) {
  var toggleUriFilters = function (facet, term) {
    var search, filters;
    search = httpService.getCurrentSearch();
    filters = httpService.getCurrentFilters();

    // If this is a new facet filter
    if (!filters.hasOwnProperty(facet)) {
      filters[facet] = [];
    }
    // If this is a new term filter:
    if (filters[facet].indexOf(term) === -1) {
      filters[facet].push(term);
    } else {
      filters[facet].splice(filters[facet].indexOf(term), 1);
    }
    // If there are no more terms in that facet filter remove the facet
    if (filters[facet].length === 0) {
      delete filters[facet];
    }
    // If there are filters add them to the search query params
    if (Object.keys(filters).length) {
      search.filters = JSON.stringify(filters).replace(/[\{\}\"]/g, '');
    } else {
      delete search.filters;
    }

    httpService.updateSearch(search)
  };

  // TODO toggle filter
  $scope.toggleFilter = function (facet, term) {
    toggleUriFilters(facet, term);
    $scope.$emit('toggleFilter');
  };
}]);
