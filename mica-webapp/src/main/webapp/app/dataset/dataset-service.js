'use strict';

mica.dataset
  .factory('StudyDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/study-datasets');
    }])

  .factory('StudyDatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/study-dataset/:id', {}, {
        'get': {method: 'GET'}
      });
    }])

  .factory('HarmonizedDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/harmonized-datasets');
    }])

  .factory('HarmonizedDatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/harmonized-dataset/:id', {}, {
        'get': {method: 'GET'}
      });
    }]);
