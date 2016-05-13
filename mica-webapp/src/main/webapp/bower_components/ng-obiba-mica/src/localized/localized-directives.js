'use strict';

angular.module('obiba.mica.localized')

  .directive('localized', ['LocalizedValues', function (LocalizedValues) {
    return {
      restrict: 'AE',
      scope: {
        value: '=',
        lang: '='
      },
      templateUrl: 'localized/localized-template.html',
      link: function(scope) {
        scope.LocalizedValues = LocalizedValues;
      }
    };
  }])

  .directive('localizedNumber', ['LocalizedValues', function (LocalizedValues) {
    return {
      restrict: 'E',
      scope: {number: '=value'},
      template: '{{LocalizedValues.formatNumber(number)}}',
      link: function($scope) {
        $scope.LocalizedValues = LocalizedValues;
      }
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
      templateUrl: 'localized/localized-input-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel) || newModel === null) {
            $scope.model = [{lang: $scope.lang, value: ''}];
          }

          var currentLang = $scope.model.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
            $scope.model.push({lang:$scope.lang, value: ''});
          }
        }, true);

        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
      }
    };
  }])

  .directive('localizedInputGroup', [function () {
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
        remove: '='
      },
      templateUrl: 'localized/localized-input-group-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel) || newModel === null) {
            $scope.model = [{lang: $scope.lang, value: ''}];
          }

          var currentLang = $scope.model.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
            $scope.model.push({lang:$scope.lang, value: ''});
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
      templateUrl: 'localized/localized-textarea-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel) || newModel === null) {
            $scope.model = [{lang: $scope.lang, value: ''}];
          }

          var currentLang = $scope.model.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
            $scope.model.push({lang:$scope.lang, value: ''});
          }
        }, true);

        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
      }
    };
  }]);
