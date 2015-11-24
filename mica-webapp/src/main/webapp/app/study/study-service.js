'use strict';

mica.study
  .factory('StudyStatesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-states');
    }])

  .factory('StudyStateResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-state/:id', {}, {
        'get': {method: 'GET'}
      });
    }])

  .factory('DraftStudiesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/studies?comment:comment', {}, {
        'save': {method: 'POST', errorHandler: true}
      });
    }])

  .factory('DraftStudyResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id', {}, {
        // override $resource.save method because it uses POST by default
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'delete': {method: 'DELETE', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('DraftStudyPermissionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', role: '@role'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftStudyPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/_publish', {id: '@id'}, {
        'publish': {method: 'PUT', params: {cascading: '@cascading'}},
        'unPublish': {method: 'DELETE'}
      });
    }])

  .factory('DraftStudyStatusResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/_status', {}, {
        'toStatus': {method: 'PUT', params: {id: '@id', value: '@value'}}
      });
    }])

  .factory('DraftStudyRevisionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/commits', {}, {
        'get': {method: 'GET', params: {id: '@id'}}
      });
    }])

  .factory('DraftStudyRestoreRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/commit/:commitId/restore', {}, {
        'restore': {method: 'PUT', params: {id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftStudyViewRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET', params: {id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('MicaStudiesConfigResource', ['$resource',
    function ($resource) {
      return $resource('ws/taxonomies/_filter', {target: 'study'}, {
        'get': {method: 'GET', isArray: true}
      });
    }])

  .factory('DraftStudiesSummariesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/studies/summaries?', {}, {
        'summaries': {method: 'GET', isArray: true, params: {id: '@id'}}
      });
    }])

  .service('StudyTaxonomyService', ['MicaStudiesConfigResource',
    function(MicaStudiesConfigResource) {
      var studiesConfig = null;

      function getLocalizedLabel(localizedString, lang) {
        var result = localizedString.filter(function (t) {
          return t.locale === lang;
        });

        return (result || [{text: null}])[0].text;
      }


      this.get = function(userSuccessCallback, userErrorCallback) {
        MicaStudiesConfigResource.get().$promise.then(
          function onSuccess(taxonomies) {
            if (taxonomies) {
              var result = taxonomies.filter(function(taxonomy) {
                return 'Mlstr_study' === taxonomy.name;
              });

              studiesConfig = result && result.length > 0 ? result[0] : null;

              if (studiesConfig){
                if (userSuccessCallback) {
                  userSuccessCallback();
                }
              } else if (userErrorCallback) {
                userErrorCallback();
              }

            }
          },
          userErrorCallback
        );
      };

      this.getLabel = function (vocabulary, term, lang) {
        if (!studiesConfig || !vocabulary || !term) {
          return;
        }

        var result = studiesConfig.vocabularies.filter(function (v) {
          return v.name === vocabulary;
        });

        if (result.length > 0) {
          result = result[0].terms.filter(function (t) {
            return t.name === term;
          });

          if (result.length > 0) {
            result = getLocalizedLabel(result[0].title, lang);

            if (result) {
              return result;
            }
          }
        }

        // could not find the term, return non-localized term value
        return term;
      };

      this.getTerms = function(vocabulary, lang) {
        var opts = studiesConfig.vocabularies.map(function (v) {
          if (v.name === vocabulary) {
            return v.terms.map(function (t) {
              return {name: t.name, label: getLocalizedLabel(t.title, lang) || t.name};
            });
          }
        }).filter(function (x) { return x; });

        return opts ? opts[0] : [];
      };

      return this;
    }])

  .factory('DraftStudyDeleteService', [
    '$rootScope',
    '$translate',
    '$interpolate',
    'NOTIFICATION_EVENTS',
    'DraftStudyResource',

    function($rootScope, $translate, $interpolate, NOTIFICATION_EVENTS, DraftStudyResource) {

      var factory = {};

      var getNames = function(study) {
        return study.name.map(function(entry) {
          return entry.value;
        }).join('-');
      };

      var getName = function(study, lang) {
        return study.name.filter(function(value) {
          return value.lang === lang;
        })[0].value;
      };

      factory.delete = function(study, onSuccess, lang) {
        factory.studyToDelete = study.id;
        factory.onSuccess = onSuccess;

        var messageArgs = lang ? getName(study, lang) : getNames(study);

        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'study.restore-dialog.title',
            messageKey: 'study.delete-dialog.message',
            messageArgs: [messageArgs]
          }, study.id
        );
      };

      $rootScope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if (factory.studyToDelete === id) {
          DraftStudyResource.delete({id: id},
            function () {
              if (factory.onSuccess) {
                factory.onSuccess();
              }
            }, function (response) {
              if (response.status === 409) {
                var conflicts = '{{network ? networks + ": " + network + ". " : "" }}' +
                  '{{harmonizationDataset ? harmonizationDatasets + ": " + harmonizationDataset + ". " : "" }}' +
                  '{{studyDataset ? studyDatasets + ": " + studyDataset : "" }}';

                $translate(['study.delete-conflict-message', 'networks', 'study-datasets', 'harmonization-datasets'])
                  .then(function (translation) {
                    $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                      titleKey: 'study.delete-conflict',
                      message: translation['study.delete-conflict-message'] + ' ' + $interpolate(conflicts)(
                        {
                          networks: translation.networks,
                          harmonizationDatasets: translation['harmonization-datasets'],
                          studyDatasets: translation['study-datasets'],
                          network: response.data.network.join(', '),
                          harmonizationDataset: response.data.harmonizationDataset.join(', '),
                          studyDataset: response.data.studyDataset.join(', ')
                        })
                    });
                  });
              } else {
                $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                  titleKey: 'form-server-error',
                  message: angular.toJson(response)
                });
              }
            });
        }

        delete factory.studyToDelete;
      });

      return factory;

    }]);
