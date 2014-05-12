'use strict';

mica.study
  .factory('DraftStudiesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/studies');
    }])
  .factory('DraftStudyResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id', {}, {
        // override $resource.save method because it uses POST by default
        'save': {method: 'PUT', params: {id: '@id'}},
        'get': {method: 'GET'}
      });
    }]);
