'use strict';

mica.dataset
  .factory('StudyDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-datasets');
    }])

  .factory('StudyDatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-dataset/:id', {}, {
        'get': {method: 'GET'}
      });
    }])

  .factory('HarmonizedDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonized-datasets');
    }])

  .factory('HarmonizedDatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonized-dataset/:id', {}, {
        'get': {method: 'GET'}
      });
    }]);
