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
      facet: '='
    },
    templateUrl: '/views/facets/term.html',
    link: function (scope, iElement, iAttrs) {

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

      function setActive(filters) {
        console.log(JSON.parse(filters));
        var facets = JSON.parse(filters);
        for (var facet in facets) {
          var terms = facets[facet];
          for (var term in terms) {
            console.log(terms[term]);
            toggleTermActiveState(terms[term]);
          }
          setFacetActiveState();
        }
      }

      scope.termClick = function (term) {
        // This is for immediate feedback
        toggleTermActiveState(term);
        setFacetActiveState();
        scope.$emit('termFilter', scope.facetName, term);
      };


      (function () {
        var filters = $location.search().filters || '';
        console.log('here', filters);
        if (filters) setActive(filters);
      })();
    }
  };
}]);
