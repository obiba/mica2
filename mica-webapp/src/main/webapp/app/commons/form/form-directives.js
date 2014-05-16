'use strict';

mica.form

  // http://codetunes.com/2013/server-form-validation-with-angular
  .directive('serverError', [function () {
    return {
      restrict: 'A',
      require: '?ngModel',
      link: function (scope, element, attrs, ctrl) {
        return element.on('change', function () {
          return scope.$apply(function () {
            return ctrl.$setValidity('server', true);
          });
        });
      }
    };
  }])

  .directive('formInput', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        required: '@',
        help: '@'
      },
      templateUrl: 'app/commons/form/form-input-template.html',
      link: function ($scope, elem, attr, ctrl) {
        console.log('micaInput', $scope);
        if ($scope.model == null) {
          $scope.model = "";
        }
        $scope.form = ctrl;
        console.log('$scope', $scope);
      }
    };
  }])

  .directive('formCheckbox', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        help: '@'
      },
      templateUrl: 'app/commons/form/form-checkbox-template.html',
      link: function ($scope, elem, attr, ctrl) {
        console.log('micaCheckbox', $scope);
        if ($scope.model == null) {
          $scope.model = false;
        }
        $scope.form = ctrl;
      }
    };
  }])

  // http://code.realcrowd.com/on-the-bleeding-edge-advanced-angularjs-form-validation/
  .directive('formSubmit', ['$parse', function ($parse) {
    return {
      restrict: 'A',
      require: ['micaSubmit', '?form'],
      controller: ['$scope', function ($scope) {
        this.attempted = false;

        var formController = null;

        this.setAttempted = function () {
          this.attempted = true;
        };

        this.setFormController = function (controller) {
          formController = controller;
        };

        this.needsAttention = function (fieldModelController) {
          if (!formController) return false;
          if (fieldModelController) {
            return fieldModelController.$invalid && (fieldModelController.$dirty || this.attempted);
          }
          return formController && formController.$invalid && (formController.$dirty || this.attempted);
        };
      }],
      compile: function (cElement, cAttributes, transclude) {
        return {
          pre: function (scope, formElement, attributes, controllers) {

            var submitController = controllers[0];
            var formController = (controllers.length > 1) ? controllers[1] : null;

            submitController.setFormController(formController);

            scope.mica = scope.mica || {};
            scope.mica[attributes.name] = submitController;
          },
          post: function (scope, formElement, attributes, controllers) {

            var submitController = controllers[0];
            var formController = (controllers.length > 1) ? controllers[1] : null;
            var fn = $parse(attributes.micaSubmit);

            formElement.bind('submit', function () {
              submitController.setAttempted();
              if (!scope.$$phase) scope.$apply();

              if (!formController.$valid) return false;

              scope.$apply(function () {
                fn(scope, {$event: event});
              });
            });
          }
        };
      }
    };
  }])
;