'use strict';

mica.file
  .factory('TempFileResource', ['$resource',
    function ($resource) {
      return $resource('ws/files/temp/:id', {}, {
        'get': {method: 'GET'},
        'delete': {method: 'DELETE'}
      });
    }])
;
