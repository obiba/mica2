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
  .factory('StudyStatesSearchResource',['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/study-states', {}, {
        'query': {method: 'GET', errorHandler: true, isArray: true},
      });
    }])

  .factory('StudyStatesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/study-states?type=:type', {}, {
        'query': {method: 'GET', params: {type: 'individual-study'}, errorHandler: true, isArray: true},
        'get': {method: 'GET', url: contextPath + '/ws/draft/study-state/:id', params: {id: '@id'}}
      });
    }])

  .factory('DraftStudiesResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource(contextPath + '/ws/draft/individual-studies?comment:comment', {}, {
        'save': {method: 'POST', errorHandler: true, transformRequest: StudyModelService.serialize}
      });
    }])

  .factory('DraftStudyResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource(contextPath + '/ws/draft/individual-study/:id', {id: '@id'}, {
        // override $resource.save method because it uses POST by default
        'save': {method: 'PUT', errorHandler: true, transformRequest: StudyModelService.serialize},
        'rSave': {method: 'PUT', errorHandler: true, transformRequest: StudyModelService.simpleSerialize},
        'delete': {method: 'DELETE', errorHandler: true},
        'get': {method: 'GET', transformResponse: StudyModelService.deserialize},
        'rGet': {method: 'GET', transformResponse: StudyModelService.simpleDeserialize},
        'indexDatasets': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/index-datasets'},
        'publish': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/_publish', params: {id: '@id', cascading: '@cascading'}},
        'unPublish': {method: 'DELETE', url: contextPath + '/ws/draft/individual-study/:id/_publish', errorHandler: true},
        'toStatus': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/_status', params: {id: '@id', value: '@value'}}
      });
    }])

  .factory('DraftStudyPermissionsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/individual-study/:id/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', role: '@role', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftStudyAccessesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/individual-study/:id/accesses', {}, {
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

  .factory('DraftStudyRevisionsResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource(contextPath + '/ws/draft/individual-study/:id/commits', {}, {
        'get': {method: 'GET', params: {id: '@id'}},
        'restore': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/commit/:commitId/restore', params: {id: '@id', commitId: '@commitId'}},
        'view': {method: 'GET', url: contextPath + '/ws/draft/individual-study/:id/commit/:commitId/view', params: {id: '@id', commitId: '@commitId'},
          transformResponse: StudyModelService.deserialize},
        'diff': {method: 'GET', url: contextPath + '/ws/draft/individual-study/:id/_diff', params: {id: '@id'}}
      });
    }])

  .factory('MicaStudiesConfigResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/taxonomies/_filter', {target: 'study'}, {
        'get': {method: 'GET', isArray: true}
      });
    }])

  .factory('DraftStudiesSummariesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/individual-studies/summaries?', {}, {
        'summaries': {method: 'GET', isArray: true, params: {id: '@id'}}
      });
    }])

  .factory('StudyModelService', ['LocalizedValues', function (LocalizedValues) {

    this.serialize = function (study) {
      return serialize(study, true);
    };

    this.deserialize = function (studyData) {
      return deserialize(studyData, true);
    };

    this.simpleSerialize = function (study) {
      return serialize(study, false);
    };

    this.simpleDeserialize = function (studyData) {
      return deserialize(studyData, false);
    };

    function serialize (study, all) {

      var studyCopy = angular.copy(study);

      if (all) {
        studyCopy.name = LocalizedValues.objectToArray(studyCopy.model._name);
        studyCopy.acronym = LocalizedValues.objectToArray(studyCopy.model._acronym);
        studyCopy.objectives = LocalizedValues.objectToArray(studyCopy.model._objectives);
        studyCopy.opal = studyCopy.model._opal;
        delete studyCopy.model._name;
        delete studyCopy.model._acronym;
        delete studyCopy.model._objectives;
        delete studyCopy.model._opal;
      }

      delete studyCopy.$promise;
      delete studyCopy.$resolved;

      studyCopy.content = studyCopy.model ? angular.toJson(studyCopy.model) : null;

      delete studyCopy.model; // NOTICE: must be removed to avoid protobuf exception in dto.

      if(studyCopy.populations) {
        studyCopy.populations.forEach(function(population) {
          populationSerialize(population, all);
        });
      }

      return angular.toJson(studyCopy);
    }

    function populationSerialize(population, all) {
      if (all) {
        population.id = population.model._id;
        population.name = LocalizedValues.objectToArray(population.model._name);
        population.description = LocalizedValues.objectToArray(population.model._description);
        delete population.model._id;
        delete population.model._name;
        delete population.model._description;
      }

      population.content = population.model ? angular.toJson(population.model) : null;

      delete population.model;

      if(population.dataCollectionEvents) {
        population.dataCollectionEvents.forEach(function(dce) {
          dceSerialize(dce, all);
        });
      }
    }

    function dceSerialize(dce, all) {
      if (all) {
        dce.id = dce.model._id;
        dce.name = LocalizedValues.objectToArray(dce.model._name);
        dce.description = LocalizedValues.objectToArray(dce.model._description);
        dce.startYear = dce.model._startYear;
        dce.startMonth = dce.model._startMonth;
        dce.endYear = dce.model._endYear;
        dce.endMonth = dce.model._endMonth;
        delete dce.model._id;
        delete dce.model._name;
        delete dce.model._description;
        delete dce.model._startYear;
        delete dce.model._startMonth;
        delete dce.model._endYear;
        delete dce.model._endMonth;
      }

      dce.content = dce.model ? angular.toJson(dce.model) : null;

      delete dce.model;
    }

    function deserialize (studyData, all) {
      var study = angular.fromJson(studyData);
      study.model = study.content ? angular.fromJson(study.content) : {};

      if (all) {
        study.model._name = LocalizedValues.arrayToObject(study.name);
        study.model._acronym = LocalizedValues.arrayToObject(study.acronym);
        study.model._objectives = LocalizedValues.arrayToObject(study.objectives);
        study.model._opal = study.opal;
      }

      if (study.populations) {
        study.populations.forEach(function (population) {
          populationDeserialize(population, all);
        });
      }

      return study;
    }

    function populationDeserialize(population, all) {
      population.model = population.content ? angular.fromJson(population.content) : {};

      if (all) {
        population.model._id = population.id;
        population.model._name = LocalizedValues.arrayToObject(population.name);
        population.model._description = LocalizedValues.arrayToObject(population.description);
      }

      if (population.dataCollectionEvents) {
        population.dataCollectionEvents.forEach(function (dce) {
          dceDeserialize(dce, all);
        });
      }
    }

    function dceDeserialize(dce, all) {
      dce.model = dce.content ? angular.fromJson(dce.content) : {};

      if (all) {
        dce.model._id = dce.id;
        dce.model._name = LocalizedValues.arrayToObject(dce.name);
        dce.model._description = LocalizedValues.arrayToObject(dce.description);
        dce.model._startYear = dce.startYear;
        dce.model._startMonth = dce.startMonth;
        dce.model._endYear = dce.endYear;
        dce.model._endMonth = dce.endMonth;
      }

    }

    return this;
  }])

  .service('StudyTaxonomyService', ['MicaStudiesConfigResource',
    function(MicaStudiesConfigResource) {
      var studyTaxonomy = null;

      function getLocalizedLabel(localizedString, lang) {
        var result = localizedString.filter(function (t) {
          return t.locale === lang;
        });

        return (result || [{text: null}])[0].text;
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
    'StudyUpdateWarningService',

    function($rootScope, $translate, $interpolate, NOTIFICATION_EVENTS, DraftStudyResource, StudyUpdateWarningService) {

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
            titleKey: 'study.delete-dialog.title',
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
                StudyUpdateWarningService.popup(response.data, 'study.delete-conflict', 'study.delete-conflict-message');
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

  .factory('DraftHarmonizationStudyDeleteService', [
    '$rootScope',
    '$translate',
    '$interpolate',
    'NOTIFICATION_EVENTS',
    'DraftHarmonizationStudyResource',
    'StudyUpdateWarningService',

    function($rootScope, $translate, $interpolate, NOTIFICATION_EVENTS, DraftHarmonizationStudyResource, StudyUpdateWarningService) {

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
            titleKey: 'study.delete-dialog.title',
            messageKey: 'study.delete-dialog.message',
            messageArgs: [messageArgs]
          }, study.id
        );
      };

      $rootScope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if (factory.studyToDelete === id) {
          DraftHarmonizationStudyResource.delete({id: id},
            function () {
              if (factory.onSuccess) {
                factory.onSuccess();
              }
            }, function (response) {
              if (response.status === 409) {
                StudyUpdateWarningService.popup(response.data, 'study.delete-conflict', 'study.delete-conflict-message');
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

  .service('StudyUpdateWarningService', ['$rootScope', '$translate', '$interpolate', 'NOTIFICATION_EVENTS',
    function ($rootScope, $translate, $interpolate, NOTIFICATION_EVENTS) {
      this.popup = function (data, title, message, callback) {
        var conflicts = '{{network ? networks + ": " + network + ". " : "" }}' +
          '{{harmonizedDataset ? harmonizedDatasets + ": " + harmonizedDataset + ". " : "" }}' +
          '{{collectedDataset ? collectedDatasets + ": " + collectedDataset : "" }}';

        $translate([message, 'networks', 'collected-datasets', 'harmonized-datasets'])
          .then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
              titleKey: title,
              message: translation[message] + ' ' + $interpolate(conflicts)(
                {
                  networks: translation.networks,
                  harmonizedDatasets: translation['harmonized-datasets'],
                  collectedDatasets: translation['collected-datasets'],
                  network: data.network ? data.network.join(', ') : null,
                  harmonizedDataset: data.harmonizedDataset ? data.harmonizedDataset.join(', ') : null,
                  collectedDataset: data.studyDataset ? data.studyDataset.join(', ') : null
                })
            }, function () {
              if (callback) {
                callback();
              }
            });
          });
      };
    }])

  .factory('HarmonizationStudyStatesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/study-states?type=harmonization-study', {}, {
        'query': {method: 'GET', errorHandler: true, isArray: true},
        'get': {method: 'GET', url: contextPath + '/ws/draft/study-state/:id', params: {id: '@id'}}
      });
    }])

  .factory('DraftHarmonizationStudiesResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource(contextPath + '/ws/draft/harmonization-studies?comment:comment', {}, {
        'save': {method: 'POST', errorHandler: true, transformRequest: StudyModelService.serialize}
      });
    }])

  .factory('DraftHarmonizationStudyResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource(contextPath + '/ws/draft/harmonization-study/:id', {id: '@id'}, {
        // override $resource.save method because it uses POST by defaultt
        'save': {method: 'PUT', errorHandler: true, transformRequest: StudyModelService.serialize},
        'rSave': {method: 'PUT', errorHandler: true, transformRequest: StudyModelService.simpleSerialize},
        'delete': {method: 'DELETE', errorHandler: true},
        'get': {method: 'GET', transformResponse: StudyModelService.deserialize},
        'rGet': {method: 'GET', transformResponse: StudyModelService.simpleDeserialize},
        'publish': {method: 'PUT', url: contextPath + '/ws/draft/harmonization-study/:id/_publish', params: {id: '@id', cascading: '@cascading'}},
        'unPublish': {method: 'DELETE', url: contextPath + '/ws/draft/harmonization-study/:id/_publish', errorHandler: true},
        'toStatus': {method: 'PUT', url: contextPath + '/ws/draft/harmonization-study/:id/_status', params: {id: '@id', value: '@value'}}
      });
    }])

  .factory('DraftHarmonizationStudyPermissionsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/harmonization-study/:id/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', role: '@role', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftHarmonizationStudyAccessesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/harmonization-study/:id/accesses', {}, {
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

  .factory('DraftHarmonizationStudyRevisionsResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource(contextPath + '/ws/draft/harmonization-study/:id/commits', {}, {
        'get': {method: 'GET', params: {id: '@id'}},
        'restore': {method: 'PUT', url: contextPath + '/ws/draft/harmonization-study/:id/commit/:commitId/restore', params: {id: '@id', commitId: '@commitId'}},
        'view': {method: 'GET', url: contextPath + '/ws/draft/harmonization-study/:id/commit/:commitId/view', params: {id: '@id', commitId: '@commitId'},
          transformResponse: StudyModelService.deserialize},
        'diff': {method: 'GET', url: contextPath + '/ws/draft/harmonization-study/:id/_diff', params: {id: '@id'}}
      });
    }])

  .factory('MicaHarmonizationStudiesConfigResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/taxonomies/_filter', {target: 'study'}, {
        'get': {method: 'GET', isArray: true}
      });
    }])

  .factory('DraftHarmonizationStudiesSummariesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/harmonization-studies/summaries?', {}, {
        'summaries': {method: 'GET', isArray: true, params: {id: '@id'}}
      });
    }]);
