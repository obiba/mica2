'use strict';

angular.module('obiba.alert')

  .directive('obibaAlert', ['$rootScope', '$timeout', '$log', 'ALERT_EVENTS',
    function ($rootScope, $timeout, $log, ALERT_EVENTS) {

      return {
        restrict: 'AE',
        scope: {
          id: '@'
        },
        templateUrl: 'alert/alert-template.tpl.html',
        link: function(scope) {
          scope.alerts = [];
          if (!scope.id) {
            throw new Error('ObibaAlert directive must have a DOM id attribute.');
          }

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

          scope.$on(ALERT_EVENTS.showAlert, function (event, alert, id) {
            if (scope.id === id) {
              scope.alerts.push(alert);
              if (alert.timeoutDelay > 0) {
                $timeout(scope.closeByUid.bind(null, alert.uid), alert.timeoutDelay);
              }
            }
          });
        }
    };
  }]);
