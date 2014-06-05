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
        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
//        console.log('localizedInput', $scope);
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
        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
//        console.log('localizedTextarea', $scope);
      }
    };
  }]);