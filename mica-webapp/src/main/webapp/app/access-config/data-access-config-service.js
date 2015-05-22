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

mica.dataAccesConfig

  .factory('DataAccessFormResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/data-access-form', {}, {
        'get': {method: 'GET', errorHandler: true},
        'save': {method: 'PUT', errorHandler: true}
      });
    }])


  .factory('DataAccessFormService', ['BrowserDetector',
    function (BrowserDetector) {
      this.getEditorOptions = function() {
        return {
          options: {
            theme: 'monokai',
            mode: 'json',
            displayIndentGuides: true,
            useElasticTabstops: true
          }
        };
      };

      this.gotoFullScreen = function (id) {
        {
          var view = document.getElementById(id);

          switch (BrowserDetector.detect()) {
            case 'ie':
              view.msRequestFullscreen();
              break;
            case 'firefox':
              view.mozRequestFullScreen();
              break;
            case 'chrome':
            case 'safari':
              view.webkitRequestFullScreen();
              break;
          }
        }
      };


      this.isFormValid = function(dataAccessForm) {
        var isJsonValid = function(json) {
          try {
            JSON.parse(json);
          } catch (e) {
            return false;
          }
          return true;
        };

        return isJsonValid(dataAccessForm.definition) && isJsonValid(dataAccessForm.schema);
      }

      return this;
    }]);
