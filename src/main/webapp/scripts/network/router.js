'use strict';

micaApp
    .config(['$routeProvider', '$httpProvider', '$translateProvider',
        function ($routeProvider, $httpProvider, $translateProvider) {
            $routeProvider
                .when('/network', {
                    templateUrl: 'views/networks.html',
                    controller: 'NetworkController',
                    resolve:{
                        resolvedNetwork: ['Network', function (Network) {
                            return Network.query();
                        }]
                    }
                })
        }]);
