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
        .when('/study', {
          templateUrl: 'app/study/views/study-main.html',
          controller: 'StudyMainController'
        })
        .when('/study/new', {
          templateUrl: 'app/study/views/study-form.html',
          controller: 'StudyEditController'
        })
        .when('/study/:id', {
          templateUrl: 'app/study/views/study-view.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/edit', {
          templateUrl: 'app/study/views/study-form.html',
          controller: 'StudyEditController'
        })
        .when('/study/:id/revisions', {
          templateUrl: 'app/study/views/study-view-revisions.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/files', {
          templateUrl: 'app/study/views/study-view-files.html',
          controller: 'StudyViewController',
          reloadOnSearch: false
        })
        .when('/study/:id/permissions', {
          templateUrl: 'app/study/views/study-view-permissions.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/comments', {
          templateUrl: 'app/study/views/study-view-comments.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/population/add', {
          templateUrl: 'app/study/views/population/population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/study/:id/population/:pid/edit', {
          templateUrl: 'app/study/views/population/population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/study/:id/population/:pid/dce/add', {
          templateUrl: 'app/study/views/population/dce/data-collection-event-form.html',
          controller: 'StudyPopulationDceController'
        })
        .when('/study/:id/population/:pid/dce/:dceId/edit', {
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
          templateUrl: 'app/study/views/population/population-form.html',
          controller: 'HarmonizationStudyPopulationController'
        })
        .when('/harmonization-study/:id/population/:pid/edit', {
          templateUrl: 'app/study/views/population/population-form.html',
          controller: 'HarmonizationStudyPopulationController'
        });
    }]);
