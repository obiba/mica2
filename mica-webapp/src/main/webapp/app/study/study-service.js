'use strict';

mica.study
  .factory('DraftStudySummariesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-summaries');
    }])
  .factory('DraftStudySummaryResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-summary/:id', {}, {
        'get': {method: 'GET'}
      });
    }])
  .factory('DraftStudyResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id', {}, {
        // override $resource.save method because it uses POST by default
        'save': {method: 'PUT', params: {id: '@id'}},
        'get': {method: 'GET'}
      });
    }])
  .factory('DraftStudyPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id'}}
      });
    }]);
