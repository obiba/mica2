'use strict';

angular.module('obiba.form')

  .service('FormServerValidation', ['$rootScope', '$log', '$filter', 'StringUtils', 'NOTIFICATION_EVENTS',
    function ($rootScope, $log, $filter, StringUtils, NOTIFICATION_EVENTS) {
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
            message: buildMessage(response)
          });
        }

      };

      function buildMessage(response) {
        var message = null;
        var data = response.data ? response.data : response;

        if (data) {
          if (data.messageTemplate) {
            message = $filter('translate')(data.messageTemplate, buildMessageArguments(data.arguments));
            if (message === data.messageTemplate) {
              message = null;
            }
          }

          if (!message && data.message) {
            message = 'Server Error ('+ data.code +'): ' + data.message;
          }
        }

        return message ? message : angular.fromJson(response);
      }

      function buildMessageArguments(args) {
        if (args &&  args instanceof Array) {
          var messageArgs = {};
          args.forEach(function(arg, index) {
            messageArgs['arg'+index] = arg;
          });

          return messageArgs;
        }

        return {};
      }

    }]);