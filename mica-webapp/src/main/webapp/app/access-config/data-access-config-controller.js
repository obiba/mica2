/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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
              AlertBuilder) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.state = new mica.commons.EntityState($q, $scope);
      $scope.pdfTemplates = {};

      var saveForm = function() {

        switch (EntitySchemaFormService.isFormValid($scope.form)) {
          case EntitySchemaFormService.ParseResult.VALID:
            $scope.dataAccessForm.definition = $scope.form.definition;
            $scope.dataAccessForm.schema = $scope.form.schema;
            $scope.dataAccessForm.pdfTemplates = [];

            for (var lang in $scope.pdfTemplates) {
              if ($scope.pdfTemplates) {
                for (var i = $scope.pdfTemplates[lang].length; i--;) {
                  $scope.pdfTemplates[lang][i].lang = lang;
                }

                $scope.dataAccessForm.pdfTemplates = $scope.dataAccessForm.pdfTemplates.concat($scope.pdfTemplates[lang]);
              }
            }

            DataAccessFormResource.save($scope.dataAccessForm,
              function () {
                $location.path('/admin').replace();
              },
              function (response) {
                AlertBuilder.newBuilder().response(response).build();
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
      $scope.fileTypes = '.pdf';

      function startWatchForDirty(fieldName, watchState) {
        var unwatch = $scope.$watch(fieldName, function(newValue, oldValue){
          if (!watchState.firstTime && newValue !== oldValue) {
            $scope.state.setDirty(true);
            unwatch();
          }
          watchState.firstTime = false;
        },true);

      }

      DataAccessFormResource.get(
        function(dataAccessForm){
          var watchState = {firstTime: true};
          $scope.form.definitionJson = EntitySchemaFormService.parseJsonSafely(dataAccessForm.definition, []);
          $scope.form.definition = EntitySchemaFormService.prettifyJson($scope.form.definitionJson);
          $scope.form.schemaJson = EntitySchemaFormService.parseJsonSafely(dataAccessForm.schema, {});
          $scope.form.schema = EntitySchemaFormService.prettifyJson($scope.form.schemaJson);
          $scope.dataAccessForm = dataAccessForm;
          $scope.dataAccessForm.pdfTemplates = $scope.dataAccessForm.pdfTemplates || [];

          $scope.pdfTemplates = $scope.dataAccessForm.pdfTemplates.reduce(function(s, file) {
            s[file.lang] = [file];
            return s;
          }, {});

          if ($scope.form.definitionJson.length === 0) {
            AlertBuilder.newBuilder().trMsg('data-access-config.parse-error.schema').build();
          }
          if (Object.getOwnPropertyNames($scope.form.schemaJson).length === 0) {
            AlertBuilder.newBuilder().trMsg('data-access-config.parse-error.definition').build();
          }

          startWatchForDirty('dataAccessForm', watchState);
          startWatchForDirty('pdfTemplates', watchState);
        },
        function(response) {
          AlertBuilder.newBuilder().response(response).build();
        });

      $scope.form = {
        definitionJson: null,
        definition: null,
        schemaJson: null,
        schema: null,
        model: {}
      };

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

      $scope.loadPermissions();
      $scope.tab = {name: 'form'};
      $scope.saveForm = saveForm;
    }]);
