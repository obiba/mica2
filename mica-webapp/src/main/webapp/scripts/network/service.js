'use strict';

micaApp.factory('Network', ['$resource',
  function ($resource) {
    return $resource('app/rest/networks/:id', {}, {
      'query': { method: 'GET', isArray: true},
      'get': { method: 'GET'}
    });
  }]);
