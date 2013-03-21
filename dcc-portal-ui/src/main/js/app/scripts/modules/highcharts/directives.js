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

angular.module('highcharts', ['highcharts.directives']);

angular.module('highcharts.directives', []);

angular.module('highcharts.directives').directive('chart', function () {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '='
    },
    template: '<div id="container" style="margin: 0 auto">not working</div>',
    link: function (scope, element, attrs) {
      var renderChart = function (settings) {
        new Highcharts.Chart(settings);
      };

      var chartsDefaults = {
        chart: {
          renderTo: 'container',
          type: attrs.type || null,
          height: attrs.height || null,
          width: attrs.width || null,
          plotBackgroundColor: null,
          plotBorderWidth: null,
          plotShadow: false
        },
        title: {
          text: 'Donor Distribution'
        },
        tooltip: {
          pointFormat: '{series.name}: <b>{point.percentage}</b>',
          percentageDecimals: 0
        },
        plotOptions: {
          pie: {
            allowPointSelect: true,
            animation: true,
            cursor: 'pointer',
            dataLabels: {
              enabled: false,
              color: '#000000',
              connectorColor: '#000000',
              formatter: function () {
                return '<b>' + this.point.name + '</b>';
              }
            }
          }
        },
        series: [
          {
            type: 'pie',
            name: 'Donors',
            data: scope.items
          }
        ]
      };

      scope.$watch("items", function (newValue, oldValue) {
        if (!newValue) return;
        if (angular.equals(newValue, oldValue)) return;

        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        var deepCopy = true;
        var newSettings = {};
        $.extend(deepCopy, newSettings, chartsDefaults);
        newSettings.series = [
          {
            type: 'pie',
            name: 'Donors',
            data: newValue
          }
        ];
        renderChart(newSettings);
      });

      renderChart(chartsDefaults);
    }
  }
});
