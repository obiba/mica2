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

mica.entityConfig

  .controller('EntityConfigController', [
    '$scope',
    '$q',
    '$location',
    '$routeParams',
    'ServerErrorUtils',
    'EntitySchemaFormService',
    'EntityFormCustomResource',
    'EntityFormPermissionsResource',
    'MicaConfigResource',
    'EntityFormAccessesResource',
    'AlertBuilder',
    '$cacheFactory',
    function ($scope,
              $q,
              $location,
              $routeParams,
              ServerErrorUtils,
              EntitySchemaFormService,
              EntityFormCustomResource,
              EntityFormPermissionsResource,
              MicaConfigResource,
              EntityFormAccessesResource,
              AlertBuilder,
              $cacheFactory) {
      var FORMS = {'network': ['network'],
        'individual-study': ['individual-study', 'population', 'data-collection-event'],
        'collected-dataset': ['collected-dataset'],
        'harmonized-dataset': ['harmonized-dataset'],
        'harmonization-study': ['harmonization-study', 'harmonization-population']
      };

      function mapTargetTypeToId(type) {
        switch (type) {
          case 'HarmonizationStudy':
          case 'Study':
            return 'studies';
          case 'HarmonizationPopulation':
          case 'Population':
            return 'populations';
          case 'DataCollectionEvent':
            return 'dataCollectionEvents';
          case 'CollectedDataset':
          case 'HarmonizedDataset':
            return 'datasets';
          case 'Network':
            return 'Networks';
        }

        throw new Error('Invalid target type ', type);
      }

      function initializeScopeTargets() {
        $scope.target = $routeParams.type.match(/([\w-]+)\-config/)[1];
        var names = [$scope.target];

        if ($scope.target.endsWith('dataset')) {
          names = ['dataset', 'variable'];
        } else if ($scope.target.endsWith('study')) {
          names = ['study'];
        }

        $scope.taxonomyTargets = names.map(function(name){
          return {name: name, editable: true};
        });
      }

      initializeScopeTargets();
      $scope.state = new mica.commons.EntityState($q, $scope);
      $scope.tab = {name: 'form'};
      $scope.permissions = [];
      $scope.targetSchemas = [];
      $scope.accesses = [];
      $scope.customTitleInfo = { permissions:'permission.help-global', accesses:'access.help-global' };

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
            AlertBuilder.newBuilder().response(response).build();
          })};
      });

      $q.all($scope.forms.map(function(item){
        return item.form.$promise;
      })).then(function(forms) {
        $scope.targetSchemas = forms.map(function(entityForm){
          return {id: mapTargetTypeToId(entityForm.type), schema: entityForm.schemaJson};
        });
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
            AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.success').build();
            $scope.state.setDirty(false);
            var taxonomyResourceCache = $cacheFactory.get('taxonomyResource');
            if (taxonomyResourceCache) {
              taxonomyResourceCache.removeAll();
            }
            return res;
          }).catch(function (res) {
            AlertBuilder.newBuilder().trMsg(res).build();
          });
        }).catch(function (res) {
          var builder = AlertBuilder.newBuilder();
          if (res.data) {
            builder.trMsg(res.data.messageTemplate || res.data.message , res.data.arguments);
          } else {
            builder.trMsg(res).build();
          }
          builder.build();
        });

      };
    }]);
