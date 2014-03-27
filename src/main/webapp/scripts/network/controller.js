'use strict';

micaApp.controller('NetworkController', ['$scope', 'resolvedNetwork', 'Network',
    function ($scope, resolvedNetwork, Network) {

        $scope.networks = resolvedNetwork;

        $scope.create = function () {
            Network.save($scope.network,
                function () {
                    $scope.networks = Network.query();
                    $('#saveNetworkModal').modal('hide');
                    $scope.clear();
                });
        };

        $scope.update = function (id) {
            $scope.network = Network.get({id: id});
            $('#saveNetworkModal').modal('show');
        };

        $scope.delete = function (id) {
            Network.delete({id: id},
                function () {
                    $scope.networks = Network.query();
                });
        };

        $scope.clear = function () {
            $scope.network = {id: "", sampleTextAttribute: "", sampleDateAttribute: ""};
        };
    }]);
