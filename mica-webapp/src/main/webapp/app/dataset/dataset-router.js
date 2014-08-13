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

        .when('/harmonization-dataset', {
          templateUrl: 'app/dataset/views/harmonization-dataset-list.html',
          controller: 'HarmonizationDatasetListController'
        })
        .when('/harmonization-dataset/new', {
          templateUrl: 'app/dataset/views/harmonization-dataset-form.html',
          controller: 'HarmonizationDatasetEditController'
        })
        .when('/harmonization-dataset/:id', {
          templateUrl: 'app/dataset/views/harmonization-dataset-view.html',
          controller: 'HarmonizationDatasetViewController'
        })
        .when('/harmonization-dataset/:id/edit', {
          templateUrl: 'app/dataset/views/harmonization-dataset-form.html',
          controller: 'HarmonizationDatasetEditController'
        });
    }]);
