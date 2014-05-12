'use strict';

mica.config
  .controller('MicaConfigController', ['$scope', '$resource', '$log', 'MicaConfigResource',

    function ($scope, $resource, $log, MicaConfigResource) {
      $scope.micaConfig = MicaConfigResource.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();
    }])

  .controller('MicaConfigEditController', ['$scope', '$resource', '$location', '$log', 'MicaConfigResource',

    function ($scope, $resource, $location, $log, MicaConfigResource) {

      $scope.micaConfig = MicaConfigResource.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();

      $scope.save = function () {
        MicaConfigResource.save($scope.micaConfig,
          function () {
            $location.path('/config').replace();
          },
          function (response) {
//            $log.debug('response:', response);

//        [{
//          "message": "ne peut pas être vide",
//          "messageTemplate": "{org.hibernate.validator.constraints.NotBlank.message}",
//          "path": "MicaConfig.name",
//          "invalidValue": ""
//        }]

            $scope.errors = [];
            response.data.forEach(function (error) {
              //$log.debug('error: ', error);
              var field = error.path.substring(error.path.indexOf('.') + 1);
              $scope.form[field].$dirty = true;
              $scope.form[field].$setValidity('server', false);
              $scope.errors[field] = error.message;
            });
          });
      };

    }]);
