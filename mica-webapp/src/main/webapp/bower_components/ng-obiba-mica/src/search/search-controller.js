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

/* global CRITERIA_ITEM_EVENT */
/* global QUERY_TARGETS */
/* global QUERY_TYPES */
/* global BUCKET_TYPES */
/* global RQL_NODE */
/* global DISPLAY_TYPES */
/* global CriteriaIdGenerator */
/* global targetToType */
/* global SORT_FIELDS */

/**
 * State shared between Criterion DropDown and its content directives
 *
 * @constructor
 */
function CriterionState() {
  var onOpenCallbacks = [];
  var onCloseCallbacks = [];

  this.dirty = false;
  this.open = false;
  this.loading = true;

  this.addOnOpen = function (callback) {
    onOpenCallbacks.push(callback);
  };

  this.addOnClose = function (callback) {
    onCloseCallbacks.push(callback);
  };

  this.onOpen = function () {
    onOpenCallbacks.forEach(function (callback) {
      callback();
    });
  };

  this.onClose = function () {
    onCloseCallbacks.forEach(function (callback) {
      callback();
    });
  };
}

/**
 * Base controller for taxonomies and classification panels.
 *
 * @param $scope
 * @param $location
 * @param TaxonomyResource
 * @param TaxonomiesResource
 * @param ngObibaMicaSearch
 * @constructor
 */
function BaseTaxonomiesController($scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils) {
  $scope.options = ngObibaMicaSearch.getOptions();
  $scope.RqlQueryUtils = RqlQueryUtils;
  $scope.metaTaxonomy = TaxonomyResource.get({
    target: 'taxonomy',
    taxonomy: 'Mica_taxonomy'
  });

  $scope.taxonomies = {
    all: [],
    search: {
      text: null,
      active: false
    },
    target: $scope.target || 'variable',
    taxonomy: null,
    vocabulary: null
  };

  // vocabulary (or term) will appear in navigation iff it doesn't have the 'showNavigate' attribute
  $scope.canNavigate = function(vocabulary) {
    if ($scope.options.hideNavigate.indexOf(vocabulary.name) > -1) {
      return false;
    }

    return (vocabulary.attributes || []).filter(function (attr) { return attr.key === 'showNavigate'; }).length === 0;
  };

  this.navigateTaxonomy = function (taxonomy, vocabulary, term) {
    $scope.taxonomies.term = term;

    if ($scope.isHistoryEnabled) {
      var search = $location.search();
      search.taxonomy = taxonomy ? taxonomy.name : null;
      search.vocabulary = vocabulary ? vocabulary.name : null;
      $location.search(search);
    } else {
      $scope.taxonomies.taxonomy = taxonomy;
      $scope.taxonomies.vocabulary = vocabulary;
    }
  };

  this.updateStateFromLocation = function () {
    var search = $location.search();
    var taxonomyName = search.taxonomy,
      vocabularyName = search.vocabulary, taxonomy = null, vocabulary = null;

    if (!$scope.taxonomies.all) { //page loading
      return;
    }

    $scope.taxonomies.all.forEach(function (t) {
      if (t.name === taxonomyName) {
        taxonomy = t;
        t.vocabularies.forEach(function (v) {
          if (v.name === vocabularyName) {
            vocabulary = v;
          }
        });
      }
    });

    if (!angular.equals($scope.taxonomies.taxonomy, taxonomy) || !angular.equals($scope.taxonomies.vocabulary, vocabulary)) {
      $scope.taxonomies.taxonomy = taxonomy;
      $scope.taxonomies.vocabulary = vocabulary;
    }
  };

  this.selectTerm = function (target, taxonomy, vocabulary, args) {
    $scope.onSelectTerm(target, taxonomy, vocabulary, args);
  };

  var self = this;

  $scope.$on('$locationChangeSuccess', function () {
    if ($scope.isHistoryEnabled) {
      self.updateStateFromLocation();
    }
  });
  
  $scope.$watch('taxonomies.vocabulary', function(value) {
    if(RqlQueryUtils && value) {
      $scope.taxonomies.isNumericVocabulary = RqlQueryUtils.isNumericVocabulary($scope.taxonomies.vocabulary);
      $scope.taxonomies.isMatchVocabulary = RqlQueryUtils.isMatchVocabulary($scope.taxonomies.vocabulary);
    } else {
      $scope.taxonomies.isNumericVocabulary = null;
      $scope.taxonomies.isMatchVocabulary = null;
    }
  });

  $scope.navigateTaxonomy = this.navigateTaxonomy;
  $scope.selectTerm = this.selectTerm;
}
/**
 * TaxonomiesPanelController
 *
 * @param $scope
 * @param $location
 * @param TaxonomyResource
 * @param TaxonomiesResource
 * @param ngObibaMicaSearch
 * @constructor
 */
function TaxonomiesPanelController($scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils) {
  BaseTaxonomiesController.call(this, $scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils);
  $scope.$watchGroup(['taxonomyName', 'target'], function (newVal) {
    if (newVal[0] && newVal[1]) {
      if ($scope.showTaxonomies) {
        $scope.showTaxonomies();
      }
      $scope.taxonomies.target = newVal[1];
      $scope.taxonomies.search.active = true;
      $scope.taxonomies.all = null;
      $scope.taxonomies.taxonomy = null;
      $scope.taxonomies.vocabulary = null;
      $scope.taxonomies.term = null;
      TaxonomyResource.get({
        target: newVal[1],
        taxonomy: newVal[0]
      }, function onSuccess(response) {
        $scope.taxonomies.taxonomy = response;
        $scope.taxonomies.search.active = false;
      });
    }
  });

}
/**
 * ClassificationPanelController
 * 
 * @param $scope
 * @param $location
 * @param TaxonomyResource
 * @param TaxonomiesResource
 * @param ngObibaMicaSearch
 * @constructor
 */
function ClassificationPanelController($scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils) {
  BaseTaxonomiesController.call(this, $scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils);
  var groupTaxonomies = function (taxonomies, target) {
    var res = taxonomies.reduce(function (res, t) {
      res[t.name] = t;
      return res;
    }, {});

    return $scope.metaTaxonomy.$promise.then(function (metaTaxonomy) {
      var targetVocabulary = metaTaxonomy.vocabularies.filter(function (v) {
        return v.name === target;
      })[0];

      $scope.taxonomyGroups = targetVocabulary.terms.map(function (v) {
        if (!v.terms) {
          var taxonomy = res[v.name];

          if (!taxonomy) {
            return null;
          }

          taxonomy.title = v.title;
          taxonomy.description = v.description;
          return {title: null, taxonomies: [taxonomy]};
        }

        var taxonomies = v.terms.map(function (t) {
          var taxonomy = res[t.name];

          if (!taxonomy) {
            return null;
          }

          taxonomy.title = t.title;
          taxonomy.description = t.description;
          return taxonomy;
        }).filter(function (t) {
          return t;
        });
        var title = v.title.filter(function (t) {
          return t.locale === $scope.lang;
        })[0];
        var description = v.description ? v.description.filter(function (t) {
          return t.locale === $scope.lang;
        })[0] : undefined;

        return {
          title: title ? title.text : null,
          description: description ? description.text : null,
          taxonomies: taxonomies
        };
      }).filter(function (t) {
        return t;
      });
    });
  };

  var self = this;
  $scope.$watch('target', function (newVal) {
    if (newVal) {
      $scope.taxonomies.target = newVal;
      $scope.taxonomies.search.active = true;
      $scope.taxonomies.all = null;
      $scope.taxonomies.taxonomy = null;
      $scope.taxonomies.vocabulary = null;
      $scope.taxonomies.term = null;

      TaxonomiesResource.get({
        target: $scope.taxonomies.target
      }, function onSuccess(taxonomies) {
        $scope.taxonomies.all = taxonomies;
        groupTaxonomies(taxonomies, $scope.taxonomies.target);
        $scope.taxonomies.search.active = false;
        self.updateStateFromLocation();
      });
    }
  });
}

angular.module('obiba.mica.search')

  .controller('SearchController', [
    '$scope',
    '$rootScope',
    '$timeout',
    '$routeParams',
    '$location',
    '$translate',
    '$filter',
    '$cookies',
    'TaxonomiesSearchResource',
    'TaxonomiesResource',
    'TaxonomyResource',
    'VocabularyResource',
    'ngObibaMicaSearchTemplateUrl',
    'ngObibaMicaSearch',
    'JoinQuerySearchResource',
    'JoinQueryCoverageResource',
    'AlertService',
    'ServerErrorUtils',
    'LocalizedValues',
    'RqlQueryService',
    'RqlQueryUtils',
    'SearchContext',
    'CoverageGroupByService',
    function ($scope,
              $rootScope,
              $timeout,
              $routeParams,
              $location,
              $translate,
              $filter,
              $cookies,
              TaxonomiesSearchResource,
              TaxonomiesResource,
              TaxonomyResource,
              VocabularyResource,
              ngObibaMicaSearchTemplateUrl,
              ngObibaMicaSearch,
              JoinQuerySearchResource,
              JoinQueryCoverageResource,
              AlertService,
              ServerErrorUtils,
              LocalizedValues,
              RqlQueryService,
              RqlQueryUtils,
              SearchContext,
              CoverageGroupByService) {

      $scope.options = ngObibaMicaSearch.getOptions();
      var cookiesSearchHelp = 'micaHideSearchHelpText';
      var cookiesClassificationHelp = 'micaHideClassificationHelpBox';

      $translate(['search.help', 'search.coverage-help'])
        .then(function (translation) {
          if(!$scope.options.SearchHelpText && !$cookies.get(cookiesSearchHelp)){
            $scope.options.SearchHelpText = translation['search.help'];
          }
          if(!$scope.options.ClassificationHelpText && !$cookies.get(cookiesClassificationHelp)){
            $scope.options.ClassificationHelpText = translation['classifications.help'];
          }
        });
      // Close the Help search box and set the local cookies
      $scope.closeHelpBox = function () {
        $cookies.put(cookiesSearchHelp, true);
        $scope.options.SearchHelpText = null;
      };

      // Close the Help classification box and set the local cookies
      $scope.closeClassificationHelpBox = function () {
        $cookies.put(cookiesClassificationHelp, true);
        $scope.options.ClassificationHelpText = null;
      };

      // Retrieve from local cookies if user has disabled the Help Search Box and hide the box if true
      if ($cookies.get(cookiesSearchHelp)) {
        $scope.options.SearchHelpText = null;
      }
      // Retrieve from local cookies if user has disabled the Help Classification Box and hide the box if true
      if ($cookies.get(cookiesClassificationHelp)) {
        $scope.options.ClassificationHelpText = null;
      }

      $scope.taxonomyTypeMap = { //backwards compatibility for pluralized naming in configs.
        variable: 'variables',
        study: 'studies',
        network: 'networks',
        dataset: 'datasets'
      };

      $translate(['search.classifications-title', 'search.classifications-link', 'search.faceted-navigation-help'])
        .then(function (translation) {
          $scope.hasClassificationsTitle = translation['search.classifications-title'];
          $scope.hasClassificationsLinkLabel = translation['search.classifications-link'];
          $scope.hasFacetedNavigationHelp = translation['search.faceted-navigation-help'];
        });
      
      var taxonomyTypeInverseMap = Object.keys($scope.taxonomyTypeMap).reduce(function (prev, k) {
        prev[$scope.taxonomyTypeMap[k]] = k;
        return prev;
      }, {});
      $scope.targets = [];
      $scope.lang = LocalizedValues.getLocal();
      $scope.metaTaxonomy = TaxonomyResource.get({
        target: 'taxonomy',
        taxonomy: 'Mica_taxonomy'
      }, function (t) {
        $scope.targets = t.vocabularies.map(function (v) {
          return v.name;
        });
        
        function flattenTaxonomies(terms){
          function inner(acc, terms) {
            angular.forEach(terms, function(t) {
              if(!t.terms) {
                acc.push(t);
                return;
              }

              inner(acc, t.terms);
            });

            return acc;
          }

          return inner([], terms);
        }

        $scope.hasFacetedTaxonomies = false;

        $scope.facetedTaxonomies = t.vocabularies.reduce(function(res, target) {
          var taxonomies = flattenTaxonomies(target.terms);
          
          function getTaxonomy(taxonomyName) {
            return taxonomies.filter(function(t) {
              return t.name === taxonomyName;
            })[0];
          }

          function notNull(t) {
            return t !== null && t !== undefined;
          }

          if($scope.options.showAllFacetedTaxonomies) {
            res[target.name] = taxonomies.filter(function(t) {
              return t.attributes && t.attributes.some(function(att) {
                  return att.key === 'showFacetedNavigation' &&  att.value.toString() === 'true';
                });
            });
          } else {
            res[target.name] = ($scope.options[target.name + 'TaxonomiesOrder'] || []).map(getTaxonomy).filter(notNull);
          }
          
          $scope.hasFacetedTaxonomies = $scope.hasFacetedTaxonomies || res[target.name].length;
          
          return res;
        }, {});
      });

      var searchTaxonomyDisplay = {
        variable: $scope.options.variables.showSearchTab,
        dataset: $scope.options.datasets.showSearchTab,
        study: $scope.options.studies.showSearchTab,
        network: $scope.options.networks.showSearchTab
      };

      function initSearchTabs() {
        $scope.taxonomyNav = [];

        function getTabsOrderParam(arg) {
          var value = $location.search()[arg];

          return value && value.split(',')
              .filter(function (t) {
                return t;
              })
              .map(function (t) {
                return t.trim();
              });
        }

        var targetTabsOrderParam = getTabsOrderParam('targetTabsOrder');
        $scope.targetTabsOrder = (targetTabsOrderParam || $scope.options.targetTabsOrder).filter(function (t) {
          return searchTaxonomyDisplay[t];
        });

        var searchTabsOrderParam = getTabsOrderParam('searchTabsOrder');
        $scope.searchTabsOrder = searchTabsOrderParam || $scope.options.searchTabsOrder;

        var resultTabsOrderParam = getTabsOrderParam('resultTabsOrder');
        $scope.resultTabsOrder = (resultTabsOrderParam || $scope.options.resultTabsOrder).filter(function (t) {
          return searchTaxonomyDisplay[t];
        });

        if($location.search().target) {
          $scope.target = $location.search().target;
        } else if (!$scope.target) {
          $scope.target = $scope.targetTabsOrder[0];
        }

        $scope.metaTaxonomy.$promise.then(function (metaTaxonomy) {
          $scope.targetTabsOrder.forEach(function (target) {
            var targetVocabulary = metaTaxonomy.vocabularies.filter(function (vocabulary) {
              return vocabulary.name === target;
            }).pop();
            if (targetVocabulary && targetVocabulary.terms) {
              targetVocabulary.terms.forEach(function (term) {
                term.target = target;
                var title = term.title.filter(function (t) {
                  return t.locale === $scope.lang;
                })[0];
                var description = term.description ? term.description.filter(function (t) {
                  return t.locale === $scope.lang;
                })[0] : undefined;
                term.locale = {
                  title: title,
                  description: description
                };
                if (term.terms) {
                  term.terms.forEach(function (trm) {
                    var title = trm.title.filter(function (t) {
                      return t.locale === $scope.lang;
                    })[0];
                    var description = trm.description ? trm.description.filter(function (t) {
                      return t.locale === $scope.lang;
                    })[0] : undefined;
                    trm.locale = {
                      title: title,
                      description: description
                    };
                  });
                }
                $scope.taxonomyNav.push(term);
              });
            }
          });
        });

      }

      function onError(response) {
        $scope.search.result = {};
        AlertService.alert({
          id: 'SearchController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response),
          delay: 5000
        });
      }

      function validateType(type) {
        if (!type || !QUERY_TYPES[type.toUpperCase()]) {
          throw new Error('Invalid type: ' + type);
        }
      }

      function validateBucket(bucket) {
        if (bucket && !BUCKET_TYPES[bucket.toUpperCase()]) {
          throw new Error('Invalid bucket: ' + bucket);
        }
      }

      function validateDisplay(display) {
        if (!display || !DISPLAY_TYPES[display.toUpperCase()]) {
          throw new Error('Invalid display: ' + display);
        }
      }

      function getDefaultQueryType() {
        return $scope.taxonomyTypeMap[$scope.resultTabsOrder[0]];
      }

      function getDefaultDisplayType() {
        return $scope.searchTabsOrder[0] || DISPLAY_TYPES.LIST;
      }

      function validateQueryData() {
        try {
          var search = $location.search();
          var type = $scope.resultTabsOrder.indexOf(taxonomyTypeInverseMap[search.type]) > -1 ? search.type : getDefaultQueryType();
          var bucket = search.bucket && CoverageGroupByService.canGroupBy(search.bucket) ? search.bucket : CoverageGroupByService.defaultBucket();
          var display = $scope.searchTabsOrder.indexOf(search.display) > -1 ? search.display : getDefaultDisplayType();
          var query = search.query || '';
          validateType(type);
          validateBucket(bucket);
          validateDisplay(display);
          $scope.search.type = type;
          $scope.search.bucket = bucket;
          $scope.search.display = display;
          $scope.search.query = query;
          $scope.search.rqlQuery = new RqlParser().parse(query);

          return true;
        } catch (e) {
          AlertService.alert({
            id: 'SearchController',
            type: 'danger',
            msg: e.message,
            delay: 5000
          });
        }

        return false;
      }

      function quoteQuery(query) {
        query = query.trim();

        if (query.match(/\s+/)) {
          return '"'+query.replace(/^"|"$/g, '').replace(/"/, '\"')+'"';
        }

        return query;
      }

      var clearSearchQuery = function () {
        var search = $location.search();
        delete search.query;
        $location.search(search);
      };

      var toggleSearchQuery = function () {
        $scope.search.advanced = !$scope.search.advanced;
      };

      var showAdvanced = function() {
        var children = $scope.search.criteria.children || [];
        for(var i = children.length; i--;) {
          var vocabularyChildren = children[i].children || [];
          for (var j = vocabularyChildren.length; j--;) {
            if (vocabularyChildren[j].type === RQL_NODE.OR || vocabularyChildren[j].type === RQL_NODE.AND) {
              return true;
            }
          }
        }
      };
      
      function sortCriteriaItems(items) {
        items.sort(function (a, b) {
          if (a.target === 'network' || b.target === 'variable') {
            return -1;
          }
          if (a.target === 'variable' || b.target === 'network') {
            return 1;
          }
          if (a.target < b.target) {
            return 1;
          }
          if (a.target > b.target) {
            return -1;
          }
          // a must be equal to b
          return 0;
        });
      }

      function loadResults() {
        // execute search only when results are to be shown
        if ($location.path() !== '/search') {
          return;
        }
        var localizedQuery =
          RqlQueryService.prepareSearchQuery(
            $scope.search.type,
            $scope.search.rqlQuery,
            $scope.search.pagination,
            $scope.lang,
            $scope.search.type === QUERY_TYPES.VARIABLES ? SORT_FIELDS.NAME : SORT_FIELDS.ACRONYM
          );
        switch ($scope.search.display) {
          case DISPLAY_TYPES.LIST:
            $scope.search.loading = true;
            $scope.search.executedQuery = localizedQuery;
            JoinQuerySearchResource[$scope.search.type]({query: localizedQuery},
              function onSuccess(response) {
                $scope.search.result.list = response;
                $scope.search.loading = false;
              },
              onError);
            break;
          case DISPLAY_TYPES.COVERAGE:
            var hasVariableCriteria = Object.keys($scope.search.criteriaItemMap).map(function (k) {
                return $scope.search.criteriaItemMap[k];
              }).filter(function (item) {
                return QUERY_TARGETS.VARIABLE  === item.getTarget() && item.taxonomy.name !== 'Mica_variable';
              }).length > 0;

            if (hasVariableCriteria) {
              $scope.search.loading = true;
              $scope.search.executedQuery = RqlQueryService.prepareCoverageQuery(localizedQuery, $scope.search.bucket);
              JoinQueryCoverageResource.get({query: $scope.search.executedQuery},
                function onSuccess(response) {
                  $scope.search.result.coverage = response;
                  $scope.search.loading = false;
                },
                onError);
            } else {
              $scope.search.result = {};
            }

            break;
          case DISPLAY_TYPES.GRAPHICS:
            $scope.search.loading = true;
            $scope.search.executedQuery = RqlQueryService.prepareGraphicsQuery(localizedQuery,
              ['Mica_study.populations-selectionCriteria-countriesIso', 'Mica_study.populations-dataCollectionEvents-bioSamples', 'Mica_study.numberOfParticipants-participant-number'],
              ['Mica_study.methods-designs']);
            JoinQuerySearchResource.studies({query: $scope.search.executedQuery},
              function onSuccess(response) {
                $scope.search.result.graphics = response;
                $scope.search.loading = false;
              },
              onError);
            break;
        }

      }

      function executeSearchQuery() {
        if (validateQueryData()) {
          // build the criteria UI
          RqlQueryService.createCriteria($scope.search.rqlQuery, $scope.lang).then(function (result) {
            // criteria UI is updated here
            $scope.search.criteria = result.root;

            if ($scope.search.criteria && $scope.search.criteria.children) {
              sortCriteriaItems($scope.search.criteria.children);
            }

            $scope.search.criteriaItemMap = result.map;

            if ($scope.search.query) {
              loadResults();
            }

            $scope.$broadcast('ngObibaMicaQueryUpdated', $scope.search.criteria);
          });
        }
      }

      $scope.setLocale = function (locale) {
        $scope.lang = locale;
        SearchContext.setLocale($scope.lang);
        executeSearchQuery();
      };

      var showTaxonomy = function (target, name) {
        if ($scope.target === target && $scope.taxonomyName === name && $scope.taxonomiesShown) {
          $scope.taxonomiesShown = false;
          return;
        }

        $scope.taxonomiesShown = true;
        $scope.target = target;
        $scope.taxonomyName = name;
      };

      var clearTaxonomy = function () {
        $scope.target = null;
        $scope.taxonomyName = null;
      };

      /**
       * Updates the URL location triggering a query execution
       */
      var refreshQuery = function () {
        var query = new RqlQuery().serializeArgs($scope.search.rqlQuery.args);
        var search = $location.search();
        if ('' === query) {
          delete search.query;
        } else {
          search.query = query;
        }
        $location.search(search);
      };

      var clearSearch = function () {
        $scope.documents.search.text = null;
        $scope.documents.search.active = false;
      };

      /**
       * Searches the criteria matching the input query
       *
       * @param query
       * @returns {*}
       */
      var searchCriteria = function (query) {
        // search for taxonomy terms
        // search for matching variables/studies/... count

        function score(item) {
          var result = 0;
          var regExp = new RegExp(query, 'ig');

          if (item.itemTitle.match(regExp)) {
            result = 10;
          } else if (item.itemDescription && item.itemDescription.match(regExp)) {
            result = 8;
          } else if (item.itemParentTitle.match(regExp)) {
            result = 6;
          } else if (item.itemParentDescription && item.itemParentDescription.match(regExp)) {
            result = 4;
          }

          return result;
        }

        // vocabulary (or term) can be used in search if it doesn't have the 'showSearch' attribute
        function canSearch(taxonomyEntity, hideSearchList) {
          if ((hideSearchList || []).indexOf(taxonomyEntity.name) > -1) {
            return false;
          }

          return (taxonomyEntity.attributes || []).filter(function (attr) { return attr.key === 'showSearch'; }).length === 0;
        }

        function processBundle(bundle) {
          var results = [];
          var total = 0;
          var target = bundle.target;
          var taxonomy = bundle.taxonomy;
          if (taxonomy.vocabularies) {
            taxonomy.vocabularies.filter(function (vocabulary) {
              return canSearch(vocabulary, $scope.options.hideSearch);
            }).forEach(function (vocabulary) {
              if (vocabulary.terms) {
                vocabulary.terms.filter(function (term) {
                  return canSearch(term, $scope.options.hideSearch);
                }).forEach(function (term) {
                  var item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, term, $scope.lang);
                  results.push({
                    score: score(item),
                    item: item
                  });
                  total++;
                });
              } else {
                var item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang);
                results.push({
                  score: score(item),
                  item: item
                });
                total++;
              }
            });
          }
          return {results: results, total: total};
        }

        var criteria = TaxonomiesSearchResource.get({
          query: quoteQuery(query), locale: $scope.lang, target: $scope.documents.search.target
        }).$promise.then(function (response) {
          if (response) {
            var results = [];
            var total = 0;
            var size = 10;

            response.forEach(function (bundle) {
              var rval = processBundle(bundle);
              results.push.apply(results, rval.results);
              total = total + rval.total;
            });

            results.sort(function (a, b) {
              return b.score - a.score;
            });

            results = results.splice(0, size);

            if (total > results.length) {
              var note = {
                query: query,
                total: total,
                size: size,
                message: 'Showing ' + size + ' / ' + total,
                status: 'has-warning'
              };
              results.push({score: -1, item: note});
            }

            return results.map(function (result) {
              return result.item;
            });
          } else {
            return [];
          }
        });

        return criteria;
      };

      /**
       * Removes the item from the criteria tree
       * @param item
       */
      var removeCriteriaItem = function (item) {
        RqlQueryService.removeCriteriaItem(item);
        refreshQuery();
      };

      /**
       * Propagates a Scope change that results in criteria panel update
       * @param item
       */
      var selectCriteria = function (item, logicalOp, replace, showNotification) {
        if (angular.isUndefined(showNotification)) {
          showNotification = true;
        }
        
        if (item.id) {
          var id = CriteriaIdGenerator.generate(item.taxonomy, item.vocabulary);
          var existingItem = $scope.search.criteriaItemMap[id];
          var growlMsgKey;

          if (existingItem && id.indexOf('dceIds') !== -1) {
            removeCriteriaItem(existingItem);
            growlMsgKey = 'search.criterion.updated';
            RqlQueryService.addCriteriaItem($scope.search.rqlQuery, item, logicalOp);
          } else if (existingItem) {
            growlMsgKey = 'search.criterion.updated';
            RqlQueryService.updateCriteriaItem(existingItem, item, replace);
          } else {
            growlMsgKey = 'search.criterion.created';
            RqlQueryService.addCriteriaItem($scope.search.rqlQuery, item, logicalOp);
          }

          if (showNotification) {
            AlertService.growl({
              id: 'SearchControllerGrowl',
              type: 'info',
              msgKey: growlMsgKey,
              msgArgs: [LocalizedValues.forLocale(item.vocabulary.title, $scope.lang), $filter('translate')('taxonomy.target.' + item.target)],
              delay: 3000
            });
          }

          refreshQuery();
          $scope.selectedCriteria = null;
        } else {
          $scope.selectedCriteria = item.query;
        }
      };

      var searchKeyUp = function (event) {
        switch (event.keyCode) {
          case 27: // ESC
            if ($scope.documents.search.active) {
              clearSearch();
            }
            break;

          default:
            if ($scope.documents.search.text) {
              searchCriteria($scope.documents.search.text);
            }
            break;
        }
      };

      var onTypeChanged = function (type) {
        if (type) {
          validateType(type);
          var search = $location.search();
          search.type = type;
          $location.search(search);
        }
      };

      var onBucketChanged = function (bucket) {
        if (bucket) {
          validateBucket(bucket);
          var search = $location.search();
          search.bucket = bucket;
          $location.search(search);
        }
      };

      var onPaginate = function (target, from, size) {
        $scope.search.pagination[target] = {from: from, size: size};
        executeSearchQuery();
      };

      var onDisplayChanged = function (display) {
        if (display) {
          validateDisplay(display);
          var search = $location.search();
          search.display = display;
          $location.search(search);
        }
      };

      function reduce(criteriaItem) {
        var parentItem = criteriaItem.parent;
        if (parentItem.type === RQL_NODE.OR) {
          var grandParentItem = parentItem.parent;
          var parentItemIndex = grandParentItem.children.indexOf(parentItem);
          grandParentItem.children[parentItemIndex] = criteriaItem;

          var parentRql = parentItem.rqlQuery;
          var grandParentRql = grandParentItem.rqlQuery;
          var parentRqlIndex = grandParentRql.args.indexOf(parentRql);
          grandParentRql.args[parentRqlIndex] = criteriaItem.rqlQuery;

          if (grandParentItem.type !== QUERY_TARGETS.VARIABLE) {
            reduce(grandParentItem);
          }
        }
      }

      var onUpdateCriteria = function (item, type, useCurrentDisplay, replaceTarget, showNotification) {
        if (type) {
          onTypeChanged(type);
        }

        if (replaceTarget) {
          var criteriaItem = criteriaItemFromMap(item);
          if (criteriaItem) {
            reduce(criteriaItem);
          }
        }

        onDisplayChanged(useCurrentDisplay && $scope.search.display ? $scope.search.display : DISPLAY_TYPES.LIST);
        selectCriteria(item, RQL_NODE.AND, true, showNotification);
      };

      function criteriaItemFromMap(item) {
        var key = Object.keys($scope.search.criteriaItemMap).filter(function (k) {
          return item.id.indexOf(k) !== -1;
        })[0];
        return $scope.search.criteriaItemMap[key];
      }

      var onRemoveCriteria = function(item) {
        var found = RqlQueryService.findCriterion($scope.search.criteria, item.id); 
        removeCriteriaItem(found);
      };

      var onSelectTerm = function (target, taxonomy, vocabulary, args) {
        args = args || {};
        
        if(angular.isString(args)) {
          args = {term: args};
        }
        
        if (vocabulary) {
          var item;
          if (RqlQueryUtils.isNumericVocabulary(vocabulary)) {
            item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang);
            item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
            RqlQueryUtils.updateRangeQuery(item.rqlQuery, args.from, args.to);
            selectCriteria(item, null, true);

            return;
          } else if(RqlQueryUtils.isMatchVocabulary(vocabulary)) {
            item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang);
            item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
            RqlQueryUtils.updateMatchQuery(item.rqlQuery, args.text);
            selectCriteria(item, null, true);

            return;
          }
        }

        selectCriteria(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, args && args.term, $scope.lang));
      };

      var selectSearchTarget = function (target) {
        $scope.documents.search.target = target;
      };

      var VIEW_MODES = {
        SEARCH: 'search',
        CLASSIFICATION: 'classification'
      };

      $scope.goToSearch = function () {
        $scope.viewMode = VIEW_MODES.SEARCH;
        $location.search('taxonomy', null);
        $location.search('vocabulary', null);
        $location.search('target', null);
        $location.path('/search');
      };

      $scope.goToClassifications = function () {
        $scope.viewMode = VIEW_MODES.CLASSIFICATION;
        $location.path('/classifications');
        $location.search('target', $scope.targetTabsOrder[0]);
      };

      $scope.navigateToTarget = function(target) {
        $location.search('target', target);
        $location.search('taxonomy', null);
        $location.search('vocabulary', null);
        $scope.target = target;
      };

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.BUCKET_TYPES = BUCKET_TYPES;

      $scope.search = {
        pagination: {},
        query: null,
        advanced: false,
        rqlQuery: new RqlQuery(),
        executedQuery: null,
        type: null,
        bucket: null,
        result: {
          list: null,
          coverage: null,
          graphics: null
        },
        criteria: [],
        criteriaItemMap: {},
        loading: false
      };

      $scope.viewMode = VIEW_MODES.SEARCH;
      $scope.documents = {
        search: {
          text: null,
          active: false,
          target: null
        }
      };

      $scope.searchHeaderTemplateUrl = ngObibaMicaSearchTemplateUrl.getHeaderUrl('search');
      $scope.classificationsHeaderTemplateUrl = ngObibaMicaSearchTemplateUrl.getHeaderUrl('classifications');
      $scope.selectSearchTarget = selectSearchTarget;
      $scope.selectDisplay = onDisplayChanged;
      $scope.searchCriteria = searchCriteria;
      $scope.selectCriteria = selectCriteria;
      $scope.searchKeyUp = searchKeyUp;

      $scope.showTaxonomy = showTaxonomy;
      $scope.clearTaxonomy = clearTaxonomy;

      $scope.removeCriteriaItem = removeCriteriaItem;
      $scope.refreshQuery = refreshQuery;
      $scope.clearSearchQuery = clearSearchQuery;
      $scope.toggleSearchQuery = toggleSearchQuery;
      $scope.showAdvanced = showAdvanced;

      $scope.onTypeChanged = onTypeChanged;
      $scope.onBucketChanged = onBucketChanged;
      $scope.onDisplayChanged = onDisplayChanged;
      $scope.onUpdateCriteria = onUpdateCriteria;
      $scope.onRemoveCriteria = onRemoveCriteria;
      $scope.onSelectTerm = onSelectTerm;
      $scope.QUERY_TARGETS = QUERY_TARGETS;
      $scope.onPaginate = onPaginate;
      $scope.inSearchMode = function() {
        return $scope.viewMode === VIEW_MODES.SEARCH;
      };
      $scope.toggleFullscreen = function() {
        $scope.isFullscreen = !$scope.isFullscreen;
      };

      $scope.$on('$locationChangeSuccess', function (newLocation, oldLocation) {
        initSearchTabs();

        if (newLocation !== oldLocation) {
          executeSearchQuery();
        }
      });

      $rootScope.$on('ngObibaMicaSearch.fullscreenChange', function(obj, isEnabled) {
        $scope.isFullscreen = isEnabled;
      });

      function init() {
        ngObibaMicaSearch.getLocale(function (locales) {
          if (angular.isArray(locales)) {
            $scope.tabs = locales;
            $scope.lang = locales[0];
          } else {
            $scope.lang = locales || $scope.lang;
          }

          SearchContext.setLocale($scope.lang);
          initSearchTabs();
          executeSearchQuery();
        });
      }

      init();
    }])

  .controller('NumericVocabularyPanelController', ['$scope', function($scope) {
    $scope.$watch('taxonomies', function() {
      $scope.from = null;
      $scope.to = null;
    }, true);
  }])
  
  .controller('MatchVocabularyPanelController', ['$scope', function($scope) {
    $scope.$watch('taxonomies', function() {
      $scope.text = null;
    }, true);
  }])
  
  .controller('NumericVocabularyFacetController', ['$scope','JoinQuerySearchResource', 'RqlQueryService',
    'RqlQueryUtils', function($scope, JoinQuerySearchResource, RqlQueryService, RqlQueryUtils) {
    function updateLimits (criteria, vocabulary) {
      function createExistsQuery(criteria, criterion) {
        var rootQuery = angular.copy(criteria.rqlQuery);
        criterion.rqlQuery = RqlQueryUtils.buildRqlQuery(criterion);
        RqlQueryService.addCriteriaItem(rootQuery, criterion);
        return rootQuery;
      }

      var criterion = RqlQueryService.findCriterion(criteria, CriteriaIdGenerator.generate($scope.$parent.taxonomy, vocabulary));

      if(!criterion) {
        criterion = RqlQueryService.createCriteriaItem($scope.target, $scope.$parent.taxonomy, $scope.vocabulary);
      }

      if(criterion.rqlQuery && criterion.rqlQuery.args[1]) {
        if(angular.isArray(criterion.rqlQuery.args[1])) {
          $scope.from = criterion.rqlQuery.args[1][0];
          $scope.to = criterion.rqlQuery.args[1][1];
        } else {
          if(criterion.rqlQuery.name === RQL_NODE.GE) {
            $scope.from = criterion.rqlQuery.args[1];
          } else {
            $scope.to = criterion.rqlQuery.args[1];
          }
        }
      } else {
        $scope.from = null;
        $scope.to = null;
        $scope.min = null;
        $scope.max = null;
      }

      var query = RqlQueryUtils.hasTargetQuery(criteria.rqlQuery, criterion.target) ? angular.copy(criteria.rqlQuery) : createExistsQuery(criteria, criterion);
      var joinQuery = RqlQueryService.prepareCriteriaTermsQuery(query, criterion);
      JoinQuerySearchResource[targetToType($scope.target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
        var stats = RqlQueryService.getTargetAggregations(joinQueryResponse, criterion, $scope.lang);

        if (stats && stats.default) {
          $scope.min = stats.default.min;
          $scope.max = stats.default.max;
        }
      });
    }

    function updateCriteria() {
      $scope.$parent.selectTerm($scope.$parent.target, $scope.$parent.taxonomy, $scope.vocabulary, {from: $scope.from, to: $scope.to});
    }

    $scope.onKeypress = function(ev) {
      if(ev.keyCode === 13) { updateCriteria(); }
    };

    $scope.$on('ngObibaMicaQueryUpdated', function(ev, criteria) {
      if ($scope.vocabulary.isNumeric && $scope.vocabulary.isOpen) {
        updateLimits(criteria, $scope.vocabulary);
      }
    });

    $scope.$on('ngObibaMicaLoadVocabulary', function(ev, taxonomy, vocabulary) {
      if ($scope.vocabulary.isNumeric &&
        vocabulary.name === $scope.vocabulary.name && !vocabulary.isOpen) {
        updateLimits($scope.criteria, vocabulary);
      }
    });
  }])

  .controller('MatchVocabularyFacetController', ['$scope', 'RqlQueryService', function($scope, RqlQueryService) {
    function updateMatch (criteria, vocabulary) {
      var criterion = RqlQueryService.findCriterion(criteria, CriteriaIdGenerator.generate($scope.$parent.taxonomy, vocabulary));
      if(criterion && criterion.rqlQuery && criterion.rqlQuery.args[1]) {
        $scope.text = criterion.rqlQuery.args[0];
      } else {
        $scope.text = null;
      }
    }
    
    function updateCriteria() {
      $scope.$parent.selectTerm($scope.$parent.target, $scope.$parent.taxonomy, $scope.vocabulary, {text: $scope.text || '*'});
    }
    
    $scope.onKeypress = function(ev) {
      if(ev.keyCode === 13) {
        updateCriteria();
      }
    };

    $scope.$on('ngObibaMicaQueryUpdated', function(ev, criteria) {
      if ($scope.vocabulary.isMatch && $scope.vocabulary.isOpen) {
        updateMatch(criteria, $scope.vocabulary);
      }
    });

    $scope.$on('ngObibaMicaLoadVocabulary', function(ev, taxonomy, vocabulary) {
      if (vocabulary.name === $scope.vocabulary.name && !vocabulary.isOpen) {
        updateMatch($scope.criteria, vocabulary);
      }
    });
  }])

  .controller('TermsVocabularyFacetController', ['$scope', '$filter', 'JoinQuerySearchResource', 'RqlQueryService',
    'RqlQueryUtils',
    function($scope, $filter, JoinQuerySearchResource, RqlQueryService, RqlQueryUtils) {
      function isSelectedTerm (criterion, term) {
        return criterion.selectedTerms && criterion.selectedTerms.indexOf(term.key) !== -1;
      }

      $scope.selectTerm = function (target, taxonomy, vocabulary, args) {
        var selected = vocabulary.terms.filter(function(t) {return t.selected;}).map(function(t) { return t.name; }),
          criterion = RqlQueryService.findCriterion($scope.criteria, CriteriaIdGenerator.generate(taxonomy, vocabulary));
        if(criterion) {
          if (selected.length === 0 && $scope.selectedFilter !== RQL_NODE.MISSING) {
            criterion.rqlQuery.name = RQL_NODE.EXISTS;
          }

          RqlQueryUtils.updateQuery(criterion.rqlQuery, selected);
          $scope.onRefresh();
        } else {
          $scope.onSelectTerm(target, taxonomy, vocabulary, args);
        }
      };

      function updateCounts(criteria, vocabulary) {
        var query = null, isCriterionPresent = false;

        function createExistsQuery(criteria, criterion) {
          var rootQuery = angular.copy(criteria.rqlQuery);
          criterion.rqlQuery = RqlQueryUtils.buildRqlQuery(criterion);
          RqlQueryService.addCriteriaItem(rootQuery, criterion);
          return rootQuery;
        }

        var criterion = RqlQueryService.findCriterion(criteria,
          CriteriaIdGenerator.generate($scope.$parent.taxonomy, vocabulary));

        if(criterion) {
          isCriterionPresent = true;
        } else {
          criterion = RqlQueryService.createCriteriaItem($scope.target, $scope.$parent.taxonomy, $scope.vocabulary);
        }
        
        if(RqlQueryUtils.hasTargetQuery(criteria.rqlQuery, criterion.target)) {
          query = angular.copy(criteria.rqlQuery);
          
          if(!isCriterionPresent) {
            RqlQueryService.addCriteriaItem(query, criterion, RQL_NODE.OR);
          }
        } else {
          query = createExistsQuery(criteria, criterion); 
        }
        
        var joinQuery = RqlQueryService.prepareCriteriaTermsQuery(query, criterion, criterion.lang);
        JoinQuerySearchResource[targetToType($scope.target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
          RqlQueryService.getTargetAggregations(joinQueryResponse, criterion, criterion.lang).forEach(function (term) {
            $scope.vocabulary.terms.some(function(t) {
              if (t.name === term.key) {
                t.selected = isSelectedTerm(criterion, term);
                t.count = term.count;
                return true;
              }
            });
          });
        });
      }
      
      $scope.$on('ngObibaMicaQueryUpdated', function(ev, criteria) {
        if(!$scope.vocabulary.isNumeric && !$scope.vocabulary.isMatch && $scope.vocabulary.isOpen) {
          updateCounts(criteria, $scope.vocabulary);
        }
      });
      
      $scope.$on('ngObibaMicaLoadVocabulary', function(ev, taxonomy, vocabulary) {
        if(vocabulary.name === $scope.vocabulary.name && !$scope.vocabulary.isNumeric && !$scope.vocabulary.isMatch &&
          !vocabulary.isOpen) {
          updateCounts($scope.criteria, vocabulary);
        }
      });
  }])

  .controller('TaxonomiesPanelController', ['$scope', '$location', 'TaxonomyResource',
    'TaxonomiesResource', 'ngObibaMicaSearch', 'RqlQueryUtils', TaxonomiesPanelController])

  .controller('ClassificationPanelController', ['$scope', '$location', 'TaxonomyResource',
    'TaxonomiesResource', 'ngObibaMicaSearch', 'RqlQueryUtils', ClassificationPanelController])

  .controller('TaxonomiesFacetsController', ['$scope', 'TaxonomyResource', 'TaxonomiesResource', 'LocalizedValues', 'ngObibaMicaSearch',
    'RqlQueryUtils', function ($scope, TaxonomyResource, TaxonomiesResource, LocalizedValues, ngObibaMicaSearch, RqlQueryUtils) {
      $scope.options = ngObibaMicaSearch.getOptions();
      $scope.taxonomies = {};
      $scope.targets = [];
      $scope.RqlQueryUtils = RqlQueryUtils;
      
      $scope.$watch('facetedTaxonomies', function(facetedTaxonomies) {
        if(facetedTaxonomies) {
          $scope.targets = $scope.options.targetTabsOrder.filter(function (t) {
            return facetedTaxonomies[t].length;
          });
          
          $scope.target = $scope.targets[0];
          init($scope.target);
        }
      });

      $scope.selectTerm = function(target, taxonomy, vocabulary, args) {
        $scope.onSelectTerm(target, taxonomy, vocabulary, args);
      };
      
      $scope.setTarget = function(target) {
        $scope.target=target;
        init(target);
      };
      
      $scope.loadVocabulary = function(taxonomy, vocabulary) {
        $scope.$broadcast('ngObibaMicaLoadVocabulary', taxonomy, vocabulary);
      };

      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.lang);
      };
      
      function init(target) {
        if($scope.taxonomies[target]) { return; }
        
        TaxonomiesResource.get({
          target: target 
        }, function onSuccess(taxonomies) {
          $scope.taxonomies[target] = $scope.facetedTaxonomies[target].map(function(f) {
            return taxonomies.filter(function(t) {
              return f.name === t.name;
            })[0];
          }).filter(function(t) { return t; }).map(function(t) {
            t.vocabularies.map(function (v) {
              v.limit = 10;
              v.isMatch = RqlQueryUtils.isMatchVocabulary(v);
              v.isNumeric = RqlQueryUtils.isNumericVocabulary(v);
            });
            
            return t;
          });
          
          if($scope.taxonomies[target].length === 1) {
            $scope.taxonomies[target][0].isOpen = 1;
          }
        });
      }
    }
  ])
  
  .controller('SearchResultController', [
    '$scope',
    'ngObibaMicaSearch',
    function ($scope,
              ngObibaMicaSearch) {

      function updateTarget(type) {
        Object.keys($scope.activeTarget).forEach(function (key) {
          $scope.activeTarget[key].active = type === key;
        });
      }

      $scope.targetTypeMap = $scope.$parent.taxonomyTypeMap;
      $scope.QUERY_TARGETS = QUERY_TARGETS;
      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.options = ngObibaMicaSearch.getOptions();
      $scope.activeTarget = {};
      $scope.activeTarget[QUERY_TYPES.VARIABLES] = {active: false, name: QUERY_TARGETS.VARIABLE, totalHits: 0};
      $scope.activeTarget[QUERY_TYPES.DATASETS] = {active: false, name: QUERY_TARGETS.DATASET, totalHits: 0};
      $scope.activeTarget[QUERY_TYPES.STUDIES] = {active: false, name: QUERY_TARGETS.STUDY, totalHits: 0};
      $scope.activeTarget[QUERY_TYPES.NETWORKS] = {active: false, name: QUERY_TARGETS.NETWORK, totalHits: 0};

      $scope.selectTarget = function (type) {
        updateTarget(type);
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };

      $scope.$watchCollection('result', function () {
        if ($scope.result.list) {
          $scope.activeTarget[QUERY_TYPES.VARIABLES].totalHits = $scope.result.list.variableResultDto.totalHits;
          $scope.activeTarget[QUERY_TYPES.DATASETS].totalHits = $scope.result.list.datasetResultDto.totalHits;
          $scope.activeTarget[QUERY_TYPES.STUDIES].totalHits = $scope.result.list.studyResultDto.totalHits;
          $scope.activeTarget[QUERY_TYPES.NETWORKS].totalHits = $scope.result.list.networkResultDto.totalHits;
        }
      });


      $scope.$watch('type', function (type) {
        updateTarget(type);
      });

      $scope.DISPLAY_TYPES = DISPLAY_TYPES;
    }])

  .controller('CriterionLogicalController', [
    '$scope',
    function ($scope) {
      $scope.updateLogical = function (operator) {
        $scope.item.rqlQuery.name = operator;
        $scope.$emit(CRITERIA_ITEM_EVENT.refresh);
      };
    }])

  .controller('CriterionDropdownController', [
    '$scope',
    '$filter',
    'LocalizedValues',
    'RqlQueryUtils',
    'StringUtils',
    function ($scope, $filter, LocalizedValues, RqlQueryUtils, StringUtils) {
      var closeDropdown = function () {
        if (!$scope.state.open) {
          return;
        }

        $scope.state.onClose();

        var wasDirty = $scope.state.dirty;
        $scope.state.open = false;
        $scope.state.dirty = false;
        if (wasDirty) {
          // trigger a query update
          $scope.$emit(CRITERIA_ITEM_EVENT.refresh);
        }
      };

      var openDropdown = function () {
        if ($scope.state.open) {
          closeDropdown();
          return;
        }

        $scope.state.open = true;
        $scope.state.onOpen();
      };

      var remove = function () {
        $scope.$emit(CRITERIA_ITEM_EVENT.deleted, $scope.criterion);
      };

      var onKeyup = function (event) {
        if (event.keyCode === 13) {
          closeDropdown();
        }
      };

      $scope.state = new CriterionState();
      $scope.timestamp = new Date().getTime();
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };
      $scope.localizeCriterion = function () {
        var rqlQuery = $scope.criterion.rqlQuery;
        if ((rqlQuery.name === RQL_NODE.IN || rqlQuery.name === RQL_NODE.CONTAINS) && $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.length > 0) {
          var sep = rqlQuery.name === RQL_NODE.IN ? ' | ' : ' + ';
          return $scope.criterion.selectedTerms.map(function (t) {
            if (!$scope.criterion.vocabulary.terms) {
              return t;
            }
            var found = $scope.criterion.vocabulary.terms.filter(function (arg) {
              return arg.name === t;
            }).pop();
            return found ? LocalizedValues.forLocale(found.title, $scope.criterion.lang) : t;
          }).join(sep);
        }
        var operation = rqlQuery.name;
        switch (rqlQuery.name) {
          case RQL_NODE.EXISTS:
            operation = ':' + $filter('translate')('any');
            break;
          case RQL_NODE.MISSING:
            operation = ':' + $filter('translate')('none');
            break;
          case RQL_NODE.EQ:
            operation = '=' + rqlQuery.args[1];
            break;
          case RQL_NODE.GE:
            operation = '>' + rqlQuery.args[1];
            break;
          case RQL_NODE.LE:
            operation = '<' + rqlQuery.args[1];
            break;
          case RQL_NODE.BETWEEN:
            operation = ':[' + rqlQuery.args[1] + ')';
            break;
          case RQL_NODE.IN:
          case RQL_NODE.CONTAINS:
            operation = '';
            break;
          case RQL_NODE.MATCH:
            operation = ':match(' + rqlQuery.args[0] + ')';
            break;
        }
        return LocalizedValues.forLocale($scope.criterion.vocabulary.title, $scope.criterion.lang) + operation;
      };
      $scope.vocabularyType = function (vocabulary) {
        return RqlQueryUtils.vocabularyType(vocabulary);
      };
      $scope.onKeyup = onKeyup;
      $scope.truncate = StringUtils.truncate;
      $scope.remove = remove;
      $scope.openDropdown = openDropdown;
      $scope.closeDropdown = closeDropdown;
      $scope.RqlQueryUtils = RqlQueryUtils;
    }])

  .controller('MatchCriterionTermsController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    'SearchContext',
    function ($scope, RqlQueryService, LocalizedValues, JoinQuerySearchResource, RqlQueryUtils, SearchContext) {
      $scope.lang = SearchContext.currentLocale();

      var update = function () {
        $scope.state.dirty = true;
        RqlQueryUtils.updateMatchQuery($scope.criterion.rqlQuery, $scope.match);
      };

      var queryString = $scope.criterion.rqlQuery.args[0];
      $scope.match = queryString === '*' ? '' : queryString;
      $scope.update = update;
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };

    }])

  .controller('NumericCriterionController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    'SearchContext',
    function ($scope, RqlQueryService, LocalizedValues, JoinQuerySearchResource, RqlQueryUtils, SearchContext) {
      $scope.lang = SearchContext.currentLocale();
      var range = $scope.criterion.rqlQuery.args[1];

      if (angular.isArray(range)) {
        $scope.from = $scope.criterion.rqlQuery.args[1][0];
        $scope.to = $scope.criterion.rqlQuery.args[1][1];
      } else {
        $scope.from = $scope.criterion.rqlQuery.name === RQL_NODE.GE ? range : null;
        $scope.to = $scope.criterion.rqlQuery.name === RQL_NODE.LE ? range : null;
      }

      var updateLimits = function () {
        var target = $scope.criterion.target,
          joinQuery = RqlQueryService.prepareCriteriaTermsQuery($scope.query, $scope.criterion);
        JoinQuerySearchResource[targetToType(target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
          var stats = RqlQueryService.getTargetAggregations(joinQueryResponse, $scope.criterion, $scope.lang);

          if (stats && stats.default) {
            $scope.min = stats.default.min;
            $scope.max = stats.default.max;
          }
        });
      };

      var onOpen = function () {
        updateLimits();
      };

      var onClose = function () {
        $scope.updateSelection();
      };

      $scope.updateSelection = function () {
        RqlQueryUtils.updateRangeQuery($scope.criterion.rqlQuery, $scope.from, $scope.to, $scope.selectMissing);
        $scope.state.dirty = true;
      };

      $scope.selectMissing = $scope.criterion.rqlQuery.name === RQL_NODE.MISSING;
      $scope.state.addOnClose(onClose);
      $scope.state.addOnOpen(onOpen);
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };
    }])

  .controller('StringCriterionTermsController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'StringUtils',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    'SearchContext',
    '$filter',
    function ($scope,
              RqlQueryService,
              LocalizedValues,
              StringUtils,
              JoinQuerySearchResource,
              RqlQueryUtils,
              SearchContext,
              $filter) {
      $scope.lang = SearchContext.currentLocale();

      var isSelected = function (name) {
        return $scope.checkboxTerms.indexOf(name) !== -1;
      };

      var updateSelection = function () {
        $scope.state.dirty = true;
        $scope.criterion.rqlQuery.name = $scope.selectedFilter;
        var selected = [];
        if($scope.selectedFilter !== RQL_NODE.MISSING && $scope.selectedFilter !== RQL_NODE.EXISTS) {
          Object.keys($scope.checkboxTerms).forEach(function (key) {
            if ($scope.checkboxTerms[key]) {
              selected.push(key);
            }
          });
        }
        if (selected.length === 0 && $scope.selectedFilter !== RQL_NODE.MISSING) {
          $scope.criterion.rqlQuery.name = RQL_NODE.EXISTS;
        }
        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, selected);
      };

      var updateFilter = function () {
        updateSelection();
      };

      var isInFilter = function () {
        return $scope.selectedFilter === RQL_NODE.IN;
      };

      var isContainsFilter = function () {
        return $scope.selectedFilter === RQL_NODE.CONTAINS;
      };

      var onOpen = function () {
        $scope.state.loading = true;
        var target = $scope.criterion.target;
        var joinQuery = RqlQueryService.prepareCriteriaTermsQuery($scope.query, $scope.criterion, $scope.lang);

        JoinQuerySearchResource[targetToType(target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
          $scope.state.loading = false;
          $scope.terms = RqlQueryService.getTargetAggregations(joinQueryResponse, $scope.criterion, $scope.lang);
          if ($scope.terms) {
            $scope.terms.forEach(function (term) {
              $scope.checkboxTerms[term.key] = $scope.isSelectedTerm(term);
            });

            $scope.terms = $filter('orderBySelection')($scope.terms, $scope.checkboxTerms);
          }
        });
      };

      $scope.isSelectedTerm = function (term) {
        return $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.indexOf(term.key) !== -1;
      };

      $scope.state.addOnOpen(onOpen);
      $scope.checkboxTerms = {};
      $scope.RQL_NODE = RQL_NODE;
      $scope.selectedFilter = $scope.criterion.type;
      $scope.isSelected = isSelected;
      $scope.updateFilter = updateFilter;
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };
      $scope.truncate = StringUtils.truncate;
      $scope.isInFilter = isInFilter;
      $scope.isContainsFilter = isContainsFilter;
      $scope.updateSelection = updateSelection;
    }])

  .controller('CoverageResultTableController', [
    '$scope',
    '$location',
    '$q',
    'PageUrlService',
    'RqlQueryUtils',
    'RqlQueryService',
    'CoverageGroupByService',
    function ($scope, $location, $q, PageUrlService, RqlQueryUtils, RqlQueryService, CoverageGroupByService) {
      var targetMap = {}, vocabulariesTermsMap = {};
      targetMap[BUCKET_TYPES.NETWORK] = QUERY_TARGETS.NETWORK;
      targetMap[BUCKET_TYPES.STUDY] = QUERY_TARGETS.STUDY;
      targetMap[BUCKET_TYPES.DCE] = QUERY_TARGETS.VARIABLE;
      targetMap[BUCKET_TYPES.DATASCHEMA] = QUERY_TARGETS.DATASET;
      targetMap[BUCKET_TYPES.DATASET] = QUERY_TARGETS.DATASET;

      $scope.showMissing = true;
      $scope.toggleMissing = function (value) {
        $scope.showMissing = value;
      };
      $scope.groupByOptions = CoverageGroupByService;
      $scope.bucketSelection = {
        dceBucketSelected: $scope.bucket === BUCKET_TYPES.DCE,
        datasetBucketSelected: $scope.bucket !== BUCKET_TYPES.DATASCHEMA
      };

      function decorateVocabularyHeaders(headers, vocabularyHeaders) {
        var count = 0, i = 0;
        for (var j=0 ; j < vocabularyHeaders.length; j ++) {
          if (count >= headers[i].termsCount) {
            i++;
            count = 0;
          }
          
          count += vocabularyHeaders[j].termsCount;
          vocabularyHeaders[j].taxonomyName = headers[i].entity.name;
        }
      }
      
      function decorateTermHeaders(headers, termHeaders, attr) {
        var idx = 0;
        return headers.reduce(function(result, h) {
          result[h.entity.name] = termHeaders.slice(idx, idx + h.termsCount).map(function(t) {
            if(h.termsCount > 1 && attr === 'vocabularyName') {
              t.canRemove = true;
            }
            
            t[attr] = h.entity.name;

            return t;
          });

          idx += h.termsCount;
          return result;
        }, {});
      }

      $scope.$watch('bucketSelection.dceBucketSelected', function (val, old) {
        if (val === old) {
          return;
        }

        if (val) {
          $scope.selectBucket(BUCKET_TYPES.DCE);
        } else if ($scope.bucket === BUCKET_TYPES.DCE) {
          $scope.selectBucket(BUCKET_TYPES.STUDY);
        }
      });

      $scope.$watch('bucketSelection.datasetBucketSelected', function (val, old) {
        if (val === old) {
          return;
        }

        if (val) {
          $scope.selectBucket(BUCKET_TYPES.DATASET);
        } else if ($scope.bucket === BUCKET_TYPES.DATASET) {
          $scope.selectBucket(BUCKET_TYPES.DATASCHEMA);
        }
      });

      $scope.selectBucket = function (bucket) {
        if (bucket === BUCKET_TYPES.STUDY && $scope.bucketSelection.dceBucketSelected) {
          bucket = BUCKET_TYPES.DCE;
        }

        if (bucket === BUCKET_TYPES.DATASET && !$scope.bucketSelection.datasetBucketSelected) {
          bucket = BUCKET_TYPES.DATASCHEMA;
        }

        $scope.bucket = bucket;
        $scope.$parent.onBucketChanged(bucket);
      };

      $scope.rowspans = {};

      $scope.getSpan = function (study, population) {
        var length = 0;
        if (population) {
          var prefix = study + ':' + population;
          length = $scope.result.rows.filter(function (row) {
            return row.title.startsWith(prefix + ':');
          }).length;
          $scope.rowspans[prefix] = length;
          return length;
        } else {
          length = $scope.result.rows.filter(function (row) {
            return row.title.startsWith(study + ':');
          }).length;
          $scope.rowspans[study] = length;
          return length;
        }
      };

      $scope.hasSpan = function (study, population) {
        if (population) {
          return $scope.rowspans[study + ':' + population] > 0;
        } else {
          return $scope.rowspans[study] > 0;
        }
      };

      $scope.hasVariableTarget = function () {
        var query = $location.search().query;
        return query && RqlQueryUtils.hasTargetQuery(new RqlParser().parse(query), RQL_NODE.VARIABLE);
      };

      $scope.hasSelected = function () {
        return $scope.table && $scope.table.rows && $scope.table.rows.filter(function (r) {
            return r.selected;
          }).length;
      };

      $scope.selectAll = function() {
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            r.selected = true;
          });
        }
      };

      $scope.selectNone = function() {
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            r.selected = false;
          });
        }
      };

      $scope.selectFull = function() {
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            if (r.hits) {
              r.selected = r.hits.filter(function(h){
                return h === 0;
              }).length === 0;
            } else {
              r.selected = false;
            }
          });
        }
      };      

      function getBucketUrl(bucket, id) {
        switch (bucket) {
          case BUCKET_TYPES.STUDY:
          case BUCKET_TYPES.DCE:
            return PageUrlService.studyPage(id);
          case BUCKET_TYPES.NETWORK:
            return PageUrlService.networkPage(id);
          case BUCKET_TYPES.DATASCHEMA:
            return PageUrlService.datasetPage(id, 'harmonization');
          case BUCKET_TYPES.DATASET:
            return PageUrlService.datasetPage(id, 'study');
        }

        return '';
      }

      function updateFilterCriteriaInternal(selected) {
        var vocabulary = $scope.bucket === BUCKET_TYPES.DCE ? 'dceIds' : 'id';
        $q.all(selected.map(function (r) {
          return RqlQueryService.createCriteriaItem(targetMap[$scope.bucket], 'Mica_' + targetMap[$scope.bucket], vocabulary, r.value);
        })).then(function (items) {
          if (!items.length) {
            return;
          }

          var selectionItem = items.reduce(function (prev, item) {
            if (prev) {
              RqlQueryService.updateCriteriaItem(prev, item);
              return prev;
            }

            item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
            return item;
          }, null);

          $scope.onUpdateCriteria(selectionItem, 'variables', true);
        });
      }

      function splitIds() {
        var cols = {
          colSpan: $scope.bucket === BUCKET_TYPES.DCE ? 3 : 1,
          ids: {}
        };

        var rowSpans = {};

        function appendRowSpan(id) {
          var rowSpan;
          if (!rowSpans[id]) {
            rowSpan = 1;
            rowSpans[id] = 1;
          } else {
            rowSpan = 0;
            rowSpans[id] = rowSpans[id] + 1;
          }
          return rowSpan;
        }

        var minMax = {};

        function appendMinMax(id, start, end) {
          if (minMax[id]) {
            if (start < minMax[id][0]) {
              minMax[id][0] = start;
            }
            if (end > minMax[id][1]) {
              minMax[id][1] = end;
            }
          } else {
            minMax[id] = [start, end];
          }
        }

        function toTime(yearMonth, start) {
          var res;
          if (yearMonth) {
            if (yearMonth.indexOf('-')>0) {
              var ym = yearMonth.split('-');
              if (!start) {
                var m = parseInt(ym[1]);
                if(m<12) {
                  ym[1] = m + 1;
                } else {
                  ym[0] = parseInt(ym[0]) + 1;
                  ym[1] = 1;
                }
              }
              var ymStr = ym[0] + '/'  + ym[1] + '/01';
              res = Date.parse(ymStr);
            } else {
              res = start ? Date.parse(yearMonth + '/01/01') : Date.parse(yearMonth + '/12/31');
            }
          }
          return res;
        }

        var currentYear = new Date().getFullYear();
        var currentMonth = new Date().getMonth() + 1;
        var currentDate = toTime(currentYear + '-' + currentMonth, true);

        function getProgress(startYearMonth, endYearMonth) {
          var start = toTime(startYearMonth, true);
          var end = endYearMonth ? toTime(endYearMonth, false) : currentDate;
          var current = end < currentDate ? end : currentDate;
          if(end === start) {
            return 100;
          } else {
            return Math.round(startYearMonth ? 100 * (current - start) / (end - start) : 0);
          }
        }

        var odd = true;
        var groupId;
        $scope.result.rows.forEach(function (row) {
          cols.ids[row.value] = [];
          if ($scope.bucket === BUCKET_TYPES.DCE) {
            var ids = row.value.split(':');
            var titles = row.title.split(':');
            var descriptions = row.description.split(':');
            var rowSpan;
            var id;

            // study
            id = ids[0];
            if (!groupId) {
              groupId = id;
            } else if(id !== groupId) {
              odd = !odd;
              groupId = id;
            }
            rowSpan = appendRowSpan(id);
            appendMinMax(id,row.start, row.end);
            cols.ids[row.value].push({
              id: id,
              url: PageUrlService.studyPage(id),
              title: titles[0],
              description: descriptions[0],
              rowSpan: rowSpan
            });

            // population
            id = ids[0] + ':' + ids[1];
            rowSpan = appendRowSpan(id);
            cols.ids[row.value].push({
              id: id,
              url: PageUrlService.studyPopulationPage(ids[0], ids[1]),
              title: titles[1],
              description: descriptions[1],
              rowSpan: rowSpan
            });

            // dce
            cols.ids[row.value].push({
              id: row.value,
              title: titles[2],
              description: descriptions[2],
              start: row.start,
              current: currentYear + '-' + currentMonth,
              end: row.end,
              progressClass: odd ? 'info' : 'warning',
              url: PageUrlService.studyPopulationPage(ids[0], ids[1]),
              rowSpan: 1
            });
          } else {
            cols.ids[row.value].push({
              id: row.value,
              url: getBucketUrl($scope.bucket, row.value),
              title: row.title,
              description: row.description,
              min: row.start,
              start: row.start,
              current: currentYear,
              end: row.end,
              max: row.end,
              progressStart: 0,
              progress: getProgress(row.start ? row.start + '-01' : undefined, row.end ? row.end + '-12' : undefined),
              progressClass: odd ? 'info' : 'warning',
              rowSpan: 1
            });
            odd = !odd;
          }
        });

        // adjust the rowspans and the progress
        if ($scope.bucket === BUCKET_TYPES.DCE) {
          $scope.result.rows.forEach(function (row) {
            if (cols.ids[row.value][0].rowSpan > 0) {
              cols.ids[row.value][0].rowSpan = rowSpans[cols.ids[row.value][0].id];
            }
            if (cols.ids[row.value][1].rowSpan > 0) {
              cols.ids[row.value][1].rowSpan = rowSpans[cols.ids[row.value][1].id];
            }
            var ids = row.value.split(':');
            if (minMax[ids[0]]) {
              var min = minMax[ids[0]][0];
              var max = minMax[ids[0]][1];
              var start = cols.ids[row.value][2].start;
              var end = cols.ids[row.value][2].end;
              var diff = toTime(max, false) - toTime(min, true);
              // set the DCE min and max dates of the study
              cols.ids[row.value][2].min = min;
              cols.ids[row.value][2].max = max;
              // compute the progress
              cols.ids[row.value][2].progressStart = 100 * (toTime(start, true) - toTime(min, true))/diff;
              cols.ids[row.value][2].progress = 100 * (toTime(end, false) - toTime(start, true))/diff;
            }
          });
        }

        return cols;
      }
      
      function mergeCriteriaItems(criteria) {
        return criteria.reduce(function(prev, item) {
          if (prev) {
            RqlQueryService.updateCriteriaItem(prev, item);
            return prev;
          }

          item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
          return item;
        }, null);
      }

      $scope.BUCKET_TYPES = BUCKET_TYPES;

      $scope.downloadUrl = function () {
        return PageUrlService.downloadCoverage($scope.query);
      };

      $scope.$watch('result', function () {
        $scope.table = {cols: []};
        vocabulariesTermsMap = {};
        
        if ($scope.result && $scope.result.rows) {
          var tableTmp = $scope.result;
          tableTmp.cols = splitIds();
          $scope.table = tableTmp;

          vocabulariesTermsMap = decorateTermHeaders($scope.table.vocabularyHeaders, $scope.table.termHeaders, 'vocabularyName');
          decorateTermHeaders($scope.table.taxonomyHeaders, $scope.table.termHeaders, 'taxonomyName');
          decorateVocabularyHeaders($scope.table.taxonomyHeaders, $scope.table.vocabularyHeaders);
        }
      });

      $scope.updateCriteria = function (id, term, idx, type) { //
        var vocabulary = $scope.bucket === BUCKET_TYPES.DCE ? 'dceIds' : 'id';
        var criteria = {varItem: RqlQueryService.createCriteriaItem(QUERY_TARGETS.VARIABLE, term.taxonomyName, term.vocabularyName, term.entity.name)};

        // if id is null, it is a click on the total count for the term
        if (id) {
          criteria.item = RqlQueryService.createCriteriaItem(targetMap[$scope.bucket], 'Mica_' + targetMap[$scope.bucket], vocabulary, id);
        } else if ($scope.bucket === BUCKET_TYPES.STUDY || $scope.bucket === BUCKET_TYPES.DATASET) {
          criteria.item = RqlQueryService.createCriteriaItem(QUERY_TARGETS.DATASET, 'Mica_' + QUERY_TARGETS.DATASET, 'className', 'StudyDataset');
        } else if ($scope.bucket === BUCKET_TYPES.NETWORK || $scope.bucket === BUCKET_TYPES.DATASCHEMA) {
          criteria.item = RqlQueryService.createCriteriaItem(QUERY_TARGETS.DATASET, 'Mica_' + QUERY_TARGETS.DATASET, 'className', 'HarmonizationDataset');
        }

        $q.all(criteria).then(function (criteria) {
          $scope.onUpdateCriteria(criteria.varItem, type, false, true);

          if (criteria.item) {
            $scope.onUpdateCriteria(criteria.item, type);
          }
        });
      };
      
      $scope.isFullCoverageImpossibleOrCoverageAlreadyFull = function () {
        var rows = $scope.table ? ($scope.table.rows || []) : [];
        var rowsWithZeroHitColumn = 0;

        if (rows.length === 0) {
          return true;
        }

        rows.forEach(function (row) {
          if (row.hits) {
            if (row.hits.filter(function (hit) { return hit === 0; }).length > 0) {
              rowsWithZeroHitColumn++;
            }
          }
        });
        
        if (rowsWithZeroHitColumn === 0) {
          return true;
        }

        return rows.length === rowsWithZeroHitColumn;
      };

      $scope.selectFullAndFilter = function() {
        var selected = [];
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            if (r.hits) {
              if (r.hits.filter(function(h){
                  return h === 0;
                }).length === 0) {
                selected.push(r);
              }
            }
          });
        }
        updateFilterCriteriaInternal(selected);
      };

      $scope.updateFilterCriteria = function () {
        updateFilterCriteriaInternal($scope.table.rows.filter(function (r) {
          return r.selected;
        }));
      };

      $scope.removeTerm = function(term) {
        var remainingCriteriaItems = vocabulariesTermsMap[term.vocabularyName].filter(function(t) {
            return t.entity.name !== term.entity.name;
          }).map(function(t) {
            return RqlQueryService.createCriteriaItem(QUERY_TARGETS.VARIABLE, t.taxonomyName, t.vocabularyName, t.entity.name);
          });
        
        $q.all(remainingCriteriaItems).then(function(criteriaItems) {
          $scope.onUpdateCriteria(mergeCriteriaItems(criteriaItems), null, true, false, false);
        });
      };

      $scope.removeVocabulary = function(vocabulary) {
        RqlQueryService.createCriteriaItem(QUERY_TARGETS.VARIABLE, vocabulary.taxonomyName, vocabulary.entity.name).then(function(item){
          $scope.onRemoveCriteria(item);
        });
      };
    }])

  .controller('GraphicsResultController', [
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    'RqlQueryService',
    '$filter',
    '$scope',
    function (GraphicChartsConfig,
              GraphicChartsUtils,
              RqlQueryService,
              $filter,
              $scope) {

      var setChartObject = function (vocabulary, dtoObject, header, title, options, isTable) {

        return GraphicChartsUtils.getArrayByAggregation(vocabulary, dtoObject)
          .then(function (entries){
            var data = entries.map(function (e) {
              if (e.participantsNbr && isTable) {
                return [e.title, e.value, e.participantsNbr];
              }
              else {
                return [e.title, e.value];
              }
            });

            if (data.length > 0) {
              data.unshift(header);
              angular.extend(options, {title: title});

              return {
                data: data,
                entries: entries,
                options: options,
                vocabulary: vocabulary
              };
            }
          });

      };

      var charOptions = GraphicChartsConfig.getOptions().ChartsOptions;

      $scope.updateCriteria = function (key, vocabulary) {
        RqlQueryService.createCriteriaItem('study', 'Mica_study', vocabulary, key).then(function (item) {
          $scope.onUpdateCriteria(item, 'studies');
        });
      };

      $scope.$watch('result', function (result) {
        $scope.chartObjects = {};
        $scope.noResults = true;

        if (result && result.studyResultDto.totalHits) {
          $scope.noResults = false;
          setChartObject('populations-selectionCriteria-countriesIso',
            result.studyResultDto,
            [$filter('translate')(charOptions.geoChartOptions.header[0]), $filter('translate')(charOptions.geoChartOptions.header[1])],
            $filter('translate')(charOptions.geoChartOptions.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.geoChartOptions.options).then(function(geoStudies) {
              if (geoStudies) {
                var chartObject = {
                  geoChartOptions: {
                    directiveTitle: geoStudies.options.title,
                    headerTitle: $filter('translate')('graphics.geo-charts'),
                    chartObject: {
                      geoTitle: geoStudies.options.title,
                      options: geoStudies.options,
                      type: 'GeoChart',
                      vocabulary: geoStudies.vocabulary,
                      data: geoStudies.data,
                      entries: geoStudies.entries
                    }
                  }
                };
                chartObject.geoChartOptions.getTable= function(){
                  return chartObject.geoChartOptions.chartObject;
                };
                angular.extend($scope.chartObjects, chartObject);
              }
            });
          // Study design chart.
          setChartObject('methods-designs',
            result.studyResultDto,
            [$filter('translate')(charOptions.studiesDesigns.header[0]),
              $filter('translate')(charOptions.studiesDesigns.header[1])
              ],
            $filter('translate')(charOptions.studiesDesigns.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.studiesDesigns.options).then(function(methodDesignStudies) {
              if (methodDesignStudies) {
                var chartObject= {
                  studiesDesigns: {
                    //directiveTitle: methodDesignStudies.options.title ,
                    headerTitle: $filter('translate')('graphics.study-design'),
                    chartObject: {
                      options: methodDesignStudies.options,
                      type: 'BarChart',
                      data: methodDesignStudies.data,
                      vocabulary: methodDesignStudies.vocabulary,
                      entries: methodDesignStudies.entries
                    }
                  }
                };
                angular.extend($scope.chartObjects, chartObject);
              }
            });

          // Study design table.
          setChartObject('methods-designs',
            result.studyResultDto,
            [$filter('translate')(charOptions.studiesDesigns.header[0]),
              $filter('translate')(charOptions.studiesDesigns.header[1]),
              $filter('translate')(charOptions.studiesDesigns.header[2])
            ],
            $filter('translate')(charOptions.studiesDesigns.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.studiesDesigns.options, true).then(function(methodDesignStudies) {
            if (methodDesignStudies) {
              var chartObject = {
                  chartObjectTable: {
                    options: methodDesignStudies.options,
                    type: 'BarChart',
                    data: methodDesignStudies.data,
                    vocabulary: methodDesignStudies.vocabulary,
                    entries: methodDesignStudies.entries
                  }

              };
              chartObject.getTable= function(){
                return chartObject.chartObjectTable;
              };
              angular.extend($scope.chartObjects.studiesDesigns, chartObject);
            }
          });

          setChartObject('numberOfParticipants-participant-range',
            result.studyResultDto,
            [$filter('translate')(charOptions.numberParticipants.header[0]), $filter('translate')(charOptions.numberParticipants.header[1])],
            $filter('translate')(charOptions.numberParticipants.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.numberParticipants.options).then(function(numberParticipant) {
              if (numberParticipant) {
                var chartObject = {
                  numberParticipants: {
                    headerTitle: $filter('translate')('graphics.number-participants'),
                    chartObject: {
                      options: numberParticipant.options,
                      type: 'PieChart',
                      data: numberParticipant.data,
                      vocabulary: numberParticipant.vocabulary,
                      entries: numberParticipant.entries
                    }
                  }
                };
                chartObject.numberParticipants.getTable= function(){
                  return chartObject.numberParticipants.chartObject;
                };
                angular.extend($scope.chartObjects, chartObject);
              }
            });

          setChartObject('populations-dataCollectionEvents-bioSamples',
            result.studyResultDto,
            [$filter('translate')(charOptions.biologicalSamples.header[0]), $filter('translate')(charOptions.biologicalSamples.header[1])],
            $filter('translate')(charOptions.biologicalSamples.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.biologicalSamples.options).then(function(bioSamplesStudies) {
              if (bioSamplesStudies) {
                var chartObject = {
                  biologicalSamples: {
                    headerTitle: $filter('translate')('graphics.bio-samples'),
                    chartObject: {
                      options: bioSamplesStudies.options,
                      type: 'BarChart',
                      data: bioSamplesStudies.data,
                      vocabulary: bioSamplesStudies.vocabulary,
                      entries: bioSamplesStudies.entries
                    }
                  }
                };
                chartObject.biologicalSamples.getTable= function(){
                  return chartObject.biologicalSamples.chartObject;
                };
                angular.extend($scope.chartObjects, chartObject);
              }
            });
        }
      });
    }])

  .controller('SearchResultPaginationController', ['$scope', function ($scope) {

    function updateMaxSize() {
      $scope.maxSize = Math.min(3, Math.ceil($scope.totalHits / $scope.pagination.selected.value));
    }

    function calculateRange() {
      var pageSize = $scope.pagination.selected.value;
      var current = $scope.pagination.currentPage;
      $scope.pagination.from = pageSize * (current - 1) + 1;
      $scope.pagination.to = Math.min($scope.totalHits, pageSize * current);
    }

    var pageChanged = function () {
      calculateRange();
      if ($scope.onChange) {
        $scope.onChange(
          $scope.target,
          ($scope.pagination.currentPage - 1) * $scope.pagination.selected.value,
          $scope.pagination.selected.value
        );
      }
    };

    var pageSizeChanged = function () {
      updateMaxSize();
      $scope.pagination.currentPage = 1;
      pageChanged();
    };

    $scope.pageChanged = pageChanged;
    $scope.pageSizeChanged = pageSizeChanged;
    $scope.pageSizes = [
      {label: '10', value: 10},
      {label: '20', value: 20},
      {label: '50', value: 50},
      {label: '100', value: 100}
    ];

    $scope.pagination = {
      selected: $scope.pageSizes[0],
      currentPage: 1
    };

    $scope.$watch('totalHits', function () {
      updateMaxSize();
      calculateRange();
    });
  }]);

