'use strict';

mica.notification
  .controller('NotificationController', ['$rootScope', '$scope', '$modal', '$log',
    function ($rootScope, $scope, $modal, $log) {

      $scope.$on('showNotificationDialogEvent', function (event, message) {
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

