'use strict';

micaApp.controller('MicaConfigController', ['$scope', '$resource', '$modal', '$log', 'resolvedMicaConfig', 'MicaConfig',

  function ($scope, $resource, $modal, $log, resolvedMicaConfig, MicaConfig) {

    $log.debug('$scope:', $scope);

    $scope.micaConfig = resolvedMicaConfig;
    $scope.availableLanguages = $resource('ws/config/languages').get();

    $scope.edit = function (id) {
      $modal.open({
        templateUrl: 'views/config/form.html',
        controller: MicaConfigModalController,
        resolve: {
          micaConfig: function () {
            return MicaConfig.get();
          },
          availableLanguages: function () {
            return $scope.availableLanguages;
          }
        }
      }).result.then(function () {
          $scope.micaConfig = MicaConfig.get();
        });
    };

  }]);

var MicaConfigModalController = function ($scope, $log, $modalInstance, MicaConfig, micaConfig, availableLanguages) {

  $scope.form = {};

  $log.debug('modal $scope:', $scope);


  $scope.micaConfig = micaConfig;
  $scope.availableLanguages = availableLanguages;

//  $scope.httpStatus = '';
//  $scope.errors = [];
//
//  $scope.processErrors = function (response) {
//    $scope.clearErrors();
//    $scope.httpStatus = response.status;
//    response.data.forEach(function (error) {
//      $scope.errors[error.path] = error.message;
//    });
//    $log.debug('$scope.errors', $scope.errors);
//  };
//
//  $scope.clearErrors = function () {
//    $scope.httpStatus = '';
//    $scope.errors = [];
//  };

  $scope.save = function (form) {

    MicaConfig.save($scope.micaConfig,
      function () {
        $modalInstance.close();
      },
      function (response) {

        $log.debug('begin save $scope:', $scope);
        $log.debug('response:', response);

//        [{
//          "message": "ne peut pas être vide",
//          "messageTemplate": "{org.hibernate.validator.constraints.NotBlank.message}",
//          "path": "MicaConfig.name",
//          "invalidValue": ""
//        }]

        $scope.errors = [];
        response.data.forEach(function (error) {
          $log.debug('error: ', error);

          var field = error.path.substring(error.path.indexOf('.') + 1);

          $log.debug('field: ', field);

          $scope.form.micaConfig.$setValidity('server', false);
          //$scope.form.micaConfig[field].$setValidity('server', false);
          $scope.errors[field] = error.message;
        });

        $log.debug('end save $scope:', $scope);


      });
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };

};
