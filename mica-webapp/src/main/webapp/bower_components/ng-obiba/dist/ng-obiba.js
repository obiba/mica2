/*!
 * ng-obiba - v0.1.0
 * https://github.com/obiba/ng-obiba

 * License: GNU Public License version 3
 * Date: 2015-05-13
 */
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
;'use strict';

angular.module('obiba.notification', [
  'templates-main',
  'pascalprecht.translate',
  'ui.bootstrap'
]);
;'use strict';

angular.module('obiba.notification')

  .constant('NOTIFICATION_EVENTS', {
    showNotificationDialog: 'event:show-notification-dialog',
    showConfirmDialog: 'event:show-confirmation-dialog',
    confirmDialogAccepted: 'event:confirmation-accepted',
    confirmDialogRejected: 'event:confirmation-rejected'
  })

  .controller('NotificationController', ['$rootScope', '$scope', '$modal', 'NOTIFICATION_EVENTS',
    function ($rootScope, $scope, $modal, NOTIFICATION_EVENTS) {

      $scope.$on(NOTIFICATION_EVENTS.showNotificationDialog, function (event, notification) {
        $modal.open({
          templateUrl: 'notification/notification-modal.tpl.html',
          controller: 'NotificationModalController',
          resolve: {
            notification: function () {
              return notification;
            }
          }
        });
      });

      $scope.$on(NOTIFICATION_EVENTS.showConfirmDialog, function (event, confirm, args) {
        $modal.open({
          templateUrl: 'notification/notification-confirm-modal.tpl.html',
          controller: 'NotificationConfirmationController',
          resolve: {
            confirm: function () {
              return confirm;
            }
          }
        }).result.then(function () {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.confirmDialogAccepted, args);
          }, function () {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.confirmDialogRejected, args);
          });
      });

    }])

  .controller('NotificationModalController', ['$scope', '$modalInstance', 'notification',
    function ($scope, $modalInstance, notification) {

      $scope.notification = notification;
      if (!$scope.notification.iconClass) {
        $scope.notification.iconClass = 'fa-exclamation-triangle';
      }
      if (!$scope.notification.title && !$scope.notification.titleKey) {
        $scope.notification.titleKey = 'error';
      }

      $scope.close = function () {
        $modalInstance.dismiss('close');
      };

    }])

  .controller('NotificationConfirmationController', ['$scope', '$modalInstance', 'confirm', 'LocaleStringUtils',
    function ($scope, $modalInstance, confirm, LocaleStringUtils) {

      function getMessage() {
        return {
          title: confirm.titleKey ? LocaleStringUtils.translate(confirm.titleKey) : confirm.title,
          message: confirm.messageKey ? LocaleStringUtils.translate(confirm.messageKey, confirm.messageArgs) : confirm.message
        };
      }

      $scope.confirm = getMessage();

      $scope.ok = function () {
        $modalInstance.close();
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

    }]);

;'use strict';

angular.module('obiba.rest', ['obiba.notification'])

  .config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('httpErrorsInterceptor');
  }])

  .factory('httpErrorsInterceptor', ['$q', '$rootScope', 'NOTIFICATION_EVENTS', 'ServerErrorUtils',
    function ($q, $rootScope, NOTIFICATION_EVENTS, ServerErrorUtils) {
      return function (promise) {
        return promise.then(
          function (response) {
            // $log.debug('httpErrorsInterceptor success', response);
            return response;
          },
          function (response) {
            // $log.debug('httpErrorsInterceptor error', response);
            var config = response.config;
            if (config.errorHandler) {
              return $q.reject(response);
            }
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
              message: ServerErrorUtils.buildMessage(response)
            });
            return $q.reject(response);
          });
      };

    }]);
;'use strict';

angular.module('obiba.form', [
  'obiba.utils',
  'obiba.notification',
  'templates-main'
]);
;'use strict';

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
    }]);;'use strict';

angular.module('obiba.form')

  // http://codetunes.com/2013/server-form-validation-with-angular
  .directive('formServerError', [function () {
    return {
      restrict: 'A',
      require: '?ngModel',
      link: function (scope, element, attrs, ctrl) {
        return element.on('change', function () {
          return scope.$apply(function () {
            return ctrl.$setValidity('server', true);
          });
        });
      }
    };
  }])

  .directive('formInput', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        disabled: '=',
        type: '@',
        label: '@',
        required: '=',
        min: '@',
        max: '@',
        step: '@',
        help: '@'
      },
      templateUrl: 'form/form-input-template.tpl.html',
      compile: function(elem, attrs) {
        if (!attrs.type) { attrs.type = 'text'; }
        return {
          post: function (scope, elem, attr, ctrl) {
            scope.form = ctrl;
          }
        };
      }
    };
  }])

  .directive('formTextarea', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        disabled: '=',
        label: '@',
        required: '=',
        help: '@'
      },
      templateUrl: 'form/form-textarea-template.tpl.html',
      compile: function(elem, attrs) {
        if (!attrs.type) { attrs.type = 'text'; }
        return {
          post: function ($scope, elem, attr, ctrl) {
            $scope.form = ctrl;
          }
        };
      }
    };
  }])

  .directive('formLocalizedInput', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        locales: '=',
        name: '@',
        model: '=',
        label: '@',
        required: '=',
        help: '@'
      },
      templateUrl: 'form/form-localized-input-template.tpl.html',
      link: function ($scope, elem, attr, ctrl) {
        $scope.form = ctrl;
      }
    };
  }])

  .directive('formCheckbox', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        help: '@'
      },
      templateUrl: 'form/form-checkbox-template.tpl.html',
      link: function ($scope, elem, attr, ctrl) {
        $scope.form = ctrl;
      }
    };
  }])

  .directive('formCheckboxGroup', [function() {
    return {
      restrict: 'A',
      scope: {
        options: '=',
        model: '='
      },
      template: '<div form-checkbox ng-repeat="item in items" name="{{item.name}}" model="item.value" label="{{item.label}}">',
      link: function ($scope, elem, attrs) {
        $scope.$watch('model', function(selected) {
          $scope.items = $scope.options.map(function(n) {
            var value = angular.isArray(selected) && (selected.indexOf(n) > -1 ||
              selected.indexOf(n.name) > -1);
            return {
              name: attrs.model + '.' + (n.name || n),
              label: n.label || n,
              value: value
            };
          });
        }, true);

        $scope.$watch('items', function(items) {
          if (angular.isArray(items)) {
            $scope.model = items.filter(function(e) { return e.value; })
              .map(function(e) { return e.name.replace(attrs.model + '.', ''); });
          }
        }, true);

        $scope.$watch('options', function(opts) {
          $scope.items = opts.map(function(n) {
            var value = angular.isArray($scope.model) && ($scope.model.indexOf(n) > -1 ||
              $scope.model.indexOf(n.name) > -1);
            return {
              name: attrs.model + '.' + (n.name || n),
              label: n.label || n,
              value: value
            };
          });
        }, true);
      }
    };
  }]);
;'use strict';

angular.module('obiba.alert', [
  'templates-main',
  'pascalprecht.translate',
  'ui.bootstrap',
  'ngSanitize'
]);
;'use strict';

angular.module('obiba.alert')

  .constant('ALERT_EVENTS', {
    showAlert: 'event:show-alert'
  })

  .service('AlertService', ['$rootScope', '$log', 'LocaleStringUtils', 'ALERT_EVENTS',
    function ($rootScope, $log, LocaleStringUtils, ALERT_EVENTS) {

      function getValidMessage(options) {
        var value = LocaleStringUtils.translate(options.msgKey, options.msgArgs);
        if (value === options.msgKey) {
          if (options.msg) {
            return options.msg;
          }

          $log.error('No message was provided for the alert!');
          return '';
        }

        return value;
      }

      this.alert = function (options) {
        $rootScope.$broadcast(ALERT_EVENTS.showAlert, {
          uid: new Date().getTime(), // useful for delay closing and cleanup
          message: getValidMessage(options),
          type: options.type ? options.type : 'info',
          timeoutDelay: options.delay ? Math.max(0, options.delay) : 0
        }, options.id);
      };
    }]);
;'use strict';

angular.module('obiba.alert')

  .directive('obibaAlert', ['$rootScope', '$timeout', '$log', 'ALERT_EVENTS',
    function ($rootScope, $timeout, $log, ALERT_EVENTS) {
      var alertsMap = {};

      $rootScope.$on(ALERT_EVENTS.showAlert, function (event, alert, id) {
        if (alertsMap[id]) {
          alertsMap[id].push(alert);
        }
      });

      return {
        restrict: 'E',
        template: '<alert ng-repeat="alert in alerts" type="{{alert.type}}" close="close($index)"><span ng-bind-html="alert.message"></span></alert>',
        compile: function(element) {
          var id = element.attr('id');
          if (!id) {
            $log.error('ObibaAlert directive must have a DOM id attribute.');
          } else {
            alertsMap[id] = [];
          }

          return {
            post: function (scope) {
              if (!id) {
                return;
              }

              scope.alerts = alertsMap[id];

              /**
               * Called when user manually closes or the timeout has expired
               * @param index
               */
              scope.close = function(index) {
                scope.alerts.splice(index, 1);
              };

              /**
               * Called when timeout has expired
               * @param uid
               */
              scope.closeByUid = function(uid) {
                var index = scope.alerts.map(function(alert) {
                  return alert.uid === uid;
                }).indexOf(true);

                if (index !== -1) {
                  scope.close(index);
                }
              };

              /**
               * when all alerts have been added, proceed with setting the timeout for those that have timeoutDelay > 0
               */
              scope.$watch('alerts', function(newAlerts, oldAlerts) {
                if (newAlerts.length - oldAlerts.length > 0) {
                  newAlerts.filter(function(alert) {
                    return alert.timeoutDelay > 0;
                  }).forEach(function(alert) {
                    $timeout(scope.closeByUid.bind(null, alert.uid), alert.timeoutDelay);
                  });
                }
              }, true);
            }
          };
        }
    };
  }]);
;'use strict';

angular.module('ngObiba', [
  'obiba.form',
  'obiba.notification',
  'obiba.rest',
  'obiba.utils',
  'obiba.alert'
]);
;angular.module('templates-main', ['form/form-checkbox-template.tpl.html', 'form/form-input-template.tpl.html', 'form/form-localized-input-template.tpl.html', 'form/form-textarea-template.tpl.html', 'notification/notification-confirm-modal.tpl.html', 'notification/notification-modal.tpl.html']);

angular.module("form/form-checkbox-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-checkbox-template.tpl.html",
    "<div class=\"checkbox\" ng-class=\"{'has-error': (form[fieldName].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "  <label for=\"{{name}}\" class=\"control-label\">\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "    <input\n" +
    "          ng-model=\"model\"\n" +
    "          type=\"checkbox\"\n" +
    "          id=\"{{name}}\"\n" +
    "          name=\"{{name}}\"\n" +
    "          form-server-error\n" +
    "          ng-required=\"required\">\n" +
    "      {{label | translate}}\n" +
    "  </label>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "    <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>\n" +
    "\n" +
    "");
}]);

angular.module("form/form-input-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-input-template.tpl.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[name].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "  <label for=\"{{name}}\" class=\"control-label\">\n" +
    "    {{label | translate}}\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "  </label>\n" +
    "\n" +
    "  <input\n" +
    "      ng-model=\"model\"\n" +
    "      type=\"{{type}}\"\n" +
    "      class=\"form-control\"\n" +
    "      id=\"{{name}}\"\n" +
    "      name=\"{{name}}\"\n" +
    "      form-server-error\n" +
    "      ng-attr-min=\"{{min}}\"\n" +
    "      ng-attr-max=\"{{max}}\"\n" +
    "      ng-attr-step=\"{{step}}\"\n" +
    "      ng-disabled=\"disabled\"\n" +
    "      ng-required=\"required\"/>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "    <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("form/form-localized-input-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-localized-input-template.tpl.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[name].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "    <label for=\"{{name}}\" class=\"control-label\">\n" +
    "        {{label | translate}}\n" +
    "        <span ng-show=\"required\">*</span>\n" +
    "    </label>\n" +
    "\n" +
    "    <div class=\"input-group\" ng-repeat=\"locale in locales track by $index\">\n" +
    "        <span class=\"input-group-addon\">{{locale.lang}}</span>\n" +
    "        <input\n" +
    "                ng-model=\"locale.value\"\n" +
    "                type=\"text\"\n" +
    "                class=\"form-control\"\n" +
    "                id=\"{{name}}.{{locale.lang}}\"\n" +
    "                name=\"{{name}}.locale.lang}}\"\n" +
    "                form-server-error>\n" +
    "    </div>\n" +
    "\n" +
    "    <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "        <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "        <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "    </ul>\n" +
    "\n" +
    "    <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>");
}]);

angular.module("form/form-textarea-template.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("form/form-textarea-template.tpl.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[name].$dirty || form.saveAttempted) && form[name].$invalid}\">\n" +
    "\n" +
    "  <label for=\"{{name}}\" class=\"control-label\">\n" +
    "    {{label | translate}}\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "  </label>\n" +
    "\n" +
    "  <textarea\n" +
    "      ng-model=\"model\"\n" +
    "      class=\"form-control\"\n" +
    "      id=\"{{name}}\"\n" +
    "      name=\"{{name}}\"\n" +
    "      form-server-error\n" +
    "      ng-disabled=\"disabled\"\n" +
    "      ng-required=\"required\"></textarea>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[name].$dirty && form[name].$invalid\">\n" +
    "    <li ng-show=\"form[name].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[name].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("notification/notification-confirm-modal.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("notification/notification-confirm-modal.tpl.html",
    "<div class=\"modal-content\">\n" +
    "\n" +
    "  <div class=\"modal-header\">\n" +
    "    <button type=\"button\" class=\"close\" aria-hidden=\"true\" ng-click=\"cancel()\">&times;</button>\n" +
    "    <h4 class=\"modal-title\">\n" +
    "      <i class=\"fa fa-exclamation-triangle\"></i>\n" +
    "      <span ng-hide=\"confirm.title\" translate>confirmation</span>\n" +
    "      {{confirm.title}}\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-body\">\n" +
    "    <p>{{confirm.message}}</p>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-footer\">\n" +
    "    <button type=\"button\" class=\"btn btn-default\" ng-click=\"cancel()\">\n" +
    "      <span ng-hide=\"confirm.cancel\" translate>cancel</span>\n" +
    "      {{confirm.cancel}}\n" +
    "    </button>\n" +
    "    <button type=\"button\" class=\"btn btn-primary\" ng-click=\"ok()\">\n" +
    "      <span ng-hide=\"confirm.ok\" translate>ok</span>\n" +
    "      {{confirm.ok}}\n" +
    "    </button>\n" +
    "  </div>\n" +
    "\n" +
    "</div>");
}]);

angular.module("notification/notification-modal.tpl.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("notification/notification-modal.tpl.html",
    "<div class=\"modal-content\">\n" +
    "\n" +
    "  <div class=\"modal-header\">\n" +
    "    <button type=\"button\" class=\"close\" aria-hidden=\"true\" ng-click=\"close()\">&times;</button>\n" +
    "    <h4 class=\"modal-title\">\n" +
    "      <i ng-hide=\"notification.iconClass\" class=\"fa fa-info-circle\"></i>\n" +
    "      <i ng-show=\"notification.iconClass\" class=\"fa {{notification.iconClass}}\"></i>\n" +
    "      <span ng-hide=\"notification.title\" translate>{{notification.titleKey || 'notification'}}</span>\n" +
    "      {{notification.title}}\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-body\">\n" +
    "    <p>{{notification.message}}</p>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"modal-footer\">\n" +
    "    <button type=\"button\" class=\"btn btn-default\" ng-click=\"close()\">\n" +
    "      <span translate>close</span>\n" +
    "    </button>\n" +
    "  </div>\n" +
    "\n" +
    "</div>");
}]);
