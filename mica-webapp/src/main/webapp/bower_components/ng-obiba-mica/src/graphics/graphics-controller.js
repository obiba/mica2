/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.graphics')

  .controller('GraphicChartsController', [
    '$rootScope',
    '$scope',
    '$filter',
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    'GraphicChartsData',
    function ($rootScope,
              $scope,
              $filter,
              GraphicChartsConfig,
              GraphicChartsUtils,
              GraphicChartsData) {


      $scope.$watch('chartSelectGraphic', function (newValue) {
        if (newValue) {
          GraphicChartsData.getData(function (StudiesData) {
            if (StudiesData) {
              $scope.ItemDataJSon = GraphicChartsUtils.getArrayByAggregation($scope.chartAggregationName, StudiesData[$scope.chartEntityDto])
                .map(function(t) {
                  return [t.title, t.value];
                });
              if ($scope.ItemDataJSon) {
                if ($scope.chartType === 'Table') {
                  $scope.chartObject = {};
                  $scope.chartObject.header = [$filter('translate')($scope.chartHeader[0]), $filter('translate')($scope.chartHeader[1])];
                  $scope.chartObject.type = $scope.chartType;
                  $scope.chartObject.data = $scope.ItemDataJSon;
                }
                else {
                  $scope.ItemDataJSon.unshift([$filter('translate')($scope.chartHeader[0]), $filter('translate')($scope.chartHeader[1])]);
                  $scope.chartObject = {};
                  $scope.chartObject.type = $scope.chartType;
                  $scope.chartObject.data = $scope.ItemDataJSon;
                  $scope.chartObject.options = {backgroundColor: {fill: 'transparent'}};
                  angular.extend($scope.chartObject.options, $scope.chartOptions);
                  $scope.chartObject.options.title = $filter('translate')($scope.chartTitle) + ' (N=' + StudiesData.studyResultDto.totalHits + ')';
                }
              }
            }
          });
        }
      });

    }]);
