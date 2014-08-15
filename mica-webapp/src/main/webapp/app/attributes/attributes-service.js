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

mica.attributes.service('attributesService', [function () {

  this.groupByNamespace = function (scope) {
    if (!scope || !scope.attributes) {
      return;
    }

    var namespaces = {};
    $.each(scope.attributes, function (i, attribute) {
      if (!namespaces.hasOwnProperty(attribute.namespace)) {
        namespaces[attribute.namespace] = [];
      }

      namespaces[attribute.namespace].push({data: attribute, masterIndex: i});
    });

    scope.namespaces = namespaces;
  };

  this.addAttribute  = function (scope, attribute) {
    if (!scope) {
      return;
    }

    if (!scope.attributes) {
      scope.attributes = [];
    }

    scope.attributes.push(attribute);
    this.groupByNamespace(scope);
  };

  this.editAttribute  = function(scope, index, attribute) {
    if (!scope || !scope.attributes) {
      return;
    }

    scope.attributes[index] = attribute;
    this.groupByNamespace(scope);
  };

  this.deleteAttribute  = function (scope, index) {
    if (!scope || !scope.attributes) {
      return;
    }

    scope.attributes.splice(index, 1);
    this.groupByNamespace(scope);
  };

}]);


mica.attributes.service('attributeModalService', ['MicaConfigResource', function (MicaConfigResource) {

  this.normalizeLocales = function (scope) {
    if (!scope || !scope.attribute) {
      return;
    }

    MicaConfigResource.get(function (micaConfig) {
      // used for UI puroses only

      if (!scope.attribute.values) {
        // in case of a new attribute
        scope.values = [];
      } else {
        scope.values = scope.attribute.values.slice();
      }

      micaConfig.languages.forEach(function (lang){
          var found = false;
          scope.values.forEach(function(value){
            if (value.lang === lang) {
              found = true;
              return true;
            }
          });

          if (!found) {
            scope.values.push({lang: lang, value: undefined});
          }
      });
    });
  };

  this.validateLocales = function (scope) {
    if (!scope || !scope.values || scope.values.length === 0) {
      return;
    }

    var validValues = [];

    scope.values.forEach(function(locale){
      if (locale.value) {
        validValues.push(locale);
      }
    });

    scope.attribute.values = validValues.length > 0 ? validValues : undefined;
  };

}]);