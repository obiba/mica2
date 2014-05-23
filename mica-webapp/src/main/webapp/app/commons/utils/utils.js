'use strict';

mica.utils = angular.module('mica.utils', [])

  .service('StringUtils', function () {
    this.capitaliseFirstLetter = function (string) {
      return string.charAt(0).toUpperCase() + string.slice(1);
    }
  });
