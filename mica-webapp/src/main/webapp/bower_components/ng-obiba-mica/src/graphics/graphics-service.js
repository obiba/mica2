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
      return $resource(ngObibaMicaUrl.getUrl('PublishedStudiesSearchResource'), {}, {
        'get': {method: 'POST', errorHandler: true}
      });
    }])
  .service('GraphicChartsConfig', function () {
    var factory = {
      options: {
        entityIds: 'NaN',
        entityType: null,
        ChartsOptions: {
          geoChartOptions: {
            options: {
              backgroundColor: {fill: 'transparent'},
              title: 'Distribution of studies by participants countries of residence',
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
            options: {
              backgroundColor: {fill: 'transparent'},
              title: 'Distribution of studies by study design',
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
            options: {
              backgroundColor: {fill: 'transparent'},
              title: 'Distribution of studies by Biological Samples',
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
    'CountriesIsoUtils',
    function (CountriesIsoUtils) {
      this.getArrayByAggregation = function (aggregationName, entityDto, fieldTransformer, lang) {
        var arrayData = [];
        if (!entityDto) {
          return arrayData;
        }

        angular.forEach(entityDto.aggs, function (aggregation) {
          var itemName = [];
          if (aggregation.aggregation === aggregationName) {
            var i = 0;
            angular.forEach(aggregation['obiba.mica.TermsAggregationResultDto.terms'], function (term) {
              switch (fieldTransformer) {
                // TODO countries are already translated on server side (term title)
                case 'country':
                  itemName.name = CountriesIsoUtils.findByCode(term.key.toUpperCase(), lang);
                  break;
                default :
                  itemName.name = term.title;
                  break;
              }
              if (term.count) {
                arrayData[i] = [itemName.name, term.count];
                i++;
              }
            });
          }
        });

        return arrayData;
      };
    }])
  .service('GraphicChartsQuery', [function () {
    this.queryDtoBuilder = function (entityIds) {
      if (!(entityIds) || entityIds === 'NaN') {
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"locale":"en","withFacets":true}';
      }
      else {
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"networkQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0},"filteredQuery":{"obiba.mica.LogicalFilterQueryDto.filter":{"fields":[{"field":{"field":"id","obiba.mica.TermsFilterQueryDto.terms":{"values":["' + entityIds + '"]}},"op":1}]}}},"locale":"en","withFacets":true}';
      }
    };
  }]);
