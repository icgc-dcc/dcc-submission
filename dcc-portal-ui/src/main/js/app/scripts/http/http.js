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

angular.module('app.http', ['app.http.http']);

angular.module('app.http.http', ['app.http.service']);

angular.module('app.http.http').factory('http', ['$http', 'httpService', function ($http, httpService) {
  var extractData = function (response) {
    //return response.data;
    return {
      type: "projects",
      hits: [
        {
          score: 1,
          fields: {
            cnsm_tested_donor_count: 4,
            country: "Germany",
            exp_tested_donor_count: 0,
            meth_tested_donor_count: 0,
            primary_site: "brain",
            project_name: "Pediatric Brain Tumors - DKFZ, DE",
            pubmed_id: [22265402, 22286061],
            ssm_tested_donor_count: 121,
            total_donor_count: 121
          }
        },
        {
          score: 1,
          fields: {
            cnsm_tested_donor_count: 4,
            country: "Spain",
            exp_tested_donor_count: 0,
            meth_tested_donor_count: 56,
            primary_site: "blood",
            project_name: "Lymphocytic Leukemia - ISC/MICINN, ES",
            pubmed_id: [21642962, 22158541],
            ssm_tested_donor_count: 109,
            total_donor_count: 109
          }
        },
        {
          score: 1,
          fields: {
            cnsm_tested_donor_count: 65,
            country: "France",
            exp_tested_donor_count: 0,
            meth_tested_donor_count: 0,
            primary_site: "liver",
            project_name: "Liver Cancer - INCa, FR",
            ssm_tested_donor_count: 24,
            total_donor_count: 75
          }
        },
        {
          score: 1,
          fields: {
            cnsm_tested_donor_count: 66,
            country: "Autralia",
            exp_tested_donor_count: 3,
            meth_tested_donor_count: 0,
            primary_site: "pancreas",
            project_name: "Pancreatic Cancer - QCMG, AU",
            ssm_tested_donor_count: 66,
            total_donor_count: 66
          }
        },
        {
          score: 1,
          fields: {
            cnsm_tested_donor_count: 27,
            country: "Canada",
            exp_tested_donor_count: 0,
            meth_tested_donor_count: 0,
            primary_site: "pancreas",
            project_name: "Pancreatic Cancer Genome Sequencing - OICR, CA",
            ssm_tested_donor_count: 33,
            total_donor_count: 42
          }
        }
      ],
      facets: {
        project_name: {
          terms: {
            name1: {count: Math.random()},
            name2: {count: 30},
            name3: {count: 30},
            name4: {count: 30}
          }
        },
        primary_site: {
          terms: {
            pancreas: {count: 30},
            liver: {count: 30},
            brain: {count: 30},
            blood: {count: 30}
          }
        },
        country: {
          terms: {
            spain: {count: 30},
            germany: {count: 30},
            france: {count: 30},
            canada: {count: 30},
            australia: {count: 30}
          }
        },
        available_data_type: {
          terms: {
            ssm: {count: 30},
            cnsm: {count: 30},
            exp: {count: 30},
            dna_meth: {count: 30}
          }
        }
      },
      pagination: {
        "count": 3,
        "size": 3,
        "from": 99,
        "total": 300,
        "page": 33,
        "pages": 100,
        "sort": "donors",
        "order": "asc",
        "next": "https://data-portal.icgc.org/ws/projects?size=3&page=34",
        "previous": "https://data-portal.icgc.org/ws/projects?size=3&page=32"
      }
    };
  };

  var setActiveFacets = function (response) {
    var currentFilters = httpService.getCurrentFilters();
    // Compare facets with filters and set actives
    angular.forEach(response.facets, function (facet, facet_name) {
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
    return response;
  };

  return {
    get: function (url) {
      return $http.get(url)
          .then(extractData)
          .then(setActiveFacets);
    }
  };
}]);
