'use strict';

angular.module('obiba.form')

  .service('FormServerValidation', ['$rootScope', '$log', 'StringUtils', 'ServerErrorUtils', 'NOTIFICATION_EVENTS',
    function ($rootScope, $log, StringUtils, ServerErrorUtils, NOTIFICATION_EVENTS) {
      this.error = function (response, form, languages) {

        if (response.data instanceof Array) {

          var setFieldError = function (field, error) {
            form[field].$dirty = true;
            form[field].$setValidity('server', false);
            if (!form[field].errors) {
              form[field].errors = [];
            }
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
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
            titleKey: 'form-server-error',
            message: ServerErrorUtils.buildMessage(response)
          });
        }

      };
    }])

  .service('RadioGroupOptionBuilder', function() {
    this.build = function(prefix, items) {
      return items.map(function(item) {
        return {
          name: prefix,
          label: item.label || item,
          value: item.name
        };
      });
    };

    return this;
  });