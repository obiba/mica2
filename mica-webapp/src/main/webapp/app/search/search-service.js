'use strict';

mica.search

  .factory('TaxonomiesResource', ['$resource',
    function ($resource) {
      return $resource('ws/taxonomies', {}, {
        'get': {
          method: 'GET',
          isArray: true,
          errorHandler: true
        }
      });
    }]);
