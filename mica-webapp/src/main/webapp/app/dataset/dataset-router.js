'use strict';

mica.dataset
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/study-dataset', {
          templateUrl: 'app/dataset/views/study-dataset-list.html',
          controller: 'StudyDatasetListController'
        })
        .when('/study-dataset/new', {
          templateUrl: 'app/dataset/views/study-dataset-form.html',
          controller: 'StudyDatasetEditController'
        })
        .when('/study-dataset/:id', {
          templateUrl: 'app/dataset/views/study-dataset-view.html',
          controller: 'StudyDatasetViewController'
        })
        .when('/study-dataset/:id/edit', {
          templateUrl: 'app/dataset/views/study-dataset-form.html',
          controller: 'StudyDatasetEditController'
        })

        .when('/harmonized-dataset', {
          templateUrl: 'app/dataset/views/harmonized-dataset-list.html',
          controller: 'HarmonizedDatasetListController'
        })
        .when('/harmonized-dataset/new', {
          templateUrl: 'app/dataset/views/harmonized-dataset-form.html',
          controller: 'HarmonizedDatasetEditController'
        })
        .when('/harmonized-dataset/:id', {
          templateUrl: 'app/dataset/views/harmonized-dataset-view.html',
          controller: 'HarmonizedDatasetViewController'
        })
        .when('/harmonized-dataset/:id/edit', {
          templateUrl: 'app/dataset/views/harmonized-dataset-form.html',
          controller: 'HarmonizedDatasetEditController'
        });
    }]);
