'use strict';

mica.config

  .factory('MicaConfigResource', ['$resource',
    function ($resource) {
      return $resource('ws/config', {}, {
        // override $resource.save method because it uses POST by default
        'save': {method: 'PUT'},
        'get': {method: 'GET'}
      });
    }])
  .factory('KeyStoreResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/keystore/system/https', {}, {
        'save': {method: 'PUT'}
      });
    }])
  .factory('OpalCredentialsResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/opal-credentials', {}, {});
    }])
  .factory('OpalCredentialResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/opal-credential/:id', {id: '@id'}, {});
    }])
  .factory('StyleEditorService', ['BrowserDetector',
    function (BrowserDetector) {
      return {
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
              mode: 'css',
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
        }
      };
    }]);
