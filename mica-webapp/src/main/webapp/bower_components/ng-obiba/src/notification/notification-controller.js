'use strict';

angular.module('obiba.notification')

  .constant('NOTIFICATION_EVENTS', {
    showNotificationDialog: 'event:show-notification-dialog',
    showConfirmDialog: 'event:show-confirmation-dialog',
    confirmDialogAccepted: 'event:confirmation-accepted',
    confirmDialogRejected: 'event:confirmation-rejected'
  })

  .controller('NotificationController', ['$rootScope', '$scope', '$uibModal', 'NOTIFICATION_EVENTS',
    function ($rootScope, $scope, $uibModal, NOTIFICATION_EVENTS) {

      $scope.$on(NOTIFICATION_EVENTS.showNotificationDialog, function (event, notification) {
        $uibModal.open({
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
        $uibModal.open({
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

  .controller('NotificationModalController', ['$scope', '$uibModalInstance', 'notification',
    function ($scope, $uibModalInstance, notification) {

      $scope.notification = notification;
      if (!$scope.notification.iconClass) {
        $scope.notification.iconClass = 'fa-exclamation-triangle';
      }
      if (!$scope.notification.title && !$scope.notification.titleKey) {
        $scope.notification.titleKey = 'error';
      }

      $scope.close = function () {
        $uibModalInstance.dismiss('close');
      };

    }])

  .controller('NotificationConfirmationController', ['$scope', '$uibModalInstance', 'confirm', 'LocaleStringUtils',
    function ($scope, $uibModalInstance, confirm, LocaleStringUtils) {

      function getMessage() {
        return {
          title: confirm.titleKey ? LocaleStringUtils.translate(confirm.titleKey) : confirm.title,
          message: confirm.messageKey ? LocaleStringUtils.translate(confirm.messageKey, confirm.messageArgs) : confirm.message
        };
      }

      $scope.confirm = getMessage();

      $scope.ok = function () {
        $uibModalInstance.close();
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

    }]);

