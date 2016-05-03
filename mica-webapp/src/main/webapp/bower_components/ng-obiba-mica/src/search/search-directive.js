/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/* exported CRITERIA_ITEM_EVENT */
var CRITERIA_ITEM_EVENT = {
  deleted: 'event:delete-criteria-item',
  refresh: 'event:refresh-criteria-item'
};

angular.module('obiba.mica.search')

  .directive('taxonomyPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        taxonomy: '=',
        lang: '=',
        onNavigate: '='
      },
      templateUrl: 'search/views/classifications/taxonomy-panel-template.html'
    };
  }])

  .directive('vocabularyPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        taxonomy: '=',
        vocabulary: '=',
        lang: '=',
        onNavigate: '=',
        onSelect: '=',
        onHideSearchNavigate: '=',
        isInHideNavigate: '='
      },
      templateUrl: 'search/views/classifications/vocabulary-panel-template.html'
    };
  }])

  .directive('termPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        taxonomy: '=',
        vocabulary: '=',
        term: '=',
        lang: '=',
        onSelect: '='
      },
      templateUrl: 'search/views/classifications/term-panel-template.html'
    };
  }])

  .directive('networksResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'RqlQueryService',
    function (PageUrlService, ngObibaMicaSearch, RqlQueryService) {
      return {
        restrict: 'EA',
        replace: true,
        scope: {
          summaries: '=',
          loading: '=',
          onUpdateCriteria: '='
        },
        templateUrl: 'search/views/list/networks-search-result-table-template.html',
        link: function(scope) {
          scope.options = ngObibaMicaSearch.getOptions().networks;
          scope.optionsCols = scope.options.networksColumn;
          scope.PageUrlService = PageUrlService;

          scope.updateCriteria = function (id, type) {
            var datasetClassName;
            if (type === 'HarmonizationDataset' || type === 'StudyDataset') {
              datasetClassName = type;
              type = 'datasets';
            }

            var variableType;
            if (type === 'DataschemaVariable' || type === 'StudyVariable') {
              variableType = type.replace('Variable', '');
              type = 'variables';
            }

            RqlQueryService.createCriteriaItem('network', 'Mica_network', 'id', id).then(function (item) {
              if(datasetClassName) {
                RqlQueryService.createCriteriaItem('dataset', 'Mica_dataset', 'className', datasetClassName).then(function(datasetItem) {
                  scope.onUpdateCriteria(item, type);
                  scope.onUpdateCriteria(datasetItem, type);
                });
              } else if (variableType) {
                RqlQueryService.createCriteriaItem('variable', 'Mica_variable', 'variableType', variableType).then(function (variableItem) {
                  scope.onUpdateCriteria(item, type);
                  scope.onUpdateCriteria(variableItem, type);
                });
              } else {
                scope.onUpdateCriteria(item, type);
              }
            });
          };
        }
      };
  }])

  .directive('datasetsResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'TaxonomyResource', 'RqlQueryService', function (PageUrlService, ngObibaMicaSearch, TaxonomyResource, RqlQueryService) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '=',
        onUpdateCriteria: '='
      },
      templateUrl: 'search/views/list/datasets-search-result-table-template.html',
      link: function(scope) {
        scope.classNames = {};
        TaxonomyResource.get({
          target: 'dataset',
          taxonomy: 'Mica_dataset'
        }).$promise.then(function (taxonomy) {
            scope.classNames = taxonomy.vocabularies.filter(function (v) {
              return v.name === 'className';
            })[0].terms.reduce(function (prev, t) {
                prev[t.name] = t.title.map(function (t) {
                  return {lang: t.locale, value: t.text};
                });
                return prev;
              }, {});
          });

        scope.updateCriteria = function (id, type) {
          RqlQueryService.createCriteriaItem('dataset', 'Mica_dataset', 'id', id).then(function(item) {
            scope.onUpdateCriteria(item, type);
          });
        };

        scope.options = ngObibaMicaSearch.getOptions().datasets;
        scope.optionsCols = scope.options.datasetsColumn;
        scope.PageUrlService = PageUrlService;
      }
    };
  }])

  .directive('studiesResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'TaxonomyResource', 'RqlQueryService', 'LocalizedValues',
    function (PageUrlService, ngObibaMicaSearch, TaxonomyResource, RqlQueryService, LocalizedValues) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        lang: '=',
        summaries: '=',
        loading: '=',
        onUpdateCriteria: '='
      },
      templateUrl: 'search/views/list/studies-search-result-table-template.html',
      link: function(scope) {
        scope.taxonomy = {};
        scope.designs = {};
        scope.datasourceTitles = {};

        function getDatasourceTitles() {
          if (Object.keys(scope.taxonomy) < 1 || Object.keys(scope.datasourceTitles) > 0) {
            return;
          }

          scope.taxonomy.vocabularies.some(function(vocabulary) {
            if (vocabulary.name === 'populations-dataCollectionEvents-dataSources') {
              vocabulary.terms.forEach(function(term) {
                scope.datasourceTitles[term.name] = {title: LocalizedValues.forLocale(term.title, scope.lang)};
              });
              return true;
            }
            return false;
          });
        }

        scope.$watch('lang', getDatasourceTitles);

        TaxonomyResource.get({
          target: 'study',
          taxonomy: 'Mica_study'
        }).$promise.then(function (taxonomy) {
          scope.taxonomy = taxonomy;
          getDatasourceTitles();
          scope.designs = taxonomy.vocabularies.filter(function (v) {
            return v.name === 'methods-designs';
          })[0].terms.reduce(function (prev, t) {
              prev[t.name] = t.title.map(function (t) {
                return {lang: t.locale, value: t.text};
              });
              return prev;
            }, {});
        });

        scope.hasDatasource = function (datasources, id) {
          return datasources && datasources.indexOf(id) > -1;
        };

        scope.options = ngObibaMicaSearch.getOptions().studies;
        scope.optionsCols = scope.options.studiesColumn;
        scope.PageUrlService = PageUrlService;

        scope.updateCriteria = function (id, type) {
          var datasetClassName;
          if (type === 'HarmonizationDataset' || type === 'StudyDataset') {
            datasetClassName = type;
            type = 'datasets';
          }

          var variableType;
          if (type === 'DataschemaVariable' || type === 'StudyVariable') {
            variableType = type.replace('Variable', '');
            type = 'variables';
          }

          RqlQueryService.createCriteriaItem('study', 'Mica_study', 'id', id).then(function(item) {
            if(datasetClassName) {
              RqlQueryService.createCriteriaItem('dataset', 'Mica_dataset', 'className', datasetClassName).then(function (datasetItem) {
                scope.onUpdateCriteria(item, type);
                scope.onUpdateCriteria(datasetItem, type);
              });
            } else if (variableType) {
              RqlQueryService.createCriteriaItem('variable', 'Mica_variable', 'variableType', variableType).then(function (variableItem) {
                scope.onUpdateCriteria(item, type);
                scope.onUpdateCriteria(variableItem, type);
              });
            } else {
              scope.onUpdateCriteria(item, type);
            }
          });
        };
      }
    };
  }])

  .directive('variablesResultTable', ['PageUrlService', 'ngObibaMicaSearch', function (PageUrlService, ngObibaMicaSearch) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/variables-search-result-table-template.html',
      link: function(scope) {
        scope.options = ngObibaMicaSearch.getOptions().variables;
        scope.optionsCols = scope.options.variablesColumn;
        scope.PageUrlService = PageUrlService;
      }
    };
  }])

  .directive('coverageResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        result: '=',
        loading: '=',
        bucket: '=',
        query: '=',
        onUpdateCriteria: '='
      },
      controller: 'CoverageResultTableController',
      templateUrl: 'search/views/coverage/coverage-search-result-table-template.html'
    };
  }])

  .directive('graphicsResult', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        result: '=',
        loading: '=',
        onUpdateCriteria: '='
      },
      controller: 'GraphicsResultController',
      templateUrl: 'search/views/graphics/graphics-search-result-template.html'
    };
  }])

  .directive('includeReplace', function () {
    return {
      require: 'ngInclude',
      link: function (scope, el) {
        el.replaceWith(el.children());
      }
    };
  })

  .directive('scrollToTop', function(){
    return {
      restrict: 'A',
      scope: {
        trigger: '=scrollToTop'
      },
      link: function postLink(scope, elem) {
        scope.$watch('trigger', function() {
          elem[0].scrollTop = 0;
        });
      }
    };
  })

  .directive('resultPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        type: '=',
        bucket: '=',
        query: '=',
        display: '=',
        result: '=',
        lang: '=',
        loading: '=',
        searchTabsOrder: '=',
        resultTabsOrder: '=',
        onTypeChanged: '=',
        onBucketChanged: '=',
        onPaginate: '=',
        onUpdateCriteria: '='
      },
      controller: 'SearchResultController',
      templateUrl: 'search/views/search-result-panel-template.html'
    };
  }])

  .directive('criteriaRoot', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '=',
        query: '=',
        advanced: '=',
        onRemove: '=',
        onRefresh: '='
      },
      templateUrl: 'search/views/criteria/criteria-root-template.html',
      link: function(scope) {
        scope.$on(CRITERIA_ITEM_EVENT.deleted, function(event, item){
          scope.onRemove(item);
        });

        scope.$on(CRITERIA_ITEM_EVENT.refresh, function(){
          scope.onRefresh();
        });
      }
    };
  }])

  .directive('criteriaTarget', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '=',
        query: '=',
        advanced: '='
      },
      templateUrl: 'search/views/criteria/criteria-target-template.html'
    };
  }])

  .directive('criteriaNode', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '=',
        query: '=',
        advanced: '='
      },
      controller: 'CriterionLogicalController',
      templateUrl: 'search/views/criteria/criteria-node-template.html'
    };
  }])

  /**
   * This directive creates a hierarchical structure matching that of a RqlQuery tree.
   */
  .directive('criteriaLeaf', ['CriteriaNodeCompileService', function(CriteriaNodeCompileService){
      return {
        restrict: 'EA',
        replace: true,
        scope: {
          item: '=',
          query: '=',
          advanced: '='
        },
        controller: 'CriterionLogicalController',
        link: function(scope, element) {
          CriteriaNodeCompileService.compile(scope, element);
        }
      };
    }])

  .directive('numericCriterion', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '=',
        state: '='
      },
      controller: 'NumericCriterionController',
      templateUrl: 'search/views/criteria/criterion-numeric-template.html'
    };
  }])

  /**
   * This directive serves as the container for each time of criterion based on a vocabulary type.
   * Specialize contents types as directives and share the state with this container.
   */
  .directive('criterionDropdown', ['$document', '$timeout', function ($document, $timeout) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '='
      },
      controller: 'CriterionDropdownController',
      templateUrl: 'search/views/criteria/criterion-dropdown-template.html',//
      link: function( $scope, $element){
        var onDocumentClick = function (event) {
          var isChild = document.querySelector('#'+$scope.criterion.id.replace('.','-')+'-dropdown-'+$scope.timestamp)
            .contains(event.target);
          
          if (!isChild) {
            $timeout(function() {
              $scope.$apply('closeDropdown()');
            });
          }
        };

        $document.on('click', onDocumentClick);
        $element.on('$destroy', function () {
          $document.off('click', onDocumentClick);
        });
      }
    };
  }])

  /**
   * Directive specialized for vocabulary of type String
   */
  .directive('stringCriterionTerms', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '=',
        state: '='
      },
      controller: 'StringCriterionTermsController',
      templateUrl: 'search/views/criteria/criterion-string-terms-template.html'
    };
  }])

  /**
   * Directive specialized for vocabulary of type String
   */
  .directive('matchCriterion', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '=',
        state: '='
      },
      controller: 'MatchCriterionTermsController',
      templateUrl: 'search/views/criteria/criterion-match-template.html'
    };
  }])

  .directive('searchResultPagination', [function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        totalHits: '=',
        onChange: '='
      },
      controller: 'SearchResultPaginationController',
      templateUrl: 'search/views/list/search-result-pagination-template.html'
    };
  }])

  .directive('taxonomiesFacetsPanel',[function() {
    return {
      restrict: 'EA',
      scope: {
        facetedTaxonomies: '=',
        onRefresh: '=',
        onSelectTerm: '=',
        lang: '=',
        criteria: '='
      },
      controller: 'TaxonomiesFacetsController',
      templateUrl: 'search/views/classifications/taxonomies-facets-view.html'
    };
  }])
  
  .directive('taxonomiesPanel',[function() {
    return {
    restrict: 'EA',
    replace: true,
    scope: {
      taxonomyName: '=',
      target: '=',
      onClose: '=',
      onSelectTerm: '=',
      taxonomiesShown: '=',
      lang: '='
    },
    controller: 'TaxonomiesPanelController',
    templateUrl: 'search/views/classifications/taxonomies-view.html',
    link: function(scope, element) {
      scope.closeTaxonomies = function () {
        element.collapse('hide');
        scope.onClose();
      };

      scope.showTaxonomies = function() {
        element.collapse('show');
      };

      element.on('show.bs.collapse', function () {
        scope.taxonomiesShown = true;
      });

      element.on('hide.bs.collapse', function () {
        scope.taxonomiesShown = false;
      });

      scope.$watch('taxonomiesShown', function(value) {
        if(value) {
          element.collapse('show');
        } else {
          element.collapse('hide');
        }
      });

      }
    };
  }])

  .directive('classificationsPanel',[function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        onSelectTerm: '=',
        isHistoryEnabled: '=',
        lang: '='
      },
      controller: 'ClassificationPanelController',
      templateUrl: 'search/views/classifications/classifications-view.html'
    };
  }])

  .factory('Fullscreen', ['$document', '$window', '$rootScope', function ($document, $window, $rootScope) {
    // based on: https://github.com/fabiobiondi/angular-fullscreen
    var document = $document[0];
    var isKeyboardAvailbleOnFullScreen = (typeof $window.Element !== 'undefined' && 'ALLOW_KEYBOARD_INPUT' in $window.Element) && $window.Element.ALLOW_KEYBOARD_INPUT;
    var emitter = $rootScope.$new();


    var serviceInstance = {
      $on: angular.bind(emitter, emitter.$on),
      enable: function(element) {
        if(element.requestFullScreen) {
          element.requestFullScreen();
        } else if(element.mozRequestFullScreen) {
          element.mozRequestFullScreen();
        } else if(element.webkitRequestFullscreen) {
          // Safari temporary fix
          if (/Version\/[\d]{1,2}(\.[\d]{1,2}){1}(\.(\d){1,2}){0,1} Safari/.test($window.navigator.userAgent)) {
            element.webkitRequestFullscreen();
          } else {
            element.webkitRequestFullscreen(isKeyboardAvailbleOnFullScreen);
          }
        } else if (element.msRequestFullscreen) {
          element.msRequestFullscreen();
        }
      },
      cancel: function() {
        if(document.cancelFullScreen) {
          document.cancelFullScreen();
        } else if(document.mozCancelFullScreen) {
          document.mozCancelFullScreen();
        } else if(document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        } else if (document.msExitFullscreen) {
          document.msExitFullscreen();
        }
      },
      isEnabled: function(){
        var fullscreenElement = document.fullscreenElement || document.mozFullScreenElement || document.webkitFullscreenElement || document.msFullscreenElement;
        return fullscreenElement ? true : false;
      }
    };

    $document.on('fullscreenchange webkitfullscreenchange mozfullscreenchange MSFullscreenChange', function(){
      emitter.$emit('ngObibaMicaSearch.fullscreenChange', serviceInstance.isEnabled());
    });

    return serviceInstance;
  }])

  .directive('fullscreen', ['Fullscreen', function(Fullscreen) {
    return {
      link : function ($scope, $element, $attrs) {
        if ($attrs.fullscreen) {
          $scope.$watch($attrs.fullscreen, function(value) {
            var isEnabled = Fullscreen.isEnabled();
            if (value && !isEnabled) {
              Fullscreen.enable($element[0]);
              $element.addClass('isInFullScreen');
            } else if (!value && isEnabled) {
              Fullscreen.cancel();
              $element.removeClass('isInFullScreen');
            }
          });
        }
      }
    };
  }]);
