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

            delete $scope.dataAccessForm.definitionJson;
            delete $scope.dataAccessForm.schemaJson;
            delete $scope.dataAccessForm.model;

            DataAccessFormResource.save($scope.dataAccessForm,
              function () {
                $scope.state.setDirty(false);
                AlertBuilder.newBuilder().delay(3000).type('success').trMsg('entity-config.save-alert.success').build();
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

      $scope.form = DataAccessFormResource.get(
        function(dataAccessForm){
          var watchState = {firstTime: true};
          dataAccessForm.model = {};
          dataAccessForm.definitionJson = EntitySchemaFormService.parseJsonSafely(dataAccessForm.definition, []);
          dataAccessForm.definition = EntitySchemaFormService.prettifyJson(dataAccessForm.definitionJson);
          dataAccessForm.schemaJson = EntitySchemaFormService.parseJsonSafely(dataAccessForm.schema, {});
          dataAccessForm.schema = EntitySchemaFormService.prettifyJson(dataAccessForm.schemaJson);
          $scope.dataAccessForm = dataAccessForm;
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
        },
        function(response) {
          AlertBuilder.newBuilder().response(response).build();
        });

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
