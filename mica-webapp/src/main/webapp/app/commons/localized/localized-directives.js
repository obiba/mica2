'use strict';

mica.localized

  .directive('localized', [function () {
    return {
      restrict: 'AE',
      scope: {
        value: '=',
        lang: '='
      },
      template: '<span ng-repeat="localizedValue in value | filter:{lang:lang}">{{localizedValue.value}}</span>'
    };
  }])

  .directive('localizedInput', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        required: '@',
        disabled: '=',
        lang: '=',
        help: '@'
      },
      templateUrl: 'app/commons/localized/localized-input-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel)) {
            return;
          }

          var currentLang = newModel.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
              newModel.push({lang:$scope.lang, value: ''});
          }
        }, true);

        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
      }
    };
  }])

  .directive('localizedTextarea', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        required: '@',
        disabled: '=',
        lang: '=',
        help: '@',
        rows: '@'
      },
      templateUrl: 'app/commons/localized/localized-textarea-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel)) {
            return;
          }

          var currentLang = newModel.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
              newModel.push({lang:$scope.lang, value: ''});
          }
        }, true);

        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
      }
    };
  }]);
