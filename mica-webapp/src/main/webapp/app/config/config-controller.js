'use strict';

mica.config
  .controller('MicaConfigController', ['$scope', '$resource', '$log', 'MicaConfigResource',

    function ($scope, $resource, $log, MicaConfigResource) {
      $scope.micaConfig = MicaConfigResource.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();
    }])

  .controller('MicaConfigEditController', ['$scope', '$resource', '$location', '$log', 'MicaConfigResource', 'FormServerValidation',

    function ($scope, $resource, $location, $log, MicaConfigResource, FormServerValidation) {

      $scope.micaConfig = MicaConfigResource.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();

      $scope.save = function () {

        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        $scope.micaConfig.$save(
          function () {
            $location.path('/config').replace();
          },
          function (response) {
            FormServerValidation.error(response, $scope.form);
          });
      };

    }]);
