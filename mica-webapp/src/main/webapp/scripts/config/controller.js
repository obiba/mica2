'use strict';

micaApp.controller('MicaConfigController', ['$scope', '$resource', '$modal', 'resolvedMicaConfig', 'MicaConfig',

  function ($scope, $resource, $modal, resolvedMicaConfig, MicaConfig) {

    console.log($scope);

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

var MicaConfigModalController = function ($scope, $modalInstance, MicaConfig, micaConfig, availableLanguages) {

  $scope.form = {};

  console.log('MicaConfigModalController');
  console.log($scope);


  $scope.micaConfig = micaConfig;
  $scope.availableLanguages = availableLanguages;

  $scope.httpStatus = '';
  $scope.errors = [];

  $scope.processErrors = function (response) {
    $scope.clearErrors();
    $scope.httpStatus = response.status;
    response.data.forEach(function (error) {
      $scope.errors[error.path] = error.message;
    });
    console.log($scope.errors);
  };

  $scope.clearErrors = function () {
    $scope.httpStatus = '';
    $scope.errors = [];
  };

  $scope.save = function (form) {
    console.log('$scope.form:');
    console.log($scope.form);
    MicaConfig.save($scope.micaConfig,
      function () {
        $modalInstance.close();
      },
      function (response) {
        console.log('ERROR:');
        console.log(response);

//        [{
//          "message": "ne peut pas être vide",
//          "messageTemplate": "{org.hibernate.validator.constraints.NotBlank.message}",
//          "path": "MicaConfig.name",
//          "invalidValue": ""
//        }]

        response.data.forEach(function (error) {
          console.log(error);
          console.log('message: ' + error.message);
          console.log('path: ' + error.path);

          var field = error.path.substring(error.path.indexOf('.') + 1);

          console.log('field: ' + field);

          $scope.form[field].$setValidity('server', false);
          $scope.errors[field] = error.message.join(', ');
        });

//        return angular.forEach(response.data, function(error) {
//          console.log(error);
//          console.log('message: ' + error.message);
//          console.log('path: ' + error.path);
//
//          var field = error.path.substring(error.path.indexOf('.') + 1);
//
//          console.log('field: ' + field);
//
//          $scope.form[field].$setValidity('server', false);
//          return $scope.errors[field] = error.message.join(', ');
//        });

      });
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };

};
