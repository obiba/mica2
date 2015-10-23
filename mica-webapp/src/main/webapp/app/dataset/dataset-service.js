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

  .factory('StudyStateProjectsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-state/:id/projects', {}, {
        'get': {method: 'GET', params: {id: '@id'}}
      });
    }])

  .factory('HarmonizationDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-datasets');
    }])

  .factory('DraftHarmonizationDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-datasets', {}, {
        'save': {method: 'POST', errorHandler: true}
      });
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
    }])

  .factory('DatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id', type: '@type'}, errorHandler: true},
        'get': {method: 'GET', params: {id: '@id', type: '@type'}}
      });
    }])

  .factory('DatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id', type: '@type'}},
        'unPublish': {method: 'DELETE', params: {id: '@id', type: '@type'}}
      });
    }])

  .factory('DraftDatasetResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id', type: '@type'}, errorHandler: true},
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type'}, errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('DraftDatasetStatusResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/_status', {}, {
        'toStatus': {method: 'PUT', params: {id: '@id', type: '@type', value: '@value'}}
      });
    }])

  .factory('DraftDatasetRevisionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/commits', {}, {
        'get': {method: 'GET'}
      });
    }])

  .factory('DraftDatasetRestoreRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/commit/:commitId/restore', {}, {
        'restore': {method: 'PUT', params: {type: '@type', id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftDatasetViewRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET'}
      });
    }]);
