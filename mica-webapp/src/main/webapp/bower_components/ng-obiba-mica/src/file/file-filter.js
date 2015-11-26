'use strict';

angular.module('obiba.mica.file')
  .filter('bytes', function () {
    return function (bytes) {
      return bytes === null || typeof bytes === 'undefined' ? '' : filesize(bytes);
    };
  });

