'use strict';

mica.admin

  .factory('TaxonomiesResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/taxonomies', {}, {
        'get': {
          method: 'GET',
          isArray: true,
          errorHandler: true
        }
      });
    }]);
