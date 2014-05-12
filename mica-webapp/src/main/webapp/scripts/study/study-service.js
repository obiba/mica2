'use strict';

mica.study
  .factory('StudiesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/studies');
    }])
  .factory('StudyResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id');
    }]);
