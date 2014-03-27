'use strict';

micaApp.factory('Study', ['$resource',
  function ($resource) {
    return $resource('app/rest/studies/:id', {}, {
      'query': { method: 'GET', isArray: true},
      'get': { method: 'GET'}
    });
  }]);
