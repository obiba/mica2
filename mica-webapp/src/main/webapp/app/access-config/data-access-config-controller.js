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

mica.dataAccessConfig

  .controller('DataAccessConfigController', ['$rootScope',
    '$q',
    '$location',
    '$scope',
    '$log',
    'MicaConfigResource',
    'DataAccessFormResource',
    'EntitySchemaFormService',
    'DataAccessFormPermissionsResource',
    'LocalizedSchemaFormService',
    'AlertBuilder',
    'DataAccessFeasibilityFormResource',
    'DataAccessAmendmentFormResource',
    function ($rootScope,
              $q,
              $location,
              $scope,
              $log,
              MicaConfigResource,
              DataAccessFormResource,
              EntitySchemaFormService,
              DataAccessFormPermissionsResource,
              LocalizedSchemaFormService,
              AlertBuilder,
              DataAccessFeasibilityFormResource,
              DataAccessAmendmentFormResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.state = new mica.commons.EntityState($q, $scope);
      $scope.pdfTemplates = {};

      function entitySchemaFormSanitizeToSave(entitySchemaForm, form){
        $scope[entitySchemaForm].definition = $scope[form].definition;
        $scope[entitySchemaForm].schema = $scope[form].schema;
      }

      function entitySchemaFormDelete(entitySchemaForm){
        delete $scope[entitySchemaForm].definitionJson;
        delete $scope[entitySchemaForm].schemaJson;
        delete $scope[entitySchemaForm].model;
      }

      var saveForm = function() {
        switch (EntitySchemaFormService.isFormValid($scope.form) && EntitySchemaFormService.isFormValid($scope.formFeasibility) && EntitySchemaFormService.isFormValid($scope.formAmendment)) {
          case EntitySchemaFormService.ParseResult.VALID:
            entitySchemaFormSanitizeToSave('dataAccessForm', 'form');
            entitySchemaFormSanitizeToSave('feasibilityForm', 'feasibilityForm');
            entitySchemaFormSanitizeToSave('amendmentForm', 'amendmentForm');

            $scope.dataAccessForm.pdfTemplates = [];

            for (var lang in $scope.pdfTemplates) {
              if ($scope.pdfTemplates) {
                for (var i = $scope.pdfTemplates[lang].length; i--;) {
                  $scope.pdfTemplates[lang][i].lang = lang;
                }

                $scope.dataAccessForm.pdfTemplates = $scope.dataAccessForm.pdfTemplates.concat($scope.pdfTemplates[lang]);
              }
            }

            entitySchemaFormDelete('dataAccessForm');
            entitySchemaFormDelete('feasibilityForm');
            entitySchemaFormDelete('amendmentForm');

            $q.all([DataAccessFormResource.save($scope.dataAccessForm).$promise, DataAccessFeasibilityFormResource.save($scope.feasibilityForm).$promise, DataAccessAmendmentFormResource.save($scope.amendmentForm).$promise]).then(function () {
              $scope.state.setDirty(false);
              AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.success').build();
            }, function (reason) {
              AlertBuilder.newBuilder().response(reason).build();
            });

            break;
          case EntitySchemaFormService.ParseResult.SCHEMA:
            AlertBuilder.newBuilder().trMsg('data-access-config.syntax-error.schema').build();
            break;
          case EntitySchemaFormService.ParseResult.DEFINITION:
            AlertBuilder.newBuilder().trMsg('data-access-config.syntax-error.definition').build();
            break;
          }
      };

      $scope.dataAccessForm = {schema: '', definition: '', pdfTemplates: []};
      $scope.feasibilityForm = {schema: '', definition: ''};
      $scope.amendmentForm = {schema: '', definition: ''};
      $scope.fileTypes = '.pdf';

      function isUndefinedOrNull(a) { return angular.isUndefined(a) || a === null; }

      // return true if different and false otherwise
      function diff(a, b) {
        if (isUndefinedOrNull(a) && isUndefinedOrNull(b)) { return false; }
        if ((!a && b) || (a && !b)) { return true; }

        var omit = ['model', 'schemaJson', 'definitionJson'];
        var aKeys = Object.keys(a).filter(function (k) { return k.indexOf('$') === -1 && omit.indexOf(k) === -1; }),
            bKeys = Object.keys(b).filter(function (k) { return k.indexOf('$') === -1 && omit.indexOf(k) === -1; });

        if (aKeys.length !== bKeys.length) { return true; }

        var check = aKeys.reduce(function (acc, curr) {
          if (typeof a[curr] !== typeof b[curr]) {
            return acc.concat(curr);
          }

          // both are objects (array and object are both considered objects)
          if (typeof a[curr] === 'object' && typeof b[curr] === 'object') {
            return !diff(a[curr], b[curr]) ? acc : acc.concat(curr);
          }

          return a[curr] === b[curr] ? acc : acc.concat(curr);
        }, []);

        return check.length > 0;
      }

      function startWatchForDirty(fieldName, watchState) {
        var unwatch = $scope.$watch(fieldName, function(newValue, oldValue){
          if (!watchState.firstTime && diff(newValue, oldValue)) {
            $scope.state.setDirty(true);
            unwatch();
          }
          watchState.firstTime = false;
        },true);

      }

      function entitySchemaFormSanitize(SchemaFormEntity){
        SchemaFormEntity.model = {};
        SchemaFormEntity.definitionJson = EntitySchemaFormService.parseJsonSafely(SchemaFormEntity.definition, []);
        SchemaFormEntity.definition = EntitySchemaFormService.prettifyJson(SchemaFormEntity.definitionJson);
        SchemaFormEntity.schemaJson = EntitySchemaFormService.parseJsonSafely(SchemaFormEntity.schema, {});
        SchemaFormEntity.schema = EntitySchemaFormService.prettifyJson(SchemaFormEntity.schemaJson);
        return  SchemaFormEntity;
      }

      function entitySchemaFormaError(response){
        AlertBuilder.newBuilder().response(response).build();
      }

      function onUpdateActionKeys(keys) {
        if (keys && keys.length > 0) {
          $scope.dataAccessForm.predefinedActions = angular.copy (keys);
        } else {
          delete $scope.dataAccessForm.predefinedActions;
        }
      }

      $scope.form = DataAccessFormResource.get(
        function(dataAccessForm){
          var watchState = {firstTime: true};
          $scope.dataAccessForm = entitySchemaFormSanitize(dataAccessForm);
          $scope.dataAccessForm.pdfTemplates = $scope.dataAccessForm.pdfTemplates || [];

          $scope.pdfTemplates = $scope.dataAccessForm.pdfTemplates.reduce(function(s, file) {
            s[file.lang] = [file];
            return s;
          }, {});

          if (dataAccessForm.definitionJson.length === 0) {
            AlertBuilder.newBuilder().trMsg('data-access-config.parse-error.schema').build();
          }
          if (Object.getOwnPropertyNames(dataAccessForm.schemaJson).length === 0) {
            AlertBuilder.newBuilder().trMsg('data-access-config.parse-error.definition').build();
          }

          startWatchForDirty('dataAccessForm', watchState);
          startWatchForDirty('pdfTemplates', watchState);
        },entitySchemaFormaError);

      $scope.formFeasibility = DataAccessFeasibilityFormResource.get(function(feasibilityForm){
        var watchState = {firstTime: true};
        $scope.feasibilityForm = entitySchemaFormSanitize(feasibilityForm);

        if (feasibilityForm.definitionJson.length === 0) {
          AlertBuilder.newBuilder().trMsg('data-access-config-feasibility.parse-error.schema').build();
        }
        if (Object.getOwnPropertyNames(feasibilityForm.schemaJson).length === 0) {
          AlertBuilder.newBuilder().trMsg('data-access-config-feasibility.parse-error.definition').build();
        }

        startWatchForDirty('feasibilityForm', watchState);

      },entitySchemaFormaError);

      $scope.formAmendment = DataAccessAmendmentFormResource.get(function(amendmentForm){
        var watchState = {firstTime: true};
        $scope.amendmentForm = entitySchemaFormSanitize(amendmentForm);

        if (amendmentForm.definitionJson.length === 0) {
          AlertBuilder.newBuilder().trMsg('data-access-config-amendment.parse-error.schema').build();
        }
        if (Object.getOwnPropertyNames(amendmentForm.schemaJson).length === 0) {
          AlertBuilder.newBuilder().trMsg('data-access-config-amendment.parse-error.definition').build();
        }

        startWatchForDirty('amendmentForm', watchState);

      },entitySchemaFormaError);

      $scope.onUpdateActionKeys = onUpdateActionKeys;
      $scope.loadPermissions = function() {
        $scope.acls = DataAccessFormPermissionsResource.get();
        return $scope.acls;
      };

      $scope.addPermission = function (acl) {
        return DataAccessFormPermissionsResource.save(acl);
      };

      $scope.deletePermission = function (acl) {
        return DataAccessFormPermissionsResource.delete(acl);
      };

      $scope.darRoles = ['READER'];

      $scope.otherResources = [{
        value: 'action-logs',
        text: 'data-access-config.action-log.action-logs-permission'
      },
      {
        value: 'private-comment',
        text: 'data-access-config.private-comment-permission'
      }];

      $scope.loadPermissions();
      $scope.tab = {name: 'form'};
      $scope.saveForm = saveForm;
    }]);
