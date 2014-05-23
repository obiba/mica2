'use strict';

mica.form

  .service('FormServerValidation', ['$rootScope', '$log', 'StringUtils',
    function ($rootScope, $log, StringUtils) {
      this.error = function (response, languages, form) {
        if (response.data instanceof Array) {
          response.data.forEach(function (error) {
            var fieldPrefix = error.path.split('.').slice(-2).join('.');
            languages.forEach(function (lang) {
              var field = fieldPrefix + '-' + lang;
              form[field].$dirty = true;
              form[field].$setValidity('server', false);
              if (form[field].errors == null) form[field].errors = [];
              form[field].errors.push(StringUtils.capitaliseFirstLetter(error.message));
            });
          });
        } else {
          $rootScope.$broadcast('showNotificationDialogEvent', {
            iconClass: "fa-exclamation-triangle",
            titleKey: "study.save-error",
            message: response.data ? response.data : angular.fromJson(response)
          });
        }
      }
    }])


;