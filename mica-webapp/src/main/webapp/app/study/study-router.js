/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
        // individual-study
        .when('/individual-study', {
          templateUrl: 'app/study/views/collection-study-main.html',
          controller: 'StudyMainController',
          reloadOnSearch: false
        })
        .when('/individual-study/new', {
          templateUrl: 'app/study/views/collection-study-form.html',
          controller: 'StudyEditController'
        })
        .when('/individual-study/:id', {
          templateUrl: 'app/study/views/collection-study-view.html',
          controller: 'StudyViewController'
        })
        .when('/individual-study/:id/edit', {
          templateUrl: 'app/study/views/collection-study-form.html',
          controller: 'StudyEditController'
        })
        .when('/individual-study/:id/revisions', {
          templateUrl: 'app/study/views/collection-study-view-revisions.html',
          controller: 'StudyViewController'
        })
        .when('/individual-study/:id/files', {
          templateUrl: 'app/study/views/collection-study-view-files.html',
          controller: 'StudyViewController',
          reloadOnSearch: false
        })
        .when('/individual-study/:id/permissions', {
          templateUrl: 'app/study/views/collection-study-view-permissions.html',
          controller: 'StudyViewController'
        })
        .when('/individual-study/:id/comments', {
          templateUrl: 'app/study/views/collection-study-view-comments.html',
          controller: 'StudyViewController'
        })
        .when('/individual-study/:id/population/add', {
          templateUrl: 'app/study/views/population/collection-population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/individual-study/:id/population/:pid/edit', {
          templateUrl: 'app/study/views/population/collection-population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/individual-study/:id/population/:pid/dce/add', {
          templateUrl: 'app/study/views/population/dce/data-collection-event-form.html',
          controller: 'StudyPopulationDceController'
        })
        .when('/individual-study/:id/population/:pid/dce/:dceId/edit', {
          templateUrl: 'app/study/views/population/dce/data-collection-event-form.html',
          controller: 'StudyPopulationDceController'
        })

        // harmonization-study
        .when('/harmonization-study', {
          templateUrl: 'app/study/views/harmonization-study-main.html',
          controller: 'HarmonizationStudyListController',
          reloadOnSearch: false
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
        });
    }]);
