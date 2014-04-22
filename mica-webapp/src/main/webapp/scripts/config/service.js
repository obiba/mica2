'use strict';

micaApp.factory('MicaConfig', ['$resource',
  function ($resource) {
    return $resource('ws/config', {}, {
      'get': { method: 'GET'}
    });
  }]);
