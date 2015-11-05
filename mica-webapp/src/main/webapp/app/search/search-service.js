'use strict';

mica.search

  .factory('TaxonomiesResource', ['$resource',
    function ($resource) {
      return $resource('ws/taxonomies/_filter', {}, {
        'get': {
          method: 'GET',
          isArray: true,
          errorHandler: true
        }
      });
    }])

  .factory('TaxonomyResource', ['$resource',
    function ($resource) {
      return $resource('ws/taxonomy/:name/_filter', {}, {
        'get': {
          method: 'GET',
          errorHandler: true
        }
      });
    }])

  .factory('VocabularyResource', ['$resource',
    function ($resource) {
      return $resource('ws/taxonomy/:taxonomy/vocabulary/:vocabulary/_filter', {}, {
        'get': {
          method: 'GET',
          errorHandler: true
        }
      });
    }]);
