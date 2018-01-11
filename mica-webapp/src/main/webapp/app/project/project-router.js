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

mica.project
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/project', {
          templateUrl: 'app/project/views/project-list.html',
          controller: 'ProjectListController'
        })
        .when('/project/new', {
          templateUrl: 'app/project/views/project-form.html',
          controller: 'ProjectEditController'
        })
        .when('/project/:id', {
        templateUrl: 'app/project/views/project-view.html',
        controller: 'ProjectViewController'
        })
        .when('/project/:id/edit', {
          templateUrl: 'app/project/views/project-form.html',
          controller: 'ProjectEditController'
        })
        .when('/project/:id/revisions', {
          templateUrl: 'app/project/views/project-view-revisions.html',
          controller: 'ProjectViewController'
        })
        .when('/project/:id/files', {
          templateUrl: 'app/project/views/project-view-files.html',
          controller: 'ProjectViewController',
          reloadOnSearch: false
        })
        .when('/project/:id/permissions', {
          templateUrl: 'app/project/views/project-view-permissions.html',
          controller: 'ProjectViewController'
        })
        .when('/project/:id/comments', {
          templateUrl: 'app/project/views/project-view-comments.html',
          controller: 'ProjectViewController'
        });
    }]);
