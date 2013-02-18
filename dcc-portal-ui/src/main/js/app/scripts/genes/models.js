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

angular.module('app.genes.models', []);

angular.module('app.genes.models').factory('Genes', ['$http', function ($http) {
  return {
    query: function () {
      return {
        type: 'genes',
        hits: [
          {
            "symbol": "MAN2B2",
            "gene_name": "ENSG00000013288",
            "chromosome": 4,
            "start": 6576902,
            "end": 6625089,
            "band": "p16.1",
            "gene_type": "protein_coding",
            "links": {
              "self": {
                "method": "GET",
                "uri": "https://data-portal.icgc.org/ws/genes/ENSG00000013288"
              }
            }
          },
          {
            "symbol": "CLDN11",
            "gene_name": "ENSG00000013297",
            "chromosome": 3,
            "start": 170136653,
            "end": 170578169,
            "band": "q26.2",
            "gene_type": "protein_coding",
            "links": {
              "self": {
                "method": "GET",
                "uri": "https://data-portal.icgc.org/ws/genes/ENSG00000013297"
              }
            }
          },
          {
            "symbol": "ANGEL1",
            "gene_name": "ENSG00000013523",
            "chromosome": 14,
            "start": 77253588,
            "end": 77292589,
            "band": "q24.3",
            "gene_type": "protein_coding",
            "links": {
              "self": {
                "method": "GET",
                "uri": "https://data-portal.icgc.org/ws/genes/ENSG00000013523"
              }
            }
          }
        ],
        facets: {
          "gene_type": {
            terms: {
              "protein_coding": {count: Math.random()},
              "pseudogene": {count: 30},
              "miRNA": {count: 30},
              "non_coding": {count: 30}
            }
          },
          "gene_type2": {
            terms: {
              "protein_coding": {count: 30},
              "pseudogene": {count: 30},
              "miRNA": {count: 30},
              "non_coding": {count: 30}
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
          "sort": "start",
          "order": "asc",
          "next": "https://data-portal.icgc.org/ws/genes?size=3&page=34",
          "previous": "https://data-portal.icgc.org/ws/genes?size=3&page=32"
        }
      };

      //return $http.get('/ws/genes').then(function (response) {
      //  return response.data;
      //});
    },
    get: function (params) {
      return $http.get('/ws/genes/' + params.gene).then(function (response) {
        return response.data.data;
      });
    }
  }
}]);
