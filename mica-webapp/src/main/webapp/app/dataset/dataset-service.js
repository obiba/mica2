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

  .factory('StudyDatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-dataset/:id/_publish', {}, {
        'put': {method: 'PUT'},
        'delete': {method: 'DELETE'}
      });
    }])

  .factory('StudyDatasetIndexResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-dataset/:id/_index', {}, {
        'put': {method: 'PUT'}
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
    }])

  .factory('HarmonizedDatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonized-dataset/:id/_publish', {}, {
        'put': {method: 'PUT'},
        'delete': {method: 'DELETE'}
      });
    }])

  .factory('HarmonizedDatasetIndexResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonized-dataset/:id/_index', {}, {
        'put': {method: 'PUT'}
      });
    }]);
