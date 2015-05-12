'use strict';

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
        template: '<alert ng-repeat="alert in alerts" type="alert.type" close="close($index)"><span ng-bind-html="alert.message"></span></alert>',
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
