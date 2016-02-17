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
  .factory('GraphicChartsDataResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('JoinQuerySearchResource'), {}, {
        'studies': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'studies'}
        }
      });
    }])
  .service('GraphicChartsConfig', function () {
    var factory = {
      options: {
        entityIds: 'NaN',
        entityType: null,
        ChartsOptions: {
          geoChartOptions: {
            header : ['graphics.country', 'graphics.nbr-studies'],
            title : 'graphics.geo-chart-title',
            options: {
              backgroundColor: {fill: 'transparent'},
              colors: [
                '#4db300',
                '#409400',
                '#317000',
                '#235200'
              ],
              width: 500,
              height: 300
            }
          },
          studiesDesigns: {
            header : ['graphics.study-design', 'graphics.nbr-studies'],
            title : 'graphics.study-design-chart-title',
            options: {
              legend: {position: 'none'},
              backgroundColor: {fill: 'transparent'},
              colors: ['#006600',
                '#009900',
                '#009966',
                '#009933',
                '#66CC33'],
              width: 500,
              height: 300
            }
          },
          biologicalSamples: {
            header : ['graphics.bio-samples', 'graphics.nbr-studies'],
            title : 'graphics.bio-samples-chart-title',
            options: {
              backgroundColor: {fill: 'transparent'},
              colors: ['#006600',
                '#009900',
                '#009966',
                '#009933',
                '#66CC33'],
              width: 500,
              height: 300
            }
          }

        }

      }
    };
    factory.setOptions = function (newOptions) {
      if (typeof(newOptions) === 'object') {
        Object.keys(newOptions).forEach(function (option) {
          if (option in factory.options) {
            factory.options[option] = newOptions[option];
          }
        });
      }
    };

    factory.getOptions = function () {
      return angular.copy(factory.options);
    };
    return factory;

  })
  .service('GraphicChartsUtils', [
    function () {
      this.getArrayByAggregation = function (aggregationName, entityDto) {
        var arrayData = [];
        if (!entityDto) {
          return arrayData;
        }

        angular.forEach(entityDto.aggs, function (aggregation) {
          if (aggregation.aggregation === aggregationName) {
            var i = 0;
            angular.forEach(aggregation['obiba.mica.TermsAggregationResultDto.terms'], function (term) {
              if (term.count) {
                arrayData[i] = [term.title, term.count];
                i++;
              }
            });
          }
        });

        return arrayData;
      };
    }])
  .service('GraphicChartsQuery', ['RqlQueryService', 'RqlQueryUtils','LocalizedValues', function (RqlQueryService, RqlQueryUtils,LocalizedValues) {
    this.queryDtoBuilder = function (entityIds, entityType) {
      var query;
      if (!(entityIds) || entityIds === 'NaN') {
        query =  'study(exists(Mica_study.id))';
      }
      if(entityType && entityIds !== 'NaN') {
        query =  entityType + '(in(Mica_'+ entityType +'.id,(' + entityIds + ')))';
      }
      var localizedRqlQuery = angular.copy(new RqlParser().parse(query));
      RqlQueryUtils.addLocaleQuery(localizedRqlQuery, LocalizedValues.getLocal());
      var localizedQuery = new RqlQuery().serializeArgs(localizedRqlQuery.args);
      return RqlQueryService.prepareGraphicsQuery(localizedQuery,
        ['methods.designs', 'populations.selectionCriteria.countriesIso', 'populations.dataCollectionEvents.bioSamples', 'numberOfParticipants.participant.number']);
    };
  }]);
