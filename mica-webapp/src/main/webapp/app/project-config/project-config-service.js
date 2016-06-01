/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* global document */
'use strict';

mica.projectConfig

  .factory('ProjectFormResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/project-form', {}, {
        'get': {method: 'GET', errorHandler: true},
        'save': {method: 'PUT', errorHandler: true}
      });
    }])
  
  .factory('ProjectFormPermissionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/project-form/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {draft: '@draft', type: '@type', principal: '@principal', role: '@role'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {draft: '@draft', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET', params: {draft: '@draft'}, isArray: true}
      });
    }])

  .factory('ProjectFormService', ['BrowserDetector',
    function (BrowserDetector) {
      return {

        ParseResult: {
          VALID: 1,
          DEFINITION: 0,
          SCHEMA: -1
        },

        /**
         * HACK until angular-ui-ce can config path settings
         */
        configureAcePaths: function () {
          var defaultPath = ace.config.get('basePath');

          if (defaultPath.indexOf('bower_components') === -1) {
            // production path must be changed
            ace.config.set('basePath', '/scripts');
            ace.config.set('modePath', '/scripts');
            ace.config.set('themePath', '/scripts');
            ace.config.set('workerPath', '/scripts');
          }
        },

        getEditorOptions: function (onLoadCallback) {
          return {
            options: {
              theme: 'monokai',
              mode: 'json',
              displayIndentGuides: true,
              useElasticTabstops: true,
              onLoad: onLoadCallback
            }
          };
        },

        gotoFullScreen: function (id) {
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
        },

        parseJsonSafely: function(json, defaultValue) {
          try {
            return JSON.parse(json);
          } catch (e) {
            return defaultValue;
          }
        },

        prettifyJson: function (jsonData) {
          var str = typeof jsonData === 'string' ? jsonData : JSON.stringify(jsonData, undefined, 2);
          return str;
        },

        isJsonValid: function (json) {
          try {
            JSON.parse(json);
          } catch (e) {
            return false;
          }
          return true;
        },

        isFormValid: function(projectForm) {
          var result = this.isJsonValid(projectForm.definition) ?
            (this.isJsonValid(projectForm.schema) ? this.ParseResult.VALID : this.ParseResult.SCHEMA) :
            this.ParseResult.DEFINITION;

          return result;
        }
      };
    }]);
