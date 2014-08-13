'use strict';

mica.dataset
  .factory('StudyDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-datasets');
    }])

  .factory('DraftStudyDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-datasets', {}, {
        'save': {method: 'POST', errorHandler: true}
      });
    }])

  .factory('StudyDatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-dataset/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
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

  .factory('DraftHarmonizationDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-datasets', {}, {
        'save': {method: 'POST', errorHandler: true}
      });
    }])

  .factory('HarmonizationDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-datasets');
    }])

  .factory('HarmonizationDatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-dataset/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('HarmonizationDatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-dataset/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id'}},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
      });
    }]);
