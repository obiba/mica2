/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.entityTaxonomyConfig

  .directive('entityTaxonomyConfig', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        targets: '=',
        schemas: '=',
        state: '=',
        forClassName: '='
      },
      templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config.html'
    };
  }])

  .directive('entityTaxonomyConfigContent', [function(){
    return {
      restrict: 'EA',
      replace: true,
      controller: 'EntityTaxonomyConfigContentController',
      scope: {
        target: '=',
        schemas: '=',
        state: '=',
        forClassName: '='
      },
      templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-content.html'
    };
  }])

  .directive('entityTaxonomyConfigPropertiesView', [function(){
    return {
      restrict: 'EA',
      replace: true,
      controller: 'entityTaxonomyConfigPropertiesViewController',
      scope: false,
      templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-properties-view.html'
    };
  }])

  .directive('entityTaxonomyConfigContentView', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: false,
      templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-content-view.html'
    };
  }])

  .directive('entityTaxonomyConfigCriteriaView', [function(){
    return {
      restrict: 'EA',
      replace: true,
      controller: 'entityTaxonomyConfigCriteriaViewController',
      scope: false,
      templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-criteria-view.html'
    };
  }])

  .directive('entityTaxonomyConfigTermsView', [function(){
    return {
      restrict: 'EA',
      replace: true,
      controller: 'entityTaxonomyConfigTermsViewController',
      scope: false,
      templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-terms-view.html'
    };
  }])

  .directive('propertyView', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        property: '='
      },
      templateUrl: 'app/entity-taxonomy-config/views/property-view.html'
    };
  }]);
