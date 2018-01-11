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

mica.projectConfig
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/admin/project-config', {
          templateUrl: 'app/project-config/views/project-config-form.html',
          controller: 'ProjectConfigController'
        });
    }]);

