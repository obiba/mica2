'use strict';

angular.module('obiba.utils', [])

  .service('StringUtils', function () {
    this.capitaliseFirstLetter = function (string) {
      return string ? string.charAt(0).toUpperCase() + string.slice(1) : null;
    };
  })

  .service('LocaleStringUtils', ['$filter', function ($filter) {
    this.translate = function (key, args) {

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

      return $filter('translate')(key, buildMessageArguments(args));
    };
  }])

  .service('ServerErrorUtils', ['LocaleStringUtils', function (LocaleStringUtils) {
    this.buildMessage = function(response) {
      var message = null;
      var data = response.data ? response.data : response;

      if (data) {
        if (data.messageTemplate) {
          message = LocaleStringUtils.translate(data.messageTemplate, data.arguments);
          if (message === data.messageTemplate) {
            message = null;
          }
        }

        if (!message && data.code && data.message) {
          message = 'Server Error ('+ data.code +'): ' + data.message;
        }
      }

      return message ? message : 'Server Error ('+ response.status +'): ' + response.statusText;
    };

  }]);
