'use strict';

micaApp.factory('StudiesResource', ['$resource',
  function ($resource) {
    return $resource('ws/draft/studies');
  }])
  .factory('StudyResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id');
    }]);
