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
  // set filters using facet onclick
  // >> should set facet to 'active'
  // >> should update location.search 
  // >> should update model url to use filter
  // >> should refresh model data
  // ^^^^ data is refeshed on every click
  // state should remain on page refresh
  // >> should update model url to use filter
  // >> should set active facets using ?filters

  var getCurrentSearch = function() {
    return $location.search();
  };

  var jsonifyParams = function(string) {
    return JSON.parse(
        ('{"' + string + '"}')
        .replace(/,/g, '","')
        .replace(/:/g, '":')
        .replace(/\[/g, '["')
        .replace(/]"/g, '"]'));
  };
  
  var getCurrentFilters = function() {
    var cfParams;
    cfParams = getCurrentSearch().filters || '';
    return cfParams.length ? jsonifyParams(cfParams) : {};
  };

  // TODO is this even needed when setActive() is done?
  var toggleActiveFilter = function (type, facet, term) {
    var t = $scope[type].facets[facet].terms[term];
    t.active = !t.active;
  };

  var toggleUriSearchQuery = function (facet, term) {
    var search, filters;
    search = getCurrentSearch();
    filters = getCurrentFilters();
    
    if (filters[facet] === undefined) {
      filters[facet] = [];
    }
    if (filters[facet].indexOf(term) === -1) {
      filters[facet].push(term);
    } else {
      filters[facet].splice(filters[facet].indexOf(term), 1);
    }
    if (filters[facet].length === 0) {
      delete filters[facet];
    }
    if (Object.keys(filters).length) {
      search.filters = JSON.stringify(filters).replace(/[\{\}\"]/g, '');  
    }else {
      delete search.filters;
    }
    
    $location.search(search);
  };

  // TODO toggle filter
  $scope.toggleFilter = function (type, facet, term) {
    console.log(type, facet, term);
    //toggleActiveFilter(type, facet, term);
    toggleUriSearchQuery(facet, term);
  };
}]);
