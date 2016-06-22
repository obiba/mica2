angular.module("sfObibaFileUploadTemplates", []).run(["$templateCache", function($templateCache) {$templateCache.put("src/templates/sf-obiba-file-upload.html","<div class=\"form-group\"\n     ng-controller=\"sfObibaFileUploadController\"\n     ng-class=\"{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess(), \'has-feedback\': form.feedback !== false }\"\n     schema-validate=\"form\"\n     sf-field-model>\n  <label ng-if=\"!form.notitle\" class=\"control-label\" >{{form.title}}</label>\n\n  <div>\n    <attachment-input ng-if=\"!form.readonly\"\n                      sf-field-model=\"replaceAll\"\n                      files=\"$$value$$.obibaFiles\"\n                      multiple=\"form.schema.multiple\"></attachment-input>\n\n    <attachment-list ng-if=\"form.readonly\"\n                     empty-message=\"form.emptyMessage\"\n                     href-builder=\"getDownloadUrl\"\n                     files=\"$$value$$.obibaFiles\"\n                     sf-field-model=\"replaceAll\"></attachment-list>\n\n    <span class=\"help-block\" sf-message=\"form.helpvalue\"></span>\n  </div>\n</div>");}]);
/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('sfObibaFileUpload', [
  'schemaForm',
  'sfObibaFileUploadTemplates',
  'ngObibaMica'
])
  /**
   * Provider to be used by client applications to set form defaults
   */
  .provider('sfObibaFileUploadOptions', function() {
    var self = this;
    var options = {
      general: {
        emptyMessage: null
      },
      validationMessages: {
        missingFiles: null,
        minItems: null
      }
    };

    function setMessage(type, key, message) {
      if (options[type].hasOwnProperty(key)) {
        options[type][key] = message;
      }
    }

    self.getOptions = function () {
      return angular.copy(options);
    };

    /**
     * Sets the value of an existing option
     *
     * @param key
     * @param message - can be a key as well
     */
    self.setGeneralMessage = function (key, message) {
      setMessage('general', key, message);
    };

    /**
     * Sets the value of an existing option
     *
     * @param key
     * @param message - can be a key as well
     */
    self.setValidationMessage = function (key, message) {
      setMessage('validationMessages', key, message);
    };

    /**
     * Options Service returned by the provider
     *
     * @param options
     * @param LocaleStringUtils
     * @constructor
     */
    function OptionsService(options, LocaleStringUtils) {
      var self = this;
      self.options = options;
      self.tr = function(key, args) {
        return LocaleStringUtils.translate(key, args);
      };
    }

    /**
     * Returns the provider instance
     *
     * @param $filter
     */
    self.$get = ['LocaleStringUtils', function(LocaleStringUtils) {
      return new OptionsService(self.getOptions(), LocaleStringUtils);
    }];
  })

  .config(['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfPathProvider', 'sfBuilderProvider',
  function (schemaFormProvider, schemaFormDecoratorsProvider, sfPathProvider, sfBuilderProvider) {

    /**
     * OBiBa's Schema Form file upload decorator.
     * @param name
     * @param schema
     * @param options
     * @returns {*}
     */
    var obibaFileUpload = function (name, schema, options) {

      if (schema.type === 'object' && schema.format === 'obibaFiles') {
        var f = schemaFormProvider.stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'obibaFileUpload';
        f.$validators = {
          missingFiles: function (value) {
            if (value && options.required) {
              return value.obibaFiles && value.obibaFiles.length > 0;
            }
            return true;
          },
          minItems: function (value) {
            if (value && schema.minItems) {
              return value.obibaFiles && value.obibaFiles.length >= schema.minItems;
            }
            return true;
          }
        };

        options.lookup[sfPathProvider.stringify(options.path)] = f;
        return f;
      }
    };

    schemaFormProvider.defaults.object.unshift(obibaFileUpload);

    schemaFormDecoratorsProvider.defineAddOn(
      'bootstrapDecorator',           // Name of the decorator you want to add to.
      'obibaFileUpload',                      // Form type that should render this add-on
      'src/templates/sf-obiba-file-upload.html',  // Template name in $templateCache
      sfBuilderProvider.stdBuilders   // List of builder functions to apply.
    );

  }])
  .controller('sfObibaFileUploadController', [
    '$scope',
    'ngObibaMicaUrl',
    'sfObibaFileUploadOptions',
    function ($scope, ngObibaMicaUrl, sfObibaFileUploadOptions) {

      /**
       * Return the download url to the file.
       *
       * @param file
       * @returns {*}
       */
      $scope.getDownloadUrl = function(file) {
        return ngObibaMicaUrl.getUrl('SchemaFormAttachmentDownloadResource')
          .replace(':path', file.path)
          .replace(':attachmentName', file.fileName)
          .replace(':attachmentId', file.id)
          .replace('//', '/');
      };

      /**
       * Watch to setup form validation message. Both default and actual message must be defined in the host
       * application's translation files.
       */
      $scope.$watch('form', function() {
        var schema = $scope.form.schema;

        if (schema.minItems && schema.minItems > 1) {
          schema.multiple = true;
        }

        var options = sfObibaFileUploadOptions.options;
        $scope.form.disableErrorState = $scope.form.hasOwnProperty('readonly') && $scope.form.readonly;
        $scope.$on('schemaFormValidate', function() {
          $scope.form.disableErrorState = undefined; // remove flag so the invalid fields get highlighted
        });

        // setup messages
        if (!$scope.form.general) {
          $scope.form.general = {};
        }

        if (!$scope.form.emptyMessage) {
          $scope.form.emptyMessage = sfObibaFileUploadOptions.tr(options.general.emptyMessage);
        }

        if (!$scope.form.validationMessage) {
          $scope.form.validationMessage = {};
        }

        if (!$scope.form.validationMessage.missingFiles) {
          $scope.form.validationMessage.missingFiles =
            sfObibaFileUploadOptions.tr(options.validationMessages.missingFiles, null);
        }

        if (!$scope.form.validationMessage.minItems) {
          $scope.form.validationMessage.minItems =
            sfObibaFileUploadOptions.tr(
              options.validationMessages.minItems,
              [schema.minItems]
            );
        }
      });

      /**
       * Watch to make the form validationMessage visible.
       */
      $scope.$watch('ngModel.$modelValue', function() {
        if ($scope.ngModel.$validate) {
          // Make sure that allowInvalid is always true so that the model is preserved when validation fails
          $scope.ngModel.$options = $scope.ngModel.$options || {};
          $scope.ngModel.$options = {allowInvalid: true};

          $scope.ngModel.$validate();
          if ($scope.ngModel.$invalid) { // The field must be made dirty so the error message is displayed
            $scope.ngModel.$dirty = true;
            $scope.ngModel.$pristine = false;
          }
        }
        else {
          $scope.ngModel.$setViewValue(ngModel.$viewValue);
        }
      }, true);
    }]);
