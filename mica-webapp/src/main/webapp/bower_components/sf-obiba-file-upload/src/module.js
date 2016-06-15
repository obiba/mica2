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
    var options = {
      validationMessages: { missingFiles: null}
    };

    this.getOptions = function () {
      return angular.copy(options);
    };

    /**
     * Sets the value of an existing option
     *
     * @param key
     * @param messageKey
     */
    this.setValidationMessageKey = function (key, messageKey) {
      if (options.validationMessages.hasOwnProperty(key)) {
        options.validationMessages[key] = messageKey;
      }
    };

    /**
     * Returns the provider instance
     *
     * @param $filter
     */
    this.$get = ['$filter', function($filter) {
      var self = this;
      return new function() {
        var translated = self.getOptions();

        // translate before returning
        Object.keys(translated.validationMessages).forEach(function(msg){
          translated.validationMessages[msg] =
              $filter('translate')(translated.validationMessages[msg]);
        });

        this.options = translated;
      };
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
          missingFiles: function(value) {
            if (value) {
              return value.obibaFiles && value.obibaFiles.length > 0;
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
  .controller('sfObibaFileUploadController', ['$scope', 'ngObibaMicaUrl','sfObibaFileUploadOptions',
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
        $scope.form.validationMessage =
          $scope.form.validationMessage ?
            $scope.form.validationMessage :
            sfObibaFileUploadOptions.options.validationMessages;
      });

      /**
       * Watch to make the form validationMessage visible.
       */
      $scope.$watch('ngModel.$modelValue', function() {
        if ($scope.ngModel.$validate) {
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
