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
    link: function ($scope, $element, $attrs) {
      var renderChart = function (settings) {
        new Highcharts.Chart(settings);
      };

      var chartsDefaults = {
        chart: {
          renderTo: $element[0],
          type: $attrs.type || null,
          height: $attrs.height || null,
          width: $attrs.width || null,
          plotBackgroundColor: null,
          plotBorderWidth: null,
          plotShadow: false
        },
        title: {
          text: $attrs.title
        },
        plotOptions: {
          pie: {
            innerSize: 100,
            allowPointSelect: true,
            animation: true,
            cursor: 'pointer',
            showInLegend: false,
            dataLabels: {
              enabled: false,
              color: '#000000',
              connectorColor: '#000000',
              distance: 1,
              zIndex: 2,
              //overflow: "justify",
              formatter: function () {
                return this.point.y + '<b>(' + this.point.percentage.toFixed(2) + '% )</b>';
              }
            }
          }
        },
        tooltip: {
          formatter: function () {
            return '<div style="border: 1px solid ' + this.point.color + '" class="hc-tooltip">' +
                this.point.name + "<br>" + "Donors: <strong>" + this.point.y + "</strong>" +
                '</div>';
          },
          useHTML: true,
          shared: false,
          borderRadius: 0,
          borderWidth: 0,
          shadow: false,
          enabled: true,
          backgroundColor: 'none',
          zIndex: 100000
        },
        series: [
          {
            type: 'pie',
            name: $attrs.label,
            data: $scope.items
          }
        ]
      };

      $scope.$watch("items", function (newValue, oldValue) {
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
            name: $attrs.label,
            data: newValue
          }
        ];
        renderChart(newSettings);
      });

      renderChart(chartsDefaults);
    }
  }
});

angular.module('highcharts.directives').directive('stacked', function () {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      xaxis: '=',
      series: '='
    },
    template: '<div id="container" style="margin: 0 auto">not working</div>',
    link: function ($scope, $element, $attrs) {
      console.log($scope, $element, $attrs);
      var renderChart = function (settings) {
        new Highcharts.Chart(settings);
      };

      var chartsDefaults = {
        chart: {
          renderTo: $element[0],
          type: 'column',
          height: $attrs.height || null,
          width: $attrs.width || null,
          plotBackgroundColor: null,
          plotBorderWidth: null,
          plotShadow: false
        },
        title: {
          text: 'Stacked column chart'
        },
        xAxis: {
          categories: $scope.xaxis //['Apples', 'Oranges', 'Pears', 'Grapes', 'Bananas']
        },
        yAxis: {
          min: 0,
          title: {
            text: 'Total fruit consumption'
          },
          stackLabels: {
            enabled: true,
            style: {
              fontWeight: 'bold',
              color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray'
            }
          }
        },
        legend: {
          enabled: false,
          align: 'right',
          x: 0,
          verticalAlign: 'top',
          y: 20,
          floating: true,
          backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColorSolid) || 'white',
          borderColor: '#CCC',
          borderWidth: 1,
          shadow: false
        },
        tooltip: {
          formatter: function () {
            return '<b>' + this.x + '</b><br/>' +
                this.series.name + ': ' + this.y + '<br/>' +
                'Total: ' + this.point.stackTotal;
          }
        },
        plotOptions: {
          column: {
            stacking: 'normal',
            dataLabels: {
              enabled: true,
              color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white'
            }
          }
        },
        series: $scope.series
      };

      $scope.$watch("items", function (newValue, oldValue) {
        if (!newValue) return;
        if (angular.equals(newValue, oldValue)) return;

        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        var deepCopy = true;
        var newSettings = {};
        $.extend(deepCopy, newSettings, chartsDefaults);
        newSettings.series = newValue;
        renderChart(newSettings);
      });

      renderChart(chartsDefaults);
    }
  }
});
