'use strict';

mica.localized

  .directive('localized', [function () {
    return {
      restrict: 'E',
      replace: 'true',
      scope: {
        value: '=',
        lang: '='
      },
      template: '<span ng-repeat="localizedValue in value | filter:{lang:lang}">{{localizedValue.value}}</span>'
    };
  }]);