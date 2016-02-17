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

      GraphicChartsData.getData(function (StudiesData) {
        if (StudiesData) {
          $scope.ItemDataJSon = GraphicChartsUtils.getArrayByAggregation($scope.chartAggregationName, StudiesData[$scope.chartEntityDto]);
          $scope.ItemDataJSon.unshift($scope.chartHeader);
          if ($scope.ItemDataJSon) {
            $scope.chartObject = {};
            $scope.chartObject.type = $scope.chartType;
            $scope.chartObject.data = $scope.ItemDataJSon;
            $scope.chartObject.options = {backgroundColor: {fill: 'transparent'}};
            angular.extend($scope.chartObject.options, $scope.chartOptions);
            $scope.chartObject.options.title = $filter('translate')($scope.chartTitle) + ' (N=' + StudiesData.studyResultDto.totalHits + ')';
          }
        }
      });

    }]);
