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

angular.module('app.facets.directives', []);

angular.module('app.facets.directives').directive('termFacet', ['$location', function ($location) {
  return {
    restrict: 'E',
    scope: {
      facetName: '@',
      type: '@',
      facet: '='
    },
    templateUrl: '/views/facets/term.html',
    link: function (scope, iElement, iAttrs) {
      scope.$watch('facet', function (newValue, oldValue) {
        if (newValue !== oldValue) {
          refresh();
        }
      });

      function setFacetActiveState() {
        var terms = scope.facet.terms;
        for (var i = 0; i < terms.length; ++i) {
          var t = terms[i];
          if (t.active === true) {
            scope.facet.active = true;
            break;
          }
          scope.facet.active = false;
        }
      }

      function toggleTermActiveState(term) {
        var terms = scope.facet.terms;
        for (var i = 0; i < terms.length; ++i) {
          var t = terms[i];
          if (t.term === term) {
            t.active = !t.active;
            break;
          }
        }
      }

      function setActive(facets) {
        // TODO - show activated terms that no longer have results -> current query
        var terms = facets[iAttrs.facetName];
        for (var term in terms) {
          toggleTermActiveState(terms[term]);
        }
        setFacetActiveState();
      }

      scope.termClick = function (term) {
        // This is for immediate feedback
        toggleTermActiveState(term);
        setFacetActiveState();
        //
        scope.$emit('termFilter', scope.type, scope.facetName, term);
      };


      var refresh = function () {
        console.log('refresh?');
        var filters = $location.search().filters || '{}';
        var f = JSON.parse(filters);
        var r;
        r = f.hasOwnProperty(iAttrs.type) ? f[iAttrs.type] : f;
        if (filters) setActive(r);
      };

      refresh();
    }
  };
}]);

angular.module('app.facets.directives').directive('locationFacet', ['$location', function ($location) {
  return {
    restrict: 'E',
    scope: {
      facetName: '@',
      type: '@',
      placeholder: '@'
    },
    templateUrl: '/views/facets/location.html',
    link: function (scope, iElement, iAttrs) {
      var oldLocation;

      // Only show search button when value changed
      scope.$watch('location', function (newLocation) {
        // Empty input and reload
        if (newLocation === null) {
          scope.changed = false;
          scope.locationClick();
        } else {
          var nL = newLocation || '';
          var oL = oldLocation || '';
          scope.changed = nL != oL;
        }
      });

      // search on click
      scope.locationClick = function () {
        scope.$emit('locationFilter', scope.type, scope.facetName, scope.location);
      };

      function setActive(facets) {
        console.log(facets, iAttrs.type, iAttrs.facetName);
        oldLocation = facets[iAttrs.type][iAttrs.facetName];
        scope.location = facets[iAttrs.type][iAttrs.facetName];
      }

      // Preset value on reload
      (function () {
        var filters = $location.search().filters || '';
        if (filters) setActive(JSON.parse(filters));
      })();
    }
  };
}]);

angular.module('app.facets.directives').directive('multiFacet', ['$location', function ($location) {
  return {
    restrict: 'E',
    scope: {
      facetName: '@',
      select2: '='
    },
    templateUrl: '/views/facets/multi.html',
    link: function (scope, iElement, iAttrs) {

      scope.init = function () {
        console.log('ehre');
      };

      function setActive(facets) {
      }

      // Preset value on reload
      (function auto() {
        var filters = $location.search().filters || '';
        if (filters) setActive(JSON.parse(filters));
      })();
    }
  };
}]);

angular.module('app.facets.directives').directive('currentSelection', ['$location', function ($location) {
  return {
    restrict: 'E',
    templateUrl: '/views/facets/current.html',
    link: function (scope, iElement, iAttrs) {
      scope.$watch(function () {
        return $location.search().filters
      }, function () {
        refresh();
      });

      var refresh = function () {
        var filters = $location.search().filters || '{}';
        scope.filters = JSON.parse(filters);
        scope.active = Object.keys(scope.filters).length
      };

      scope.termClick = function (term) {
        // This is for immediate feedback
        toggleTermActiveState(term);
        setFacetActiveState();
        scope.$emit('termFilter', scope.type, scope.facetName, term);
      };

      refresh();
    }
  };
}]);
