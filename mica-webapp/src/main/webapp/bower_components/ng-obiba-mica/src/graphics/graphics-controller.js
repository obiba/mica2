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
    '$window',
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    'GraphicChartsData',
    'RqlQueryService',
    'ngObibaMicaUrl',
    'googleChartApiPromise',
    function ($rootScope,
              $scope,
              $filter,
              $window,
              GraphicChartsConfig,
              GraphicChartsUtils,
              GraphicChartsData,
              RqlQueryService,
              ngObibaMicaUrl,
              googleChartApiPromise) {

      function initializeChartData() {
        $scope.chartObject = {};
        GraphicChartsData.getData(function (StudiesData) {
          if (StudiesData) {
            GraphicChartsUtils.getArrayByAggregation($scope.chartAggregationName, StudiesData[$scope.chartEntityDto])
              .then(function(entries){

                var data = entries.map(function(e) {
                  if(e.participantsNbr) {
                    return [e.title, e.value, e.participantsNbr];
                  }
                  else{
                    return [e.title, e.value];
                  }
                });

                $scope.updateCriteria = function(key, vocabulary) {
                  RqlQueryService.createCriteriaItem('study', 'Mica_study', vocabulary, key).then(function (item) {
                    var entity = GraphicChartsConfig.getOptions().entityType;
                    var id = GraphicChartsConfig.getOptions().entityIds;
                    var parts = item.id.split('.');

                    var urlRedirect = ngObibaMicaUrl.getUrl('GraphicsSearchRootUrl') + '?type=studies&query=' +
                      entity + '(in(Mica_' + entity + '.id,' + id + ')),study(in(' + parts[0] + '.' + parts[1] + ',' +
                      parts[2].replace(':', '%253A') + '))';

                    $window.location.href = ngObibaMicaUrl.getUrl('BaseUrl') + urlRedirect;
                  });
                };

                if (data) {
                  if (/^Table-/.exec($scope.chartType) !== null) {
                    $scope.chartObject.ordered = $scope.chartOrdered;
                    $scope.chartObject.notOrdered = $scope.chartNotOrdered;
                    if($scope.chartHeader.length<3){
                      $scope.chartObject.header = [
                        $filter('translate')($scope.chartHeader[0]),
                        $filter('translate')($scope.chartHeader[1])
                      ];
                    }
                    else{
                      $scope.chartObject.header = [
                        $filter('translate')($scope.chartHeader[0]),
                        $filter('translate')($scope.chartHeader[1]),
                   //     $filter('translate')($scope.chartHeader[2])
                      ];
                    }
                    $scope.chartObject.type = $scope.chartType;
                    $scope.chartObject.data = data;
                    $scope.chartObject.vocabulary = $scope.chartAggregationName;
                    $scope.chartObject.entries = entries;
                  }
                  else {
                    if($scope.chartHeader.length<3){
                      data.unshift([$filter('translate')($scope.chartHeader[0]), $filter('translate')($scope.chartHeader[1])]);
                    }
                    else{
                      data.unshift([
                        $filter('translate')($scope.chartHeader[0]),
                        $filter('translate')($scope.chartHeader[1]),
                   //     $filter('translate')($scope.chartHeader[2])
                      ]);
                    }
                    $scope.chartObject.term = true;
                    $scope.chartObject.type = $scope.chartType;
                    $scope.chartObject.data = data;
                    $scope.chartObject.options = {backgroundColor: {fill: 'transparent'}};
                    angular.extend($scope.chartObject.options, $scope.chartOptions);
                    $scope.chartObject.options.title = $filter('translate')($scope.chartTitleGraph) + ' (N=' + StudiesData.studyResultDto.totalHits + ')';
                    $scope.$parent.directive = {title: $scope.chartObject.options.title};
                  }
                }
              });
          }
        });

      }

      googleChartApiPromise.then(function() {
        $scope.ready = true;
      });

      $scope.$watchGroup(['chartType', 'ready'], function() {
        if ($scope.chartType && $scope.ready) {
          initializeChartData();
        }
      });

    }]);
