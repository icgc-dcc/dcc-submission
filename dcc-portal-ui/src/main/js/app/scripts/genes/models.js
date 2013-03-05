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

angular.module('app.genes.models').factory('Genes', ['http', function ($http) {
  return {
    query: function () {
      return $http.get('/ws/genes').then(function () {
        return {
          "hits": [
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000187939",
              "_score": null,
              "fields": {
                "band": "p13.3",
                "symbol": "DOC2B",
                "start": Math.random(),
                "description": "double C2-like domains, beta",
                "chromosome": 17,
                "gene_type": "protein_coding",
                "end": 31427
              },
              "sort": [
                6007
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000236875",
              "_score": null,
              "fields": {
                "band": "p24.3",
                "symbol": "XXyac-YRM2039.1.1",
                "start": 11987,
                "description": "",
                "chromosome": 9,
                "gene_type": "pseudogene",
                "end": 14522
              },
              "sort": [
                11987
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000251180",
              "_score": null,
              "fields": {
                "band": "",
                "symbol": "CU459201.1",
                "start": 12836,
                "description": "Novel protein (KIAA1671)",
                "chromosome": "GL000242.1",
                "gene_type": "protein_coding",
                "end": 34543
              },
              "sort": [
                12836
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000253620",
              "_score": null,
              "fields": {
                "band": "p23.3",
                "symbol": "AC144568.4.1",
                "start": 14091,
                "description": "",
                "chromosome": 8,
                "gene_type": "pseudogene",
                "end": 14320
              },
              "sort": [
                14091
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000227232",
              "_score": null,
              "fields": {
                "band": "p36.33",
                "symbol": "WASH7P",
                "start": 14363,
                "description": "WAS protein family homolog 7 pseudogene",
                "chromosome": 1,
                "gene_type": "pseudogene",
                "end": 29806
              },
              "sort": [
                14363
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000181404",
              "_score": null,
              "fields": {
                "band": "p24.3",
                "symbol": "XXyac-YRM2039.2.1",
                "start": 14511,
                "description": "",
                "chromosome": 9,
                "gene_type": "pseudogene",
                "end": 29739
              },
              "sort": [
                14511
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000244758",
              "_score": null,
              "fields": {
                "band": "p22.3",
                "symbol": "AC093627.6.1",
                "start": 19757,
                "description": "",
                "chromosome": 7,
                "gene_type": "lincRNA",
                "end": 35479
              },
              "sort": [
                19757
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000253896",
              "_score": null,
              "fields": {
                "band": "p23.3",
                "symbol": "AC144568.2.1",
                "start": 22601,
                "description": "Uncharacterized protein",
                "chromosome": 8,
                "gene_type": "protein_coding",
                "end": 29428
              },
              "sort": [
                22601
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000227518",
              "_score": null,
              "fields": {
                "band": "p24.3",
                "symbol": "MIR1302-10",
                "start": 27657,
                "description": "microRNA 1302-10",
                "chromosome": 9,
                "gene_type": "antisense",
                "end": 30891
              },
              "sort": [
                27657
              ]
            },
            {
              "_index": "icgc_test54",
              "_type": "genes",
              "_id": "ENSG00000243485",
              "_score": null,
              "fields": {
                "band": "p36.33",
                "symbol": "MIR1302-10",
                "start": 29554,
                "description": "microRNA 1302-10",
                "chromosome": 1,
                "gene_type": "antisense",
                "end": 31109
              },
              "sort": [
                29554
              ]
            }
          ],
          "facets": {
            "gene_type": {
              "_type": "terms",
              "missing": 0,
              "total": 46324,
              "other": 2317,
              "terms": [
                {
                  "term": "protein_coding",
                  "count": Math.random()
                },
                {
                  "term": "pseudogene",
                  "count": 10538
                },
                {
                  "term": "lincRNA",
                  "count": 3647
                },
                {
                  "term": "antisense",
                  "count": 2883
                },
                {
                  "term": "snRNA",
                  "count": 1804
                },
                {
                  "term": "miRNA",
                  "count": 1645
                },
                {
                  "term": "processed_transcript",
                  "count": 1635
                },
                {
                  "term": "snoRNA",
                  "count": 1420
                },
                {
                  "term": "misc_RNA",
                  "count": 1109
                },
                {
                  "term": "scRNA_pseudogene",
                  "count": 737
                }
              ]
            }
          },
          "pagination": {
            "count": 10,
            "total": 46324,
            "size": 10,
            "from": 1,
            "page": 1,
            "pages": 4632,
            "sort": "start",
            "order": "asc"
          }
        };

      });
    },
    get: function (params) {
      return $http.get('/ws/genes/' + params.gene).then(function (response) {
        return response.data.data;
      });
    }
  }
}]);
