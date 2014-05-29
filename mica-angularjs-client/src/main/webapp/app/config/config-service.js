'use strict';

mica.config.factory('MicaConfigResource', ['$resource',
  function ($resource) {
    return $resource('ws/config', {}, {
      // override $resource.save method because it uses POST by default
      'save': {method: 'PUT'},
      'get': {method: 'GET'}
    });
  }]);