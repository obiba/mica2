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
        entityType: null
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
    'LocalizedStringService',
    function (CountriesIsoUtils,
              LocalizedStringService) {

      this.getArrayByAggregation = function (AggregationName, EntityDto, fieldTransformer) {
        var ArrayData = [];
        angular.forEach(EntityDto.aggs, function (aggragation) {
          var itemName = [];
          if (aggragation.aggregation === AggregationName) {
            var i = 0;
            angular.forEach(aggragation['obiba.mica.TermsAggregationResultDto.terms'], function (term) {
              switch (fieldTransformer) {
                case 'country' :
                  itemName.name = CountriesIsoUtils.findByCode(term.title.toUpperCase(), LocalizedStringService.getLocal());
                  break;
                default :
                  itemName.name = term.title;
                  break;
              }
              if (term.count) {
                ArrayData[i] = [itemName.name, term.count];
                i ++;
              }
            });
          }
        });
        return ArrayData;
      };
    }])
  .service('GraphicChartsQuery', [function () {
    this.queryDtoBuilder = function (entityIds) {
      if (!(entityIds) || entityIds ==='NaN') {
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"locale":"en","withFacets":true}';
      }
      else{
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"networkQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0},"filteredQuery":{"obiba.mica.LogicalFilterQueryDto.filter":{"fields":[{"field":{"field":"id","obiba.mica.TermsFilterQueryDto.terms":{"values":["'+entityIds+'"]}},"op":1}]}}},"locale":"en","withFacets":true}';
      }
    };
  }]);
