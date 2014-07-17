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
        'publish': {method: 'PUT', params: {id: '@id'}},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
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
        'publish': {method: 'PUT', params: {id: '@id'}},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
      });
    }]);
