'use strict';

micaApp.factory('Study', ['$resource',
  function ($resource) {
    return $resource('ws/studies/:id', {}, {
      'query': { method: 'GET', isArray: true},
      'get': { method: 'GET'}
    });
  }]);
