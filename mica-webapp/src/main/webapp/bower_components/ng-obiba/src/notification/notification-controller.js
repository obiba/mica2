'use strict';

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

