'use strict';

angular.module('formModule', ['schemaForm'])
  .controller('FormController', ['$scope', function ($scope) {
    $scope.forms = {};
    $scope.schema = formSchema;
    $scope.form = formDefinition;
    $scope.model = formModel;

    $scope.submit = function (id) {
      $scope.$broadcast('schemaFormValidate');
      // check if the form is valid
      if ($scope.forms.requestForm.$valid) {
        micajs.signup.submit(id);
      } else {
        // an invalid form cannot be submitted
        micajs.error(formMessages.validationErrorOnSubmit);
      }
    };
  }]);
