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
        'save': {method: 'POST', errorHandler: true, transformRequest: StudyModelService.serialize},
        'index': {method: 'PUT', url: '/ws/draft/individual-studies/_index', params:{id:'@id'}}
      });
    }])

  .factory('DraftStudyResource', ['$resource', 'StudyModelService',
    function ($resource, StudyModelService) {
      return $resource(contextPath + '/ws/draft/individual-study/:id', {id: '@id'}, {
        // override $resource.save method because it uses POST by default
        'save': {method: 'PUT', errorHandler: true, transformRequest: StudyModelService.serialize},
        'rSave': {method: 'PUT', errorHandler: true, transformRequest: StudyModelService.serializeForRestoringFields},
        'delete': {method: 'DELETE', errorHandler: true},
        'get': {method: 'GET', transformResponse: StudyModelService.deserialize},
        'rGet': {method: 'GET', transformResponse: StudyModelService.deserializeForRestoringFields},
        'indexDatasets': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/index-datasets'},
        'publish': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/_publish', params: {id: '@id', cascading: '@cascading'}},
        'unPublish': {method: 'DELETE', url: contextPath + '/ws/draft/individual-study/:id/_publish', errorHandler: true},
        'toStatus': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/_status', params: {id: '@id', value: '@value'}},
        'tag': {method: 'PUT', url: contextPath + '/ws/draft/individual-study/:id/attributes', params: {id: '@id'}}
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

    const PERSISTABLE_DATE_REGEXP = /(\d{4})-?(\d{1,2})?/g;

    this.serialize = function (study) {
      return serialize(study, false);
    };

    this.deserialize = function (studyData) {
      return deserialize(studyData, false);
    };

    this.serializeForRestoringFields = function (study) {
      return serialize(study, true);
    };

    this.deserializeForRestoringFields = function (studyData) {
      return deserialize(studyData, true);
    };

    function serialize (study, restore) {

      var studyCopy = angular.copy(study);

      if (!restore) {
        studyCopy.name = LocalizedValues.objectToArray(studyCopy.model._name);
        studyCopy.acronym = LocalizedValues.objectToArray(studyCopy.model._acronym);
        studyCopy.objectives = LocalizedValues.objectToArray(studyCopy.model._objectives);
        studyCopy.opal = studyCopy.model._opal;
        delete studyCopy.model._name;
        delete studyCopy.model._acronym;
        delete studyCopy.model._objectives;
        delete studyCopy.model._opal;
      } else {
        studyCopy.name = LocalizedValues.objectToArray(studyCopy.name);
        studyCopy.acronym = LocalizedValues.objectToArray(studyCopy.acronym);
        studyCopy.objectives = LocalizedValues.objectToArray(studyCopy.objectives);
      }

      studyCopy.content = studyCopy.model ? angular.toJson(studyCopy.model) : null;

      if (studyCopy.logo) {
        // Remove fields not in the DTO
        delete studyCopy.logo.showProgressBar;
        delete studyCopy.logo.progress;
      }

      delete studyCopy.model; // NOTICE: must be removed to avoid protobuf exception in dto.

      if(studyCopy.populations) {
        studyCopy.populations.forEach(function(population) {
          populationSerialize(population, restore);
        });
      }

      return angular.toJson(studyCopy);
    }

    function populationSerialize(population, restore) {
      if (!restore) {
        population.id = population.model._id;
        population.name = LocalizedValues.objectToArray(population.model._name);
        population.description = LocalizedValues.objectToArray(population.model._description);
        delete population.model._id;
        delete population.model._name;
        delete population.model._description;
      } else {
        population.name = LocalizedValues.objectToArray(population.name);
        population.description = LocalizedValues.objectToArray(population.description);
      }

      population.content = population.model ? angular.toJson(population.model) : null;

      delete population._active;
      delete population.model;

      if(population.dataCollectionEvents) {
        population.dataCollectionEvents.forEach(function(dce) {
          dceSerialize(dce, restore);
        });
      }
    }

    function dceSerialize(dce, restore) {
      if (!restore) {
        dce.id = dce.model._id;
        dce.name = LocalizedValues.objectToArray(dce.model._name);
        dce.description = LocalizedValues.objectToArray(dce.model._description);
        dce.startYear = dce.model._startYear;
        dce.startMonth = dce.model._startMonth;
        dce.startDay = dce.model._startDay;
        dce.endYear = dce.model._endYear;
        dce.endMonth = dce.model._endMonth;
        dce.endDay = dce.model._endDay;
        delete dce.model._id;
        delete dce.model._name;
        delete dce.model._description;
        delete dce.model._startYear;
        delete dce.model._startMonth;
        delete dce.model._startDay;
        delete dce.model._endYear;
        delete dce.model._endMonth;
        delete dce.model._endDay;
      } else {
        dce.name = LocalizedValues.objectToArray(dce.name);
        dce.description = LocalizedValues.objectToArray(dce.description);
        const persistableStartYearMonthData = getPersistableYearMonth(dce.start);
        const persistableEndYearMonthData = getPersistableYearMonth(dce.end);

        if (persistableStartYearMonthData) {
          dce.startYear = persistableStartYearMonthData.year;
          if (persistableStartYearMonthData.month && typeof persistableStartYearMonthData.month === 'number') {
            dce.startMonth = persistableStartYearMonthData.month;
          } else {
            delete dce.startMonth;
          }

          dce.startDay = persistableStartYearMonthData.day;

          delete dce.start;
        }

        if (persistableEndYearMonthData) {
          dce.endYear = persistableEndYearMonthData.year;
          if (persistableEndYearMonthData.month && typeof persistableEndYearMonthData.month === 'number') {
            dce.endMonth = persistableEndYearMonthData.month;
          } else {
            delete dce.endMonth;
          }

          dce.endDay = persistableEndYearMonthData.day;

          delete dce.end;
        }
      }

      dce.content = dce.model ? angular.toJson(dce.model) : null;

      delete dce._active;
      delete dce.model;
    }

    function deserialize (studyData, restore) {
      var study = angular.fromJson(studyData);
      study.model = study.content ? angular.fromJson(study.content) : {};

      if (!restore) {
        study.model._name = LocalizedValues.arrayToObject(study.name);
        study.model._acronym = LocalizedValues.arrayToObject(study.acronym);
        study.model._objectives = LocalizedValues.arrayToObject(study.objectives);
        study.model._opal = study.opal;
      } else {
        study.name = LocalizedValues.arrayToObject(study.name);
        study.acronym = LocalizedValues.arrayToObject(study.acronym);
        study.objectives = LocalizedValues.arrayToObject(study.objectives);
      }

      if (study.populations) {
        study.populations.forEach(function (population) {
          populationDeserialize(population, restore);
        });
      }

      return study;
    }

    function populationDeserialize(population, restore) {
      population.model = population.content ? angular.fromJson(population.content) : {};

      if (!restore) {
        population.model._id = population.id;
        population.model._name = LocalizedValues.arrayToObject(population.name);
        population.model._description = LocalizedValues.arrayToObject(population.description);
      } else {
        population.name = LocalizedValues.arrayToObject(population.name);
        population.description = LocalizedValues.arrayToObject(population.description);
      }

      if (population.dataCollectionEvents) {
        population.dataCollectionEvents.forEach(function (dce) {
          dceDeserialize(dce, restore);
        });
      }
    }

    function dceDeserialize(dce, restore) {
      dce.model = dce.content ? angular.fromJson(dce.content) : {};

      if (!restore) {
        dce.model._id = dce.id;
        dce.model._name = LocalizedValues.arrayToObject(dce.name);
        dce.model._description = LocalizedValues.arrayToObject(dce.description);
        dce.model._startYear = dce.startYear;
        dce.model._startMonth = dce.startMonth;
        dce.model._startDay = dce.startDay;
        dce.model._endYear = dce.endYear;
        dce.model._endMonth = dce.endMonth;
        dce.model._endDay = dce.endDay;
      } else {
        dce.name = LocalizedValues.arrayToObject(dce.name);
        dce.description = LocalizedValues.arrayToObject(dce.description);
      }

    }

    function getPersistableYearMonth(dceDate) {
      const date = (dceDate || {});
      const yearMonthField = date.yearMonth;
      const dayField = (date.day || '').length > 0 ? date.day : null;
      let found = PERSISTABLE_DATE_REGEXP.exec(yearMonthField || '');
      if (found || dayField !== null) {
        const result = {};

        if (found) {
          result.year = parseInt(found[1]);
          if (found[2]) {
            result.month = parseInt(found[2]);
          }
        }

        if (dayField) {
          result.day = dayField;
          // Validate year and month in case those fields were not restored
          if (!result.year && !result.month) {
            found = PERSISTABLE_DATE_REGEXP.exec(result.day);
            result.year = parseInt(found[1]);
            result.month = parseInt(found[2]);
          }
        }

        return result;
      }

      return null;
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
        'rSave': {method: 'PUT', errorHandler: true, transformRequest: StudyModelService.serializeForRestoringFields},
        'delete': {method: 'DELETE', errorHandler: true},
        'get': {method: 'GET', transformResponse: StudyModelService.deserialize},
        'rGet': {method: 'GET', transformResponse: StudyModelService.deserializeForRestoringFields},
        'publish': {method: 'PUT', url: contextPath + '/ws/draft/harmonization-study/:id/_publish', params: {id: '@id', cascading: '@cascading'}},
        'unPublish': {method: 'DELETE', url: contextPath + '/ws/draft/harmonization-study/:id/_publish', errorHandler: true},
        'toStatus': {method: 'PUT', url: contextPath + '/ws/draft/harmonization-study/:id/_status', params: {id: '@id', value: '@value'}},
        'tag': {method: 'PUT', url: contextPath + '/ws/draft/harmonization-study/:id/attributes', params: {id: '@id'}}
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
    }])

  .factory('TaxonomyFilterResource', ['$resource', function ($resource) {
    return $resource(contextPath + 'ws/taxonomies/_filter', {target: 'variable'});
  }]);
