angular.module('sfLocalizedString').directive('updateOnBlur', function () {
  return {
    restrict: 'E',
    require: 'ngModel',
    scope: {
      locales: '='
    },
    templateUrl: 'src/templates/sf-localized-string-update-on-blur-template.html',
    link: function (scope, element, attrs, ngModel) {
      scope.modelValue = ngModel.$viewValue || {};
      scope.updateModel = function (locale, modelValue) {
        ngModel.$setViewValue(modelValue);
      };
    },
  };
});
