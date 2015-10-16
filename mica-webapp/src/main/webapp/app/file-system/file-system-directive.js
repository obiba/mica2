/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.fileSystem
  .directive('filesList', [function () {
    return {
      restrict: 'EA',
      replace: true,
      controller: 'FileSystemController',
      scope: {
        docPath: '@',
        docId: '@'
      },
      templateUrl: 'app/file-system/views/file-system-template.html'
    };
  }]);
