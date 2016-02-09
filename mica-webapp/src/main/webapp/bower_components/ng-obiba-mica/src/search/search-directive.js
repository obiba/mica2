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

/* global RQL_NODE */

/* exported CRITERIA_ITEM_EVENT */
var CRITERIA_ITEM_EVENT = {
  selected: 'event:select-criteria-item',
  deleted: 'event:delete-criteria-item'
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
        taxonomy: '=',
        vocabulary: '=',
        lang: '=',
        onNavigate: '='
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

  .directive('networksResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/networks-search-result-table-template.html'
    };
  }])

  .directive('datasetsResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/datasets-search-result-table-template.html'
    };
  }])

  .directive('studiesResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/studies-search-result-table-template.html'
    };
  }])

  .directive('variablesResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/variables-search-result-table-template.html'
    };
  }])

  .directive('coverageResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        result: '=',
        loading: '='
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
        loading: '='
      },
      controller: 'GraphicsResultController',
      templateUrl: 'search/views/graphics/graphics-search-result-template.html'
    };
  }])

  .directive('resultPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        type: '=',
        display: '=',
        dto: '=',
        lang: '=',
        loading: '=',
        onTypeChanged: '='
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
        onRemove: '=',
        onSelect: '='
      },
      template: '<span ng-repeat="child in item.children"><criteria-target item="child"></criteria-target></span>',
      link: function(scope) {
        scope.$on(CRITERIA_ITEM_EVENT.deleted, function(event, item){
          scope.onRemove(item);
        });

        scope.$on(CRITERIA_ITEM_EVENT.selected, function(){
          scope.onSelect();
        });
      }
    };
  }])

  .directive('criteriaTarget', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '='
      },
      template: '<span ng-repeat="child in item.children" ><criteria-node item="child"></criteria-node></span>',
      link: function(scope) {
        console.log('criteriaTarget', scope.item);
      }
    };
  }])

  .directive('criteriaNode', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '='
      },
      templateUrl: 'search/views/criteria-node-template.html',
      link: function(scope) {
        console.log('criteriaNode', scope.item);
      }
    };
  }])

  /**
   * This directive is responsible to build the proper type of drop-down leaf
   *
   * TODO needs more specialization
   */
  .directive('criteriaLeaf', ['$compile',
    function($compile){
      return {
        restrict: 'EA',
        replace: true,
        scope: {
          item: '=',
          parentType: '='
        },
        template: '<span></span>',
        link: function(scope, element) {
          console.log('criteriaLeaf', scope);

          var template = '';
          if (scope.item.type === RQL_NODE.OR) {
            template = '<criteria-node item="item"></criteria-node>';
            $compile(template)(scope, function(cloned){
              element.append(cloned);
            });
          } else {
            template = '<span criterion-dropdown criterion="item"></span>';
            $compile(template)(scope, function(cloned){
              element.append(cloned);
            });
          }
        }
      };
    }])

  .directive('criterionDropdown', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '='
      },
      controller: 'CriterionDropdownController',
      templateUrl: 'search/views/criterion-dropdown-template.html'

    };
  }])

  .directive('criteriaPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criteria: '=',
        query: '='
      },
      controller: 'CriteriaPanelController',
      templateUrl: 'search/views/criteria-panel-template.html'
    };
  }]);
