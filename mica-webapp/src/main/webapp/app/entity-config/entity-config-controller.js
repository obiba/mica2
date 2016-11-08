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


mica.entityConfig.EntityState = function($q) {
  var listeners = [];

  this.registerListener = function (listener) {
    if (listeners.indexOf(listener) < 0) {
      if (typeof listener.onSave === 'function') {
        listeners.push(listener);
      } else {
        throw new Error('EntityState - listener must define onSave() method.');
      }
    }
  };

  this.onSave = function () {
    return listeners.map(function (listener) {
      var defer = $q.defer();
      listener.onSave().$promise.then(
        function() {
          defer.resolve();
        }, function(response) {
          defer.reject(response);
        });
    });
  };

  return this;
};

mica.entityConfig

  .controller('EntityConfigController', [
    '$scope',
    '$q',
    '$location',
    '$routeParams',
    'AlertService',
    'ServerErrorUtils',
    'EntitySchemaFormService',
    'EntityFormCustomResource',
    'EntityFormPermissionsResource',
    'MicaConfigResource',
    'EntityFormAccessesResource',
    function ($scope, $q, $location, $routeParams, AlertService, ServerErrorUtils,
              EntitySchemaFormService, EntityFormCustomResource, EntityFormPermissionsResource, MicaConfigResource,
              EntityFormAccessesResource) {
      var FORMS = {'network': ['network'],
        'study': ['study', 'population', 'data-collection-event'],
        'dataset': ['dataset']};

      $scope.state = new mica.entityConfig.EntityState($q);
      $scope.target = $routeParams.type.match(/(\w+)\-config/)[1];
      $scope.taxonomyTargets = $scope.target === 'dataset' ? [$scope.target, 'variable'] : [$scope.target];
      $scope.tab = {name: 'form'};
      $scope.forms = [];
      $scope.permissions = [];
      $scope.accesses = [];
      MicaConfigResource.get(function (micaConfig) {
        $scope.roles = micaConfig.roles;
        $scope.openAccess = micaConfig.openAccess;
      });

      $scope.loadPermissions = function () {
        $scope.permissions = EntityFormPermissionsResource.query({target: $scope.target});
        return $scope.permissions;
      };

      $scope.deletePermission = function (permission) {
        return EntityFormPermissionsResource.delete({target: $scope.target}, permission);
      };

      $scope.addPermission = function (permission) {
        return EntityFormPermissionsResource.save({target: $scope.target}, permission);
      };

      $scope.loadAccesses = function () {
        $scope.accesses = EntityFormAccessesResource.query({target: $scope.target});
        return $scope.accesses;
      };

      $scope.deleteAccess = function (access) {
        return EntityFormAccessesResource.delete({target: $scope.target}, access);
      };

      $scope.addAccess = function (access) {
        return EntityFormAccessesResource.save({target: $scope.target}, access);
      };

      $scope.forms = FORMS[$scope.target].map(function(name) {
        return {name: name, form: EntityFormCustomResource.get({target: name},
          function(entityForm){
            entityForm.definitionJson = EntitySchemaFormService.parseJsonSafely(entityForm.definition, []);
            entityForm.definition = EntitySchemaFormService.prettifyJson(entityForm.definitionJson);
            entityForm.schemaJson = EntitySchemaFormService.parseJsonSafely(entityForm.schema, {});
            entityForm.schema = EntitySchemaFormService.prettifyJson(entityForm.schemaJson);
            entityForm.model = {};
            return entityForm;
          },
          function(response) {
            AlertService.alert({
              id: 'EntityConfigController',
              type: 'danger',
              msg: ServerErrorUtils.buildMessage(response)
            });
          })};
      });

      $scope.forms[0].active = true;

      $scope.setActive = function(form) {
        $scope.forms.forEach(function(f) {
          f.active = f.name === form.name;
        });
      };

      $scope.saveForm = function() {
        var res = $scope.state.onSave();

        $q.all(res).then(function() {
          res = $scope.forms.map(function(form) {
            var defer = $q.defer();
            switch (EntitySchemaFormService.isFormValid(form.form)) {
              case EntitySchemaFormService.ParseResult.VALID:
                delete form.form.definitionJson;
                delete form.form.schemaJson;
                delete form.form.model;
                EntityFormCustomResource.save({target: form.name}, form.form, function() {
                  defer.resolve();
                }, function(response) {
                  defer.reject(ServerErrorUtils.buildMessage(response));
                });
                break;
              case EntitySchemaFormService.ParseResult.SCHEMA:
                defer.reject('entity-config.syntax-error.schema');
                break;
              case EntitySchemaFormService.ParseResult.DEFINITION:
                defer.reject('entity-config.syntax-error.definition');
            }

            return defer.promise;
          });

          $q.all(res).then(function (res) {
            $location.path('/admin').replace();
            return res;
          }).catch(function (res) {
            AlertService.alert({
              id: 'EntityConfigController',
              type: 'danger',
              msg: res
            });
          });
        }).catch(function (res) {
          AlertService.alert({
            id: 'EntityConfigController',
            type: 'danger',
            msg: res
          });
        });

      };
    }]);
