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

angular.module('app.facets.controllers').controller('FacetsController', [ "$scope", "$location", function ($scope, $location) {
  console.log('FacetsController');
  // set filters using facet onclick
  // >> should set facet to 'active'
  // >> should update location.search 
  // >> should update model url to use filter
  // >> should refresh model data
  // ^^^^ data is refeshed on every click
  // state should remain on page refresh
  // >> should update model url to use filter
  // >> should set active facets using ?filters

  var getCurrentSearch = function () {
    return $location.search();
  };

  var jsonifyParams = function (string) {
    return ('{"' + string + '"}')
        .replace(/,/g, '","')
        .replace(/:/g, '":')
        .replace(/\[/g, '["')
        .replace(/]"/g, '"]');
  };

  var getCurrentFilters = function () {
    var cfParams;
    cfParams = getCurrentSearch().filters || '';
    return cfParams.length ? JSON.parse(jsonifyParams(cfParams)) : {};
  };


  var applyCurrentFilters = function () {
    var currentFilters = getCurrentFilters();
    // Compare facets with filters and set actives
    angular.forEach($scope.facets, function (facet, facet_name) {
      // Set facets as active
      facet.active = true;
      if (currentFilters.hasOwnProperty(facet_name)) {
        // Compare terms with filters and set actives
        angular.forEach(facet.terms, function (term, term_name) {
          term.active = currentFilters[facet_name].indexOf(term_name) !== -1;
        });
      } else {
        facet.active = false;
        angular.forEach(facet.terms, function (term) {
          term.active = false;
        });
      }
    });
  };
  applyCurrentFilters();

  // TODO is this even needed when setActive() is done?
  var toggleActiveFilter = function (type, facet, term) {
    var t = $scope[type].facets[facet].terms[term];
    t.active = !t.active;
  };

  var toggleUriFilters = function (facet, term) {
    var search, filters;
    search = getCurrentSearch();
    filters = getCurrentFilters();

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

    $location.search(search);
  };

  // TODO toggle filter
  $scope.toggleFilter = function (facet, term) {
    toggleUriFilters(facet, term);
    $scope.$emit('toggleFilter');
    applyCurrentFilters();
  };
}]);
