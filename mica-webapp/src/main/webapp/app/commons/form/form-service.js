'use strict';

mica.form

  .service('FormServerValidation', ['$rootScope', '$log', 'StringUtils',
    function ($rootScope, $log, StringUtils) {
      this.error = function (response, form, languages) {
//        $log.debug('FormServerValidation response', response);
//        $log.debug('FormServerValidation form', form);
//        $log.debug('FormServerValidation languages', languages);

        if (response.data instanceof Array) {

          var setFieldError = function (field, error) {
            form[field].$dirty = true;
            form[field].$setValidity('server', false);
            if (form[field].errors == null) form[field].errors = [];
            form[field].errors.push(StringUtils.capitaliseFirstLetter(error.message));
          };

          response.data.forEach(function (error) {
            var fieldPrefix = error.path.split('.').slice(-2).join('.');
            if (languages && languages.length) {
              languages.forEach(function (lang) {
                setFieldError(fieldPrefix + '-' + lang, error);
              });
            } else {
              setFieldError(fieldPrefix, error);
            }
          });
          $log.debug(form);
        } else {
          $rootScope.$broadcast('showNotificationDialogEvent', {
            iconClass: "fa-exclamation-triangle",
            titleKey: "study.save-error",
            message: response.data ? response.data : angular.fromJson(response)
          });
        }

      }
    }]);