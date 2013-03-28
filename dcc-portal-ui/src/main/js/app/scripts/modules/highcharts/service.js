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

angular.module('highcharts.services', []);

angular.module('highcharts.services').service('HighchartsService', [function () {
  this.termFacets2HCpie = function (type, facet, terms) {
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

  this.hits2HCpie = function (type, facet, hits, countBy) {
    var r = [];

    for (var i = 0; i < hits.length; ++i) {
      r.push({
        name: hits[i].fields[facet],
        y: hits[i].fields[countBy],
        type: type,
        facet: facet
      })
    }

    return r;
  };


  this.hits2HCdonut = function (type, facetInner, facetOuter, hits, countBy) {
    var colors = Highcharts.getOptions().colors;
    console.log(hits);
    var inr = [];
    var outr = [];

    var innerHits = {};
    var outerHits = {};

    for (var i = 0; i < hits.length; ++i) {
      var hit = hits[i];
      var name = hit.fields[facetInner];
      var count = hit.fields[countBy];
      if (innerHits.hasOwnProperty(name)) {
        innerHits[name] += count;
      } else {
        innerHits[name] = count;
      }
    }

    console.log('ih: ', innerHits);
    var i = 0;
    for (var iName in innerHits) {
      inr.push({
        name: iName,
        y: innerHits[iName],
        type: type,
        facet: facetInner,
        color: colors[i]
      });

      for (var j = 0; j < hits.length; ++j) {
        var hitj = hits[j];
        console.log(hitj);
        console.log(hitj.fields[facetOuter]);
        var brightness = 0.3 - (j / hitj.fields[countBy]) / 5;

        // TODO
        var in_array = (angular.isArray(hitj.fields[facetInner]) && hitj.fields[facetInner].indexOf(iName) !== -1);
        console.log(in_array);
        var in_value = (!angular.isArray(hitj.fields[facetInner]) && hitj.fields[facetInner] === iName);
        console.log(in_value);
        if (in_array || in_value) {
          outr.push({
            name: hitj.fields[facetOuter],
            y: hitj.fields[countBy],
            type: type,
            facet: facetOuter,
            color: Highcharts.Color(colors[i]).brighten(brightness).get()
          });
        }
      }
      i++;
    }

    return {
      inner: inr,
      outer: outr
    };
  };

  this.hits2HCstacked = function (hits, x, y) {
    var xaxis = [];
    var series = {};
    var r = [];

    for (var i = 0; i < hits.length; ++i) {
      var hit = hits[i];
      xaxis.push(hit.fields.symbol);
      //series[hit.fields.symbol] = {};
      for (var j = 0; j < hit.fields[x].length; ++j) {
        var label = hit.fields[x][j];
        var count = hit.fields[y][j];
        if (series.hasOwnProperty(label) == false) {
          series[label] = [];
        }
        series[label].push({x: i, y: count});
      }
    }

    for (var s in series) {
      r.push({name: s, data: series[s]})
    }

    return {
      x: xaxis,
      s: r
    }
  };

}]);
