/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.study
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        // collection-study
        .when('/collection-study', {
          templateUrl: 'app/study/views/collection-study-main.html',
          controller: 'StudyMainController'
        })
        .when('/collection-study/new', {
          templateUrl: 'app/study/views/collection-study-form.html',
          controller: 'StudyEditController'
        })
        .when('/collection-study/:id', {
          templateUrl: 'app/study/views/collection-study-view.html',
          controller: 'StudyViewController'
        })
        .when('/collection-study/:id/edit', {
          templateUrl: 'app/study/views/collection-study-form.html',
          controller: 'StudyEditController'
        })
        .when('/collection-study/:id/revisions', {
          templateUrl: 'app/study/views/collection-study-view-revisions.html',
          controller: 'StudyViewController'
        })
        .when('/collection-study/:id/files', {
          templateUrl: 'app/study/views/collection-study-view-files.html',
          controller: 'StudyViewController',
          reloadOnSearch: false
        })
        .when('/collection-study/:id/permissions', {
          templateUrl: 'app/study/views/collection-study-view-permissions.html',
          controller: 'StudyViewController'
        })
        .when('/collection-study/:id/comments', {
          templateUrl: 'app/study/views/collection-study-view-comments.html',
          controller: 'StudyViewController'
        })
        .when('/collection-study/:id/population/add', {
          templateUrl: 'app/study/views/population/collection-population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/collection-study/:id/population/:pid/edit', {
          templateUrl: 'app/study/views/population/collection-population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/collection-study/:id/population/:pid/dce/add', {
          templateUrl: 'app/study/views/population/dce/data-collection-event-form.html',
          controller: 'StudyPopulationDceController'
        })
        .when('/collection-study/:id/population/:pid/dce/:dceId/edit', {
          templateUrl: 'app/study/views/population/dce/data-collection-event-form.html',
          controller: 'StudyPopulationDceController'
        })

        // harmonization-study
        .when('/harmonization-study', {
          templateUrl: 'app/study/views/harmonization-study-main.html',
          controller: 'HarmonizationStudyMainController'
        })
        .when('/harmonization-study/new', {
          templateUrl: 'app/study/views/harmonization-study-form.html',
          controller: 'HarmonizationStudyEditController'
        })
        .when('/harmonization-study/:id', {
          templateUrl: 'app/study/views/harmonization-study-view.html',
          controller: 'HarmonizationStudyViewController'
        })
        .when('/harmonization-study/:id/edit', {
          templateUrl: 'app/study/views/harmonization-study-form.html',
          controller: 'HarmonizationStudyEditController'
        })
        .when('/harmonization-study/:id/revisions', {
          templateUrl: 'app/study/views/harmonization-study-view-revisions.html',
          controller: 'HarmonizationStudyViewController'
        })
        .when('/harmonization-study/:id/files', {
          templateUrl: 'app/study/views/harmonization-study-view-files.html',
          controller: 'HarmonizationStudyViewController',
          reloadOnSearch: false
        })
        .when('/harmonization-study/:id/permissions', {
          templateUrl: 'app/study/views/harmonization-study-view-permissions.html',
          controller: 'HarmonizationStudyViewController'
        })
        .when('/harmonization-study/:id/comments', {
          templateUrl: 'app/study/views/harmonization-study-view-comments.html',
          controller: 'HarmonizationStudyViewController'
        })
        .when('/harmonization-study/:id/population/add', {
          templateUrl: 'app/study/views/population/harmonization-population-form.html',
          controller: 'HarmonizationStudyPopulationController'
        })
        .when('/harmonization-study/:id/population/:pid/edit', {
          templateUrl: 'app/study/views/population/harmonization-population-form.html',
          controller: 'HarmonizationStudyPopulationController'
        });
    }]);
