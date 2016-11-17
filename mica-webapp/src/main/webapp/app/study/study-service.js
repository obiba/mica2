/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

  .factory('DraftStudiesResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource('ws/draft/studies?comment:comment', {}, {
        'save': {method: 'POST', errorHandler: true, transformRequest: StudyModelService.serialize}
      });
    }])

  .factory('DraftStudyResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource('ws/draft/study/:id', {}, {
        // override $resource.save method because it uses POST by default
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: StudyModelService.serialize},
        'delete': {method: 'DELETE', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET', transformResponse: StudyModelService.deserialize}
      });
    }])

  .factory('StudyModelService', ['LocalizedValues', function (LocalizedValues) {

    this.serialize = function (study) {

      var studyCopy = angular.copy(study);

      studyCopy.name = LocalizedValues.objectToArray(studyCopy.model._name);
      studyCopy.acronym = LocalizedValues.objectToArray(studyCopy.model._acronym);
      studyCopy.objectives = LocalizedValues.objectToArray(studyCopy.model._objectives);
      studyCopy.opal = studyCopy.model._opal;
      delete studyCopy.model._name;
      delete studyCopy.model._acronym;
      delete studyCopy.model._objectives;
      delete studyCopy.model._opal;
      studyCopy.content = studyCopy.model ? angular.toJson(studyCopy.model) : null;
      delete studyCopy.model; // NOTICE: must be removed to avoid protobuf exception in dto.

      if(studyCopy.populations) {
        studyCopy.populations.forEach(function(population) {
          populationSerialize(population);
        });
      }

      return angular.toJson(studyCopy);
    };

    function populationSerialize(population) {
      population.id = population.model._id;
      population.name = LocalizedValues.objectToArray(population.model._name);
      population.description = LocalizedValues.objectToArray(population.model._description);
      population.content = population.model ? angular.toJson(population.model) : null;

      delete population.model;

      if(population.dataCollectionEvents) {
        population.dataCollectionEvents.forEach(function(dce) {
          dceSerialize(dce);
        });
      }
    }

    function dceSerialize(dce) {
      dce.id = dce.model._id;
      dce.name = LocalizedValues.objectToArray(dce.model._name);
      dce.startYear = dce.model._startYear;
      dce.startMonth = dce.model._startMonth;
      dce.endYear = dce.model._endYear;
      dce.endMonth = dce.model._endMonth;
      dce.content = dce.model ? angular.toJson(dce.model) : null;

      delete dce.model;
    }

    this.deserialize = function (studyData) {
      var study = angular.fromJson(studyData);
      study.model = study.content ? angular.fromJson(study.content) : {};
      study.model._name = LocalizedValues.arrayToObject(study.name);
      study.model._acronym = LocalizedValues.arrayToObject(study.acronym);
      study.model._objectives = LocalizedValues.arrayToObject(study.objectives);
      study.model._opal = study.opal;

      if (study.populations) {
        study.populations.forEach(function (population) {
          populationDeserialize(population);
        });
      }
      return study;
    };

    function populationDeserialize(population) {
      population.model = population.content ? angular.fromJson(population.content) : {};
      population.model._id = population.id;
      population.model._name = LocalizedValues.arrayToObject(population.name);
      population.model._description = LocalizedValues.arrayToObject(population.description);

      if (population.dataCollectionEvents) {
        population.dataCollectionEvents.forEach(function (dce) {
          dceDeserialize(dce);
        });
      }
    }

    function dceDeserialize(dce) {
      dce.model = dce.content ? angular.fromJson(dce.content) : {};
      dce.model._id = dce.id;
      dce.model._name = LocalizedValues.arrayToObject(dce.name);
      dce.model._startYear = dce.startYear;
      dce.model._startMonth = dce.startMonth;
      dce.model._endYear = dce.endYear;
      dce.model._endMonth = dce.endMonth;
    }

    return this;
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

  .factory('DraftStudyAccessesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study/:id/accesses', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', file: '@file'},
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

  .factory('DraftStudyViewRevisionResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource('ws/draft/study/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET', params: {id: '@id', commitId: '@commitId'}, transformResponse: StudyModelService.deserialize}
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
      var studyTaxonomy = null;

      function getLocalizedLabel(localizedString, lang) {
        var result = localizedString.filter(function (t) {
          return t.locale === lang;
        });

        //TODO
        return result;
        // return (result || [{text: null}])[0].text;
      }

      this.get = function(userSuccessCallback, userErrorCallback) {

        MicaStudiesConfigResource.get().$promise.then(
          function onSuccess(response) {
            studyTaxonomy = response.pop();

            if (studyTaxonomy){
              if (userSuccessCallback) {
                userSuccessCallback();
              }
            } else if (userErrorCallback) {
              userErrorCallback();
            }

          },
          userErrorCallback
        );
      };

      this.getLabel = function (vocabulary, term, lang) {
        if (!studyTaxonomy || !vocabulary || !term) {
          return;
        }

        var result = studyTaxonomy.vocabularies.filter(function (v) {
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
        var opts = studyTaxonomy.vocabularies.map(function (v) {
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

    }])

  .service('StudyModelUtil', [function() {
    this.updateContents = function() {};

    return this;
  }]);
