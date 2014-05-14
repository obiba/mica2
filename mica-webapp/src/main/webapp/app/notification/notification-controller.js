'use strict';

mica.notification
  .controller('NotificationController', ['$rootScope', '$scope', '$modal',
    function ($rootScope, $scope, $modal) {

      $scope.$on('showNotificationDialogEvent', function (event, notification) {
        $modal.open({
          templateUrl: 'app/notification/notification-modal.html',
          controller: 'NotificationModalController',
          resolve: {
            notification: function () {
              return notification;
            }
          }
        });
      });

      $scope.$on('showConfirmDialogEvent', function (event, confirm, args) {
        $modal.open({
          templateUrl: 'app/notification/notification-confirm-modal.html',
          controller: 'NotificationConfirmationController',
          resolve: {
            confirm: function () {
              return confirm;
            }
          }
        }).result.then(function () {
            $rootScope.$broadcast('confirmDialogAcceptedEvent', args);
          }, function () {
            $rootScope.$broadcast('confirmDialogRejectedEvent', args);
          });
      });

    }])
  .controller('NotificationModalController', ['$scope', '$modalInstance', 'notification',
    function ($scope, $modalInstance, notification) {

      $scope.notification = notification;

      $scope.close = function () {
        $modalInstance.dismiss('close');
      };

    }])
  .controller('NotificationConfirmationController', ['$scope', '$modalInstance', 'confirm',
    function ($scope, $modalInstance, confirm) {

      $scope.confirm = confirm;

      $scope.ok = function () {
        $modalInstance.close();
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

    }]);

