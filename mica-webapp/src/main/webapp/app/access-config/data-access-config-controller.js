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
    'DataAccessConfigResource',
    'DataAccessFormResource',
    'DataAccessFormPublicationResource',
    'EntitySchemaFormService',
    'DataAccessFormPermissionsResource',
    'LocalizedSchemaFormService',
    'AlertBuilder',
    'DataAccessFeasibilityFormResource',
    'DataAccessFeasibilityFormPublicationResource',
    'DataAccessAgreementFormResource',
    'DataAccessAgreementFormPublicationResource',
    'DataAccessAmendmentFormResource',
    'DataAccessAmendmentFormPublicationResource',
    function ($rootScope,
              $q,
              $location,
              $scope,
              $log,
              MicaConfigResource,
              DataAccessConfigResource,
              DataAccessFormResource,
              DataAccessFormPublicationResource,
              EntitySchemaFormService,
              DataAccessFormPermissionsResource,
              LocalizedSchemaFormService,
              AlertBuilder,
              DataAccessFeasibilityFormResource,
              DataAccessFeasibilityFormPublicationResource,
              DataAccessAgreementFormResource,
              DataAccessAgreementFormPublicationResource,
              DataAccessAmendmentFormResource,
              DataAccessAmendmentFormPublicationResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.formState = new mica.commons.EntityState($q, $scope);
      $scope.feasibilityState = new mica.commons.EntityState($q, $scope);
      $scope.agreementState = new mica.commons.EntityState($q, $scope);
      $scope.amendmentState = new mica.commons.EntityState($q, $scope);
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
        switch (EntitySchemaFormService.isFormValid($scope.form)) {
          case EntitySchemaFormService.ParseResult.VALID:
            entitySchemaFormSanitizeToSave('dataAccessForm', 'form');

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

            DataAccessFormResource.save($scope.dataAccessForm).$promise
              .then(function () {
                $scope.formState.setDirty(false);
                AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.data-access-form-success').build();
                refreshDataAccessForms();
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

      var publishForm = function() {
        DataAccessFormPublicationResource.publish().$promise
          .then(function () {
            AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.publish-alert.data-access-form-success').build();
            refreshDataAccessForms();
          }, function (reason) {
            AlertBuilder.newBuilder().response(reason).build();
          });
      };

      var saveFeasibilityForm = function() {
        switch (EntitySchemaFormService.isFormValid($scope.formFeasibility)) {
          case EntitySchemaFormService.ParseResult.VALID:
            entitySchemaFormSanitizeToSave('feasibilityForm', 'feasibilityForm');
            entitySchemaFormDelete('feasibilityForm');

            DataAccessFeasibilityFormResource.save($scope.feasibilityForm).$promise
              .then(function () {
                $scope.feasibilityState.setDirty(false);
                AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.data-access-feasibility-form-success').build();
                refreshDataAccessFeasibilityForms();
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

      var publishFeasibilityForm = function() {
        DataAccessFeasibilityFormPublicationResource.publish().$promise
          .then(function () {
            AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.publish-alert.data-access-feasibility-form-success').build();
            refreshDataAccessFeasibilityForms();
          }, function (reason) {
            AlertBuilder.newBuilder().response(reason).build();
          });
      };

      var saveAgreementForm = function() {
        switch (EntitySchemaFormService.isFormValid($scope.formAgreement)) {
          case EntitySchemaFormService.ParseResult.VALID:
            entitySchemaFormSanitizeToSave('agreementForm', 'agreementForm');
            entitySchemaFormDelete('agreementForm');

            DataAccessAgreementFormResource.save($scope.agreementForm).$promise
              .then(function () {
                $scope.agreementState.setDirty(false);
                AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.data-access-agreement-form-success').build();
                refreshDataAccessAgreementForms();
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

      var publishAgreementForm = function() {
        DataAccessAgreementFormPublicationResource.publish().$promise
          .then(function () {
            AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.publish-alert.data-access-agreement-form-success').build();
            refreshDataAccessAgreementForms();
          }, function (reason) {
            AlertBuilder.newBuilder().response(reason).build();
          });
      };

      var saveAmendmentForm = function() {
        switch (EntitySchemaFormService.isFormValid($scope.formAmendment)) {
          case EntitySchemaFormService.ParseResult.VALID:
            entitySchemaFormSanitizeToSave('amendmentForm', 'amendmentForm');
            entitySchemaFormDelete('amendmentForm');

            DataAccessAmendmentFormResource.save($scope.amendmentForm).$promise
              .then(function () {
                $scope.amendmentState.setDirty(false);
                AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.data-access-amendment-form-success').build();
                refreshDataAccessAmendmentForms();
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

      var publishAmendmentForm = function() {
        DataAccessAmendmentFormPublicationResource.publish().$promise
          .then(function () {
            AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.publish-alert.data-access-amendment-form-success').build();
            refreshDataAccessAmendmentForms();
          }, function (reason) {
            AlertBuilder.newBuilder().response(reason).build();
          });
      };

      var saveConfig = function() {
        DataAccessConfigResource.save($scope.config).$promise
          .then(function () {
            AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.data-access-config-success').build();
          }, function (reason) {
            AlertBuilder.newBuilder().response(reason).build();
          });
      };

      $scope.dataAccessForm = {schema: '', definition: '', pdfTemplates: []};
      $scope.feasibilityForm = {schema: '', definition: ''};
      $scope.agreementForm = {schema: '', definition: ''};
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

      function startWatchForDirty(fieldName, formState) {
        const watchState = {firstTime: true};
        const unwatch = $scope.$watch(fieldName, function(newValue, oldValue){
          if (!watchState.firstTime && diff(newValue, oldValue)) {
            formState.setDirty(true);
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

      function entitySchemaFormError(response){
        AlertBuilder.newBuilder().response(response).build();
      }

      function onUpdateActionKeys(keys) {
        if (keys && keys.length > 0) {
          $scope.dataAccessForm.predefinedActions = angular.copy (keys);
        } else {
          delete $scope.dataAccessForm.predefinedActions;
        }
      }

      $scope.config = DataAccessConfigResource.get();

      const refreshDataAccessForms = function(setUpWatch) {
        $scope.form = DataAccessFormResource.get({revision: 'draft'},
          function(dataAccessForm){
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

            if (setUpWatch) {
              startWatchForDirty('dataAccessForm', $scope.formState);
            }
          }, entitySchemaFormError);

        DataAccessFormResource.get({revision: 'latest'},
          function(dataAccessForm) {
            $scope.dataAccessFormLatestDate = dataAccessForm.lastUpdateDate;
          }
        );
      };
      refreshDataAccessForms(true);

      const refreshDataAccessFeasibilityForms = function(setUpWatch) {
        $scope.formFeasibility = DataAccessFeasibilityFormResource.get({revision: 'draft'},
          function(feasibilityForm){
            $scope.feasibilityForm = entitySchemaFormSanitize(feasibilityForm);

            if (feasibilityForm.definitionJson.length === 0) {
              AlertBuilder.newBuilder().trMsg('data-access-config-feasibility.parse-error.schema').build();
            }
            if (Object.getOwnPropertyNames(feasibilityForm.schemaJson).length === 0) {
              AlertBuilder.newBuilder().trMsg('data-access-config-feasibility.parse-error.definition').build();
            }

            if (setUpWatch) {
              startWatchForDirty('feasibilityForm', $scope.feasibilityState);
            }
          }, entitySchemaFormError);

        DataAccessFeasibilityFormResource.get({revision: 'latest'},
          function(feasibilityForm) {
            $scope.dataAccessFeasibilityFormLatestDate = feasibilityForm.lastUpdateDate;
          }
        );
      };
      refreshDataAccessFeasibilityForms(true);

      const refreshDataAccessAgreementForms = function(setUpWatch) {
        $scope.formAgreement = DataAccessAgreementFormResource.get({revision: 'draft'},
          function(agreementForm){
            $scope.agreementForm = entitySchemaFormSanitize(agreementForm);

            if (agreementForm.definitionJson.length === 0) {
              AlertBuilder.newBuilder().trMsg('data-access-config-agreement.parse-error.schema').build();
            }
            if (Object.getOwnPropertyNames(agreementForm.schemaJson).length === 0) {
              AlertBuilder.newBuilder().trMsg('data-access-config-agreement.parse-error.definition').build();
            }

            if (setUpWatch) {
              startWatchForDirty('agreementForm', $scope.agreementState);
            }
          }, entitySchemaFormError);

        DataAccessAgreementFormResource.get({revision: 'latest'},
          function(agreementForm) {
            $scope.dataAccessAgreementFormLatestDate = agreementForm.lastUpdateDate;
          }
        );
      };
      refreshDataAccessAgreementForms(true);

      const refreshDataAccessAmendmentForms = function(setUpWatch) {
        $scope.formAmendment = DataAccessAmendmentFormResource.get({revision: 'draft'},
          function(amendmentForm){
            $scope.amendmentForm = entitySchemaFormSanitize(amendmentForm);

            if (amendmentForm.definitionJson.length === 0) {
              AlertBuilder.newBuilder().trMsg('data-access-config-amendment.parse-error.schema').build();
            }
            if (Object.getOwnPropertyNames(amendmentForm.schemaJson).length === 0) {
              AlertBuilder.newBuilder().trMsg('data-access-config-amendment.parse-error.definition').build();
            }

            if (setUpWatch) {
              startWatchForDirty('amendmentForm', $scope.amendmentState);
            }
          }, entitySchemaFormError);

        DataAccessAmendmentFormResource.get({revision: 'latest'},
          function(amendmentForm) {
            $scope.dataAccessAmendmentFormLatestDate = amendmentForm.lastUpdateDate;
          }
        );
      };
      refreshDataAccessAmendmentForms(true);

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
      $scope.publishForm = publishForm;
      $scope.saveFeasibilityForm = saveFeasibilityForm;
      $scope.publishFeasibilityForm = publishFeasibilityForm;
      $scope.saveAgreementForm = saveAgreementForm;
      $scope.publishAgreementForm = publishAgreementForm;
      $scope.saveAmendmentForm = saveAmendmentForm;
      $scope.publishAmendmentForm = publishAmendmentForm;
      $scope.saveConfig = saveConfig;
    }]);
