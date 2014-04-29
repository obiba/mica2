'use strict';

micaApp.controller('MicaConfigController', ['$scope', '$resource', '$log', 'resolvedMicaConfig', 'MicaConfig',

  function ($scope, $resource, $log, resolvedMicaConfig, MicaConfig) {
    $scope.micaConfig = resolvedMicaConfig;
    $scope.availableLanguages = $resource('ws/config/languages').get();
  }])

  .controller('MicaConfigEditController', ['$scope', '$resource', '$location', '$log', 'resolvedMicaConfig', 'MicaConfig',

    function ($scope, $resource, $location, $log, resolvedMicaConfig, MicaConfig) {

      $log.debug('edit $scope:', $scope);

      $scope.micaConfig = resolvedMicaConfig;
      $scope.availableLanguages = $resource('ws/config/languages').get();

      $scope.save = function () {
        MicaConfig.save($scope.micaConfig,
          function () {
            $location.path('/config').replace();
          },
          function (response) {

//            $log.debug('begin save $scope:', $scope);
//            $log.debug('response:', response);

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
//              $log.debug('field: ', field);
              $scope.form[field].$setValidity('server', false);
              $scope.errors[field] = error.message;
            });

            $log.debug('end save $scope:', $scope);

          });
      };

    }]);
