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

/* global JSON */

mica.dataset
  .factory('CollectedDatasetsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/collected-datasets', {}, {
        'query': {method: 'GET', errorHandler: true, isArray: true},
        'index': {method: 'PUT', url: contextPath + '/ws/draft/collected-datasets/_index', params: {id: '@id'}, errorHandler: true},
        'delete': {method: 'DELETE', url: contextPath + '/ws/draft/collected-dataset/:id', params: {id: '@id'}, errorHandler: true}
      });
    }])

  .factory('DraftCollectedDatasetsResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource(contextPath + '/ws/draft/collected-datasets?comment:comment', {}, {
        'save': {method: 'POST', errorHandler: true, transformRequest: DatasetModelService.serialize}
      });
    }])

  .factory('CollectedDatasetResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource(contextPath + '/ws/draft/collected-dataset/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: DatasetModelService.serialize},
        'get': {method: 'GET', transformResponse: DatasetModelService.deserialize},
        'rSave': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: DatasetModelService.serializeForRestoringFields},
        'rGet': {method: 'GET', transformResponse: DatasetModelService.deserializeForRestoringFields}
      });
    }])

  .factory('CollectedDatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/collected-dataset/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id'}},
        'handledPublish': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
      });
    }])

  .factory('StudyStateOpalProjectsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/study-state/:id/opal-projects', {}, {
        'get': {method: 'GET', params: {id: '@id'}}
      });
    }])

  .factory('HarmonizedDatasetsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/harmonized-datasets', {}, {
        'query': {method: 'GET', errorHandler: true, isArray: true},
        'index': {method: 'PUT', url: contextPath + '/ws/draft/harmonized-datasets/_index', params: {id: '@id'}, errorHandler: true},
        'delete': {method: 'DELETE', url: contextPath + '/ws/draft/harmonized-dataset/:id', params: {id: '@id'}, errorHandler: true}
      });
    }])

  .factory('DraftHarmonizedDatasetsResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource(contextPath + '/ws/draft/harmonized-datasets', {}, {
        'save': {method: 'POST', errorHandler: true, transformRequest: function(dataset) {
          return DatasetModelService.serialize(dataset);
        }}
      });
    }])

  .factory('HarmonizedDatasetResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource(contextPath + '/ws/draft/harmonized-dataset/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: DatasetModelService.serialize},
        'get': {method: 'GET', transformResponse: DatasetModelService.deserialize},
        'rSave': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: DatasetModelService.serializeForRestoringFields},
        'rGget': {method: 'GET', transformResponse: DatasetModelService.deserializeForRestoringFields}
      });
    }])

  .factory('HarmonizedDatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/harmonized-dataset/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id'}},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
      });
    }])

  .factory('DatasetResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource(contextPath + '/ws/draft/:type/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id', type: '@type'}, errorHandler: true, transformRequest: DatasetModelService.serialize},
        'rSave': {method: 'PUT', params: {id: '@id', type: '@type'}, errorHandler: true, transformRequest: DatasetModelService.serializeForRestoringFields},
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type'}, errorHandler: true},
        'get': {method: 'GET', params: {id: '@id', type: '@type'}, transformResponse: DatasetModelService.deserialize},
        'rGet': {method: 'GET', params: {id: '@id', type: '@type'}, transformResponse: DatasetModelService.deserializeForRestoringFields}
      });
    }])

  .factory('DatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/:type/:id/_publish', {id: '@id', type: '@type'}, {
        'publish': {method: 'PUT', params: {cascading: '@cascading'}},
        'handledPublish': {method: 'PUT', params: {cascading: '@cascading'}, errorHandler: true},
        'unPublish': {method: 'DELETE'}
      });
    }])

  .factory('DraftDatasetStatusResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/:type/:id/_status', {}, {
        'toStatus': {method: 'PUT', params: {id: '@id', type: '@type', value: '@value'}}
      });
    }])

  .factory('DraftDatasetRevisionsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/:type/:id/commits', {}, {
        'get': {method: 'GET'},
        'diff': {method: 'GET', url: contextPath + '/ws/draft/:type/:id/_diff', params: {id: '@id', type: '@type'}}
      });
    }])

  .factory('DraftDatasetPermissionsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/:datasetType/:id/permissions', {}, {
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

  .factory('DraftDatasetAccessesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/:datasetType/:id/accesses', {}, {
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

  .factory('DraftDatasetRestoreRevisionResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/:type/:id/commit/:commitId/restore', {}, {
        'restore': {method: 'PUT', params: {type: '@type', id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftDatasetViewRevisionResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource(contextPath + '/ws/draft/:type/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET', transformResponse: function(data) {
          return DatasetModelService.deserialize(data);
        }}
      });
    }])

  .factory('DatasetService', ['$rootScope',
    'HarmonizedDatasetResource',
    'NOTIFICATION_EVENTS',
    function ($rootScope, HarmonizedDatasetResource, NOTIFICATION_EVENTS) {

      function getNames(name) {
        return name.map(function(entry) {
          return entry.value;
        }).join('-');
      }

      return {
        delete: function (dataset, onSuccess) {
          var datasetToDelete = dataset;

          var removeSubscriber = $rootScope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
            if (datasetToDelete.id === id) {
              dataset.$delete(onSuccess);
            }
            removeSubscriber();
          });

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'dataset.delete-dialog.title',
              messageKey: 'dataset.delete-dialog.message',
              messageArgs: [getNames(dataset.name)]
            }, dataset.id
          );
        }
      };
    }])

  .factory('OpalTablesService', [function() {
    var factory = {};
    var tableWrappers = null;

    function findTargetTables(dataset, type) {
      switch (type) {
        case mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE:
          return dataset['obiba.mica.HarmonizedDatasetDto.type'].studyTables;
        case mica.dataset.OPAL_TABLE_TYPES.HARMONIZATION_TABLE:
          return dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTables;
      }

      throw new Error('Invalid table type');
    }

    function createTargetTables(dataset, type) {
      var tablesName;
      switch (type) {
        case mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE:
          tablesName = 'studyTables';
          break;
        case mica.dataset.OPAL_TABLE_TYPES.HARMONIZATION_TABLE:
          tablesName = 'harmonizationTables';
          break;
        default:
          throw new Error('Invalid table type');
      }

      dataset['obiba.mica.HarmonizedDatasetDto.type'][tablesName] = dataset['obiba.mica.HarmonizedDatasetDto.type'][tablesName] || [];
      return dataset['obiba.mica.HarmonizedDatasetDto.type'][tablesName];
    }

    factory.updateTable = function(dataset, wrapper, newTable) {
      var tables = findTargetTables(dataset, wrapper.type);

      var index = tables.indexOf(wrapper.table);
      if (index === -1) {
        throw new Error('Wrapper table is not found.');
      }

      tables[index] = newTable;
      wrapper.table = newTable;
    };

    factory.addTable = function(dataset, type, newTable) {
      var tables = createTargetTables(dataset, type);
      tables.push(newTable);
      tableWrappers = tableWrappers || [];
      tableWrappers.push({type: type, table: newTable});
    };

    factory.addUpdateTable = function(dataset, tableType, wrapper, newTable) {
      dataset.type = 'PROTOCOL';
      if (angular.isDefined(wrapper)) {
        if (wrapper.type !== tableType) {
          this.deleteTable(dataset, wrapper);
          this.addTable(dataset, tableType, newTable);
        } else {
          this.updateTable(dataset, wrapper, newTable);
        }
      } else {
        this.addTable(dataset, tableType, newTable);
      }

      return tableWrappers;
    };

    factory.setTable = function(dataset, newTable) {
      dataset.type = 'COLLECTED';
      dataset.studyTable = newTable;
    };

    factory.getTables = function getOpalTables(dataset) {
      tableWrappers = [];

      if (dataset.studyTables) {
        tableWrappers = dataset.studyTables.map(function (studyTable) {
          return {type: mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE, table: studyTable};
        });
      }

      if (dataset.harmonizationTables) {
        tableWrappers = tableWrappers.concat(
          dataset.harmonizationTables.map(function (harmonizationTable) {
            return {type: mica.dataset.OPAL_TABLE_TYPES.HARMONIZATION_TABLE, table: harmonizationTable};
          })
        );
      }

      tableWrappers = tableWrappers.sort(function(a,b){
        return a.table.weight - b.table.weight;
      });

      return tableWrappers;
    };

    factory.deleteTable = function(dataset, wrapper) {
      var wrapperIndex = tableWrappers.indexOf(wrapper);
      var tables = findTargetTables(dataset, wrapper.type);
      var index = tables.indexOf(wrapper.table);

      if (index > -1) {
        tables.splice(index, 1);
        if (tables.length === 0) {
          var tablesName = wrapper.type === mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE ? 'studyTables' : 'harmonizationTables';
          dataset[tablesName] = undefined;
        }

        tableWrappers.splice(wrapperIndex, 1);
      }
    };

    factory.deleteTables = function (dataset, wrappers) {
      if (Array.isArray(wrappers)) {
        wrappers.forEach(function (w) {
          factory.deleteTable(dataset, w);
        });
      }
    };

    factory.updateWeights = function() {
      if (tableWrappers) {
        for (var i = 0; i < tableWrappers.length;  i++) {
          tableWrappers[i].table.weight = i;
        }
      }
    };

    return factory;
  }])

  .service('DatasetModelService',['LocalizedValues', function(LocalizedValues) {
    this.serialize = function (network) {
      return serialize(network, false);
    };

    this.deserialize = function (data) {
      return deserialize(data, false);
    };

    this.serializeForRestoringFields = function (network) {
      return serialize(network, true);
    };

    this.deserializeForRestoringFields = function (data) {
      return deserialize(data, true);
    };

    function serialize(dataset, restore) {
      var datasetCopy = angular.copy(dataset);

      if (!restore) {
        datasetCopy.name = LocalizedValues.objectToArray(datasetCopy.model._name);
        datasetCopy.acronym = LocalizedValues.objectToArray(datasetCopy.model._acronym);
        datasetCopy.description = LocalizedValues.objectToArray(datasetCopy.model._description);
        datasetCopy.entityType = datasetCopy.model._entityType;
        delete datasetCopy.model._name;
        delete datasetCopy.model._acronym;
        delete datasetCopy.model._description;
        delete datasetCopy.model._entityType;
      } else {
        datasetCopy.name = LocalizedValues.objectToArray(datasetCopy.name);
        datasetCopy.acronym = LocalizedValues.objectToArray(datasetCopy.acronym);
        datasetCopy.description = LocalizedValues.objectToArray(datasetCopy.description);

        serializeOpalTableForRestoringFields(datasetCopy);
      }

      if (dataset.type === 'PROTOCOL') {
        datasetCopy.harmonizationTables = (dataset.harmonizationTables || []).map(serializeTableSource);
        datasetCopy.studyTables = (dataset.studyTables || []).map(serializeTableSource);
        datasetCopy.harmonizationTable = serializeTableSource(dataset.harmonizationTable || {});
      } else if (dataset.type === 'COLLECTED') {
        datasetCopy.studyTable = serializeTableSource(dataset.studyTable || {});
      }

      datasetCopy.content = datasetCopy.model ? angular.toJson(datasetCopy.model) : null;
      delete datasetCopy.model; // NOTICE: must be removed to avoid protobuf exception in dto.
      return angular.toJson(datasetCopy);
    }

    function deserialize(data, restore) {
      var dataset = angular.fromJson(data);
      dataset.model = dataset.content ? angular.fromJson(dataset.content) : {};

      if (!restore) {
        dataset.model._name = LocalizedValues.arrayToObject(dataset.name);
        dataset.model._acronym = LocalizedValues.arrayToObject(dataset.acronym);
        dataset.model._description = LocalizedValues.arrayToObject(dataset.description);
        dataset.model._entityType = dataset.entityType;
      } else {
        dataset.name = LocalizedValues.arrayToObject(dataset.name);
        dataset.acronym = LocalizedValues.arrayToObject(dataset.acronym);
        dataset.description = LocalizedValues.arrayToObject(dataset.description);

        deserializeOpalTableForRestoringFields(dataset);
      }

      if (dataset.type === 'PROTOCOL') {
        dataset.harmonizationTables = (dataset.harmonizationTables || []).map(deserializeTableSource);
        dataset.studyTables = (dataset.studyTables || []).map(deserializeTableSource);
        dataset.harmonizationTable = deserializeTableSource(dataset.harmonizationTable || {});
      } else if (dataset.type === 'COLLECTED') {
        dataset.studyTable = deserializeTableSource(dataset.studyTable || {});
      }

      return dataset;
    }

    function serializeOpalTableForRestoringFields(dataset) {
      if (dataset.type === 'PROTOCOL') {
        dataset.harmonizationTables = (dataset.harmonizationTables || []).map(serializeTable);
        dataset.studyTables = (dataset.studyTables || []).map(serializeTable);
        dataset.harmonizationTable = serializeTable(dataset.harmonizationTable || {});
      } else if (dataset.type === 'COLLECTED') {
        dataset.studyTable = serializeTable(dataset.studyTable || {});
      }

      function serializeTable(table) {
        const result = JSON.parse(JSON.stringify(table));

        result.name = LocalizedValues.objectToArray(result.name);
        result.description = LocalizedValues.objectToArray(result.description);

        return result;
      }
    }

    function serializeTableSource(table) {
      const result = JSON.parse(JSON.stringify(table));

      if (result.namespace === 'file') {
        result.source = 'urn:file:' + result.path + (result.table ? ':' + result.table : '');
      } else if (result.namespace === 'opal') {
        result.source = 'urn:opal:' + result.project + '.' + result.table;
      } else {
        result.source = 'urn:' + result.nid + ':' + result.nss;
      }
      delete result.path;
      delete result.project;
      delete result.table;
      delete result.nid;
      delete result.nss;
      delete result.namespace;

      return result;
    }

    function deserializeOpalTableForRestoringFields(dataset) {
      if (dataset.type === 'PROTOCOL') {
        dataset.harmonizationTables = (dataset.harmonizationTables || []).map(deserializeTable);
        dataset.studyTables = (dataset.studyTables || []).map(deserializeTable);
        dataset.harmonizationTable = deserializeTable(dataset.harmonizationTable || {});
      } else if (dataset.type === 'COLLECTED') {
        dataset.studyTable = deserializeTable(dataset.studyTable || {});
      }

      function deserializeTable(table) {
        const result = JSON.parse(JSON.stringify(table));

        result.name = LocalizedValues.arrayToObject(result.name);
        result.description = LocalizedValues.arrayToObject(result.description);

        return result;
      }
    }

    function deserializeTableSource(table) {
      const result = JSON.parse(JSON.stringify(table));

      if (result.source) {
        if (result.source.startsWith('urn:opal:')) {
          const id = result.source.replace('urn:opal:', '');
          result.namespace = 'opal';
          result.project = id.substring(0, id.indexOf('.'));
          result.table = id.substring(id.indexOf('.') + 1);
        } else if (result.source.startsWith('urn:file:')) {
          const id = result.source.replace('urn:file:', '');
          result.namespace = 'file';
          result.path = id.indexOf(':') > 0 ? id.substring(0, id.indexOf(':')) : id;
          result.table = id.indexOf(':') > 0 ? id.substring(id.indexOf(':') + 1) : undefined;
        } else {
          result.namespace = 'other';
        }
        const id = result.source.replace('urn:', '');
        const sepIdx = id.indexOf(':');
        result.nid = id.substring(0, sepIdx);
        result.nss = id.substring(sepIdx + 1);
      } else {
        result.namespace = 'opal';
      }

      return result;
    }

    return this;
  }]);
