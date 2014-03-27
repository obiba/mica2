'use strict';

micaApp.factory('Study', ['$resource',
    function ($resource) {
        return $resource('app/rest/studys/:id', {}, {
            'query': { method: 'GET', isArray: true},
            'get': { method: 'GET'}
        });
    }]);
