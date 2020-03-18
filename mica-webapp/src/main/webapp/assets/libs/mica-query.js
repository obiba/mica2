const TYPES = {
  VARIABLES: 'variables',
  DATASETS: 'datasets',
  STUDIES: 'studies',
  NETWORKS: 'networks'
};

const TARGETS = {
  VARIABLE: 'variable',
  DATASET: 'dataset',
  STUDY: 'study',
  NETWORK: 'network'
};

const TYPES_TARGETS_MAP = {
  variables: 'variable',
  datasets: 'dataset',
  studies: 'study',
  networks: 'network'
};

BUCKETS = {
  study: 'studyId',
  dce: 'dceId',
  dataset: 'datasetId'
};

TAREGT_ID_BUCKET_MAP = {
  studyId: 'study',
  dceId: 'dce',
  datasetId: 'dataset'
};

const TARGETS_TYPES_MAP = {
  variable: 'variables',
  dataset: 'datasets',
  study: 'studies',
  network: 'networks'
};

const QUERY_ACTIONS = {
  SELECTION: 'selection',
  UPDATE: 'update',
  DELETE: 'delete',
  PAGINATE: 'paginate'
};

const DISPLAYS = {
  LISTS: 'lists',
  COVERAGE: 'coverage',
  GRAPHICS: 'grahics'
};
const DISPLAY_TABS = {
  LISTS: 'tab-lists',
  COVERAGE: 'tab-coverage',
  GRAPHICS: 'tab-grahics'
};

const QueryTreeOptions  = {containers: Object.values(TARGETS)};

const EVENTS = {
  QUERY_TYPE_SELECTION: 'query-type-selection',
  QUERY_TYPE_UPDATE: 'query-type-update',
  QUERY_TYPE_UPDATES_SELECTION: 'query-type-updates-selection',
  QUERY_TYPE_DELETE: 'query-type-delete',
  QUERY_TYPE_PAGINATE: 'query-type-paginate',
  QUERY_TYPE_COVERAGE: 'query-type-coverage',
  LOCATION_CHANGED: 'location-changed'
};

const OPERATOR_NODES = ['and','or'];
const CRITERIA_NODES = ['contains', 'in', 'out', 'eq', 'gt', 'ge', 'lt', 'le', 'between', 'match', 'exists', 'missing'];
const AGGREGATE = 'aggregate';
const BUCKET = 'bucket';


/**
 * Base class used to build quries and for CRUD operations
 */
class EntityQuery {
  constructor(type, target, defaultSize) {
    this._type = type;
    this._target = target;
    this._defaultSize = defaultSize;
    this._fields = [];
    this._sortFields = [];
  }

  getType() {
    return this._type;
  }

  getTarget() {
    return this._target;
  }

  getFields() {
    return this._fields;
  }

  getSortFields() {
    return this._sortFields;
  }

  __ensureDestinationTargetQuery(type, tree) {
    const destTarget = TYPES_TARGETS_MAP[type];
    let destQuery = tree.search((name) => name === destTarget);
    if (!destQuery) {
      destQuery = new RQL.Query(destTarget,[]);
      tree.addQuery(null, destQuery);
    }

    return destQuery;
  }

  __ensureLimitFieldsSizeQueries(tree, targetQuery) {
    let limitQuery = tree.search((name, args, parent) => name === 'limit' && this._target === parent.name);

    if (!limitQuery) {
      tree.addQuery(targetQuery, new RQL.Query('limit', [0, this._defaultSize]));
    }

    tree.addQuery(targetQuery, new RQL.Query('fields', this.getFields()));
    tree.addQuery(targetQuery, new RQL.Query('sort', this.getSortFields()));
  }

  /**
   * Searches for a query by searching for the criterion key (<Taxonomy>_<vocabulary>)
   *
   * @returns Query
   * @private
   */
  __findQuery(tree, query) {
    const fnSearch = query.name === 'match'
      ? (name, args) => args.indexOf(query.args[1]) > -1
      : (name, args) => args.indexOf(query.args[0]) > -1;

    return tree.search(fnSearch);
  }

  __getOpOrCriteriaChild(tree, targetQuery) {
    return targetQuery.args
      .filter(query =>
        OPERATOR_NODES.indexOf(query.name) > -1 ||
        CRITERIA_NODES.indexOf(query.name) > -1
      ).pop();
  }

  /**
   * Final preparation by making sure the limit, size and especially the type related target query is added
   *
   * @param type
   * @param tree
   */
  prepareForQuery(type, tree) {
    this.__ensureLimitFieldsSizeQueries(tree, this.__ensureDestinationTargetQuery(type, tree));
    return tree;
  }

  prepareForSelection(tree, type) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);
    let targetQuery = theTree.search((name) => name === this._target);

    if (!targetQuery) {
      targetQuery = new RQL.Query(this._target, []);
      theTree.addQuery(null, targetQuery);
    }

    return theTree;
  }

  prepareForUpdate(tree, type, target, updateQuery) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);
    let targetQuery = theTree.search((name) => name === target);

    if (!targetQuery) {
      targetQuery = new RQL.Query(target);
      theTree.addQuery(null, targetQuery);
    }

    let query = this.__findQuery(theTree, updateQuery);

    if (!query) {
      theTree.addQuery(targetQuery, updateQuery);
    } else {
      query.name = updateQuery.name;
      query.args = updateQuery.args;
    }

    return theTree;
  }

  /**
   * Updates several queries and modifies the tree.
   * TODO merge children of matching operators as to reduce the tree branching
   *
   * @param tree
   * @param type
   * @param updateInfo - [{target, query, operator}]
   */
  prepareForUpdatesAndSelection(tree, type, updateInfo) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);

    (updateInfo || []).forEach(info => {
      if (info.target && info.query) {
        const operator = {and: 'and', or: 'or'}[info.operator] || 'and';

        let targetQuery = theTree.search((name) => name === info.target);
        if (!targetQuery) {
          // create target and add query as child, done!
          targetQuery = new RQL.Query(info.target);
          theTree.addQuery(null, targetQuery);
          theTree.addQuery(targetQuery, info.query)
        } else {
          let theQuery = this.__findQuery(theTree, info.query);
          if (theQuery) {
            // query exists, just update
            theQuery.name = info.query.name;
            theQuery.args = info.query.args;
          } else {
            let opOrCriteriaChild = this.__getOpOrCriteriaChild(theTree, targetQuery);
            if (opOrCriteriaChild) {
              // there is a an operator or criteria child, use the proposed operator and modify theTree
              const operatorQuery = new RQL.Query(operator);
              theTree.addQuery(targetQuery, operatorQuery);
              theTree.deleteQuery(opOrCriteriaChild);
              theTree.addQuery(operatorQuery, opOrCriteriaChild);
              theTree.addQuery(operatorQuery, info.query);
            } else {
              // target has no operator or crtieria child, just add criteria
              theTree.addQuery(targetQuery, info.query);
            }
          }
        }
      }
    });

    return theTree;
  }

  prepareForDelete(tree, type, target, deleteQuery, exact) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);
    let targetQuery = theTree.search((name) => name === target);

    if (!targetQuery) {
      console.debug(`Cannot delete query, target ${target} does not exits.`);
      return;
    }

    let query = this.__findQuery(theTree, deleteQuery);

    if (query) {
      theTree.deleteQuery(query);
    }

    return theTree;
  }

  prepareForPaginate(tree, type, target, from, size) {
    let limitQuery = tree.search((name, args, parent) => 'limit' === name && target === parent.name);
    if (limitQuery) {
      limitQuery.args[0] = from;
      limitQuery.args[1] = size;
    }

    return this.prepareForSelection(tree, type);
  }

  prepareForCoverage(tree, bucket) {
    if (tree) {
      let variableQuery = tree.search(name => TARGETS.VARIABLE === name);
      if (variableQuery) {
        // TODO: should the coverage be limited to MLSTR taxonomies?
        const coveragePossible = tree.search((name, args) => {
          return args.some(arg => typeof arg === 'string' && arg.search(/Mlstr/) > -1)
        });

        if (coveragePossible) {
          // aggregation
          tree.addQuery(variableQuery, new RQL.Query(AGGREGATE, [new RQL.Query(BUCKET, [bucket])]));
          return tree;
        }
      }
    }

    return null;
  }

}

class VariableQuery extends EntityQuery {
  constructor(defaultSize) {
    super(TYPES.VARIABLES, TARGETS.VARIABLE, defaultSize);
    this._fields = ['attributes.label.*', 'variableType', 'datasetId', 'datasetAcronym', 'attributes.Mlstr_area*'];
    this._sortFields = ['variableType,containerId', 'populationWeight', 'dataCollectionEventWeight', 'datasetId', 'index', 'name'];
  }
}

class DatasetQuery extends EntityQuery {
  constructor(defaultSize) {
    super(TYPES.DATASETS, TARGETS.DATASET, defaultSize);
    this._fields = ['acronym.*','name.*','variableType','studyTable.studyId','studyTable.project','studyTable.table','studyTable.populationId','studyTable.dataCollectionEventId','harmonizationTable.studyId','harmonizationTable.project','harmonizationTable.table','harmonizationTable.populationId'];
    this._sortFields = ['studyTable.studyId','studyTable.populationWeight','studyTable.dataCollectionEventWeight','acronym'];
  }
}

class StudyQuery extends EntityQuery {
  constructor(defaultSize) {
    super(TYPES.STUDIES, TARGETS.STUDY, defaultSize);
    this._fields = ['acronym.*','name.*','model.methods.design','populations.dataCollectionEvents.model.dataSources','model.numberOfParticipants.participant'];
    this._sortFields = ['acronym'];
  }
}

class NetworkQuery extends EntityQuery {
  constructor(defaultSize) {
    super(TYPES.NETWORKS, TARGETS.NETWORK, defaultSize);
    this._fields = ['acronym.*','name.*','studyIds'];
    this._sortFields = ['acronym'];
  }
}

class MicaQueryExecutor {

  constructor(eventBus, defaultSize) {
    this._eventBus = eventBus;
    this._query = {};
    this._query[TYPES.VARIABLES] = new VariableQuery(defaultSize);
    this._query[TYPES.DATASETS] = new DatasetQuery(defaultSize);
    this._query[TYPES.STUDIES] = new StudyQuery(defaultSize);
    this._query[TYPES.NETWORKS] = new NetworkQuery(defaultSize);
  }

  /**
   * @returns {{hash: string, searchParams: {}}}
   * @private
   */
  __parseUrl() {
    let urlParts ={
      hash: "",
      searchParams: {}
    };

    const hash = window.location.hash;

    if ((hash || "#").length > 1) {
      urlParts.hash = hash.substring(1);
      const searchIndex = hash.indexOf('?');
      if (searchIndex > -1) {
        urlParts.hash = hash.substring(1, searchIndex);
        const params = hash.substring(searchIndex+1).split('&');
        params.forEach(param => {
          const parts = param.split('=');
          urlParts.searchParams[parts[0]] = parts[1] || "";
        })
      }
    }

    return urlParts;
  }

  /**
   * Ensure a valid type either supplied by the payload or url
   */
  __ensureValidType(urlSearchParams, type) {
    return TYPES[(type || urlSearchParams.type || "").toUpperCase()] || TYPES.VARIABLES;
  }

  /**
   * Ensure a valid type either supplied by the payload or url
   */
  __getBucketType(urlSearchParams, display) {
    if (DISPLAYS.COVERAGE === display) {
      return urlSearchParams.hasOwnProperty(BUCKET) ? BUCKETS[urlSearchParams[BUCKET]]: BUCKETS.study;
    }

    return null;
  }

  /**
   * Ensure a valid display either supplied by the payload or url
   */
  __ensureValidDisplay(hash, display) {
    return DISPLAYS[(display || hash || "").toUpperCase()] || DISPLAYS.LISTS;
  }

  /**
   * If the URL has query param, a QueryTree is returned
   *
   * @private
   * @param urlSearchParams
   * @returns {QueryTree|null}
   */
  __getQueryTreeFromUrl(urlSearchParams) {
    if (Object.keys(urlSearchParams).length > 0) {
      return new RQL.QueryTree(RQL.Parser.parseQuery(urlSearchParams.query), QueryTreeOptions);
    }

    return null;
  }

  /**
   * Common handler for all query-type events
   *
   * @param eventId
   * @param payload
   * @private
   */
  __prepareAndExecuteQuery(eventId, payload) {
    const urlParts = this.__parseUrl();
    const type = this.__ensureValidType(urlParts.searchParams, payload.type);

    switch (type) {
      case TYPES.VARIABLES:
      case TYPES.DATASETS:
      case TYPES.STUDIES:
      case TYPES.NETWORKS:
        const entityQuery = this._query[type];
        const display = this.__ensureValidDisplay(urlParts.hash, payload.display);
        const bucket = this.__getBucketType(urlParts.searchParams, display);
        let tree = this.getTree(urlParts);

        switch (eventId) {
          case EVENTS.QUERY_TYPE_SELECTION:
            tree = entityQuery.prepareForSelection(tree, type);
            break;
          case EVENTS.QUERY_TYPE_UPDATE:
            tree = entityQuery.prepareForUpdate(tree, type, payload.target, payload.query);
            break;
          case EVENTS.QUERY_TYPE_UPDATES_SELECTION:
            tree = entityQuery.prepareForUpdatesAndSelection(tree, type, payload.updates);
            break;
          case EVENTS.QUERY_TYPE_DELETE:
            tree = entityQuery.prepareForDelete(tree, type, payload.target, payload.query);
            break;
          case EVENTS.QUERY_TYPE_PAGINATE:
            tree = entityQuery.prepareForPaginate(tree, type, payload.target, payload.from, payload.size);
            break;
          case EVENTS.QUERY_TYPE_COVERAGE:
            tree = entityQuery.prepareForCoverage(tree, bucket);
            break;
        }

        if (tree) {
          switch (display) {
            case DISPLAYS.COVERAGE:
              if (EVENTS.QUERY_TYPE_COVERAGE === eventId) {
                this.__executeCoverage(tree, type, display, payload.noUrlUpdate, bucket);
              } else {
                const coverageTree = entityQuery.prepareForCoverage(tree, bucket);
                if (!coverageTree) {
                  this.__ignoreCoverage(tree, type, display, payload.noUrlUpdate, bucket);
                } else {
                  this.__executeCoverage(coverageTree, type, display, payload.noUrlUpdate, bucket);
                }
              }
              break;

            case DISPLAYS.LISTS:
              this.__executeQuery(entityQuery.prepareForQuery(type, tree), type, display, payload.noUrlUpdate);
              break;
          }
        }
    }
  }

  getTree(parts) {
    const urlParts = parts || this.__parseUrl();
    return this.__getQueryTreeFromUrl(urlParts.searchParams);
  }

  getTreeQueries(parts) {
    const tree = this.getTree(parts);
    const validNodes = CRITERIA_NODES.concat(OPERATOR_NODES);
    let queries = {};

    for (const key in TARGETS) {
      let targetQuery = tree.search((name) => {
        return name === TARGETS[key]
      });

      if (targetQuery) {
        targetQuery.args = targetQuery.args.filter(arg => validNodes.indexOf(arg.name) > -1);
        queries[TARGETS[key]] = targetQuery;
      }
    }

    return queries;
  }

  /**
   * Executes a query and emits a result event
   *
   * @param tree
   * @param type
   * @param display
   * @param noUrlUpdate
   * @private
   */
  __executeQuery(tree, type ,display, noUrlUpdate) {
    console.log(`__executeQuery`);

    axios
      .get(`../ws/${type}/_rql?query=${tree.serialize()}`)
      .then(response => {
        // remove hidden queries
        tree.findAndDeleteQuery((name) => 'fields' === name);
        tree.findAndDeleteQuery((name) => 'sort' === name);
        const limitQuery = tree.search((name, args, parent) => 'limit' === name && TYPES_TARGETS_MAP[type] === parent.name);
        this.__updateLocation(type, display, tree, noUrlUpdate);

        EventBus.$emit(`${type}-results`, {response: response.data, from: limitQuery.args[0], size: limitQuery.args[1]});
      });
  }

  /**
   * Ignores coverage as a result of a query that does not yield any 'coverage query'.
   *
   * @param tree
   * @param type
   * @param display
   * @param noUrlUpdate
   * @param bucket
   * @private
   */
  __ignoreCoverage(tree, type, display, noUrlUpdate, bucket) {
    const aggQuery = tree.search((name) => AGGREGATE === name);
    if (aggQuery) tree.deleteQuery(aggQuery);
    this.__updateLocation(type, display, tree, noUrlUpdate, bucket);
    EventBus.$emit(`coverage-results`, {bucket, response: []});
  }

  /**
   * Executes a coverage and emits a result event
   *
   * @param tree
   * @param type
   * @param display
   * @param noUrlUpdate
   * @private
   */
  __executeCoverage(tree, type, display, noUrlUpdate, bucket) {
    console.log(`__executeCoverage`);

    axios
      .get(`../ws/variables/_coverage?query=${tree.serialize()}`)
      .then(response => {
        tree.findAndDeleteQuery((name) => AGGREGATE === name);
        this.__updateLocation(type, display, tree, noUrlUpdate, bucket);
        EventBus.$emit(`coverage-results`, {bucket, response: response.data});
      });
  }

  __updateLocation(type, display, tree, replace, bucket) {
    const query = tree.serialize();
    console.log(`__updateLocation ${type} ${display} ${query} - history states ${history.length}`);
    let params = [`type=${type}`, `query=${query}`];
    if (bucket) {
      params.push(`bucket=${TAREGT_ID_BUCKET_MAP[bucket]}`);
    }

    const urlSearch = params.join('&');
    const hash = `${display}?${urlSearch}`;
    if(replace) {
      history.replaceState(null, "", `#${hash}`);
    } else {
      history.pushState(null, "", `#${hash}`);
    }

    this._eventBus.$emit(EVENTS.LOCATION_CHANGED, {type, display, tree});
  }

  /**
   * Handles hash changes and emits a query-type-selection event
   *
   * @param event
   * @private
   */
  __onHashChanged(event) {
    console.log(`On hash changed ${event}`);
    const urlParts = this.__parseUrl();
    const searchParams = urlParts.searchParams || {};
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_SELECTION, {
      type: searchParams.type || TYPES.VARIABLES,
      display: urlParts.hash || 'list',
      noUrlUpdate: true
    });
  }

  /**
   * When changing pages, Vue's life cycle is broken, this handler is called when leaving the '/search' and removes
   * all 'window' event handlers.
   *
   * @param event
   * @private
   */
  __onBeforeUnload(event) {
    console.log(`On before unload ${event}`);
    window.removeEventListener('hashchange', this.__onHashChanged);
    window.removeEventListener('beforeunload', this.__onBeforeUnload);
  }

  __onQueryTypeSelection(payload) {
    console.log(`__onQueryTypeSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_SELECTION, payload);
  }

  __onQueryTypeUpdate(payload) {
    console.log(`__onQueryTypeSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_UPDATE, payload);
  }

  __onQueryTypeUpdatesSelection(payload) {
    console.log(`__onQueryTypeUpdatesSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_UPDATES_SELECTION, payload);
  }

  __onQueryTypeDelete(payload) {
    console.log(`__onQueryTypeSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_DELETE, payload);
  }

  __onQueryTypePaginate(payload) {
    console.log(`__onQueryTypeSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_PAGINATE, payload);
  }

  __onQueryTypeCoverage(payload) {
    console.log(`__onQueryTypeSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_COVERAGE, payload);
  }

  init() {
    this._eventBus.register(EVENTS.QUERY_TYPE_SELECTION, this.__onQueryTypeSelection.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_UPDATE, this.__onQueryTypeUpdate.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_UPDATES_SELECTION, this.__onQueryTypeUpdatesSelection.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_DELETE, this.__onQueryTypeDelete.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_PAGINATE, this.__onQueryTypePaginate.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_COVERAGE, this.__onQueryTypeCoverage.bind(this));
    window.addEventListener('hashchange', this.__onHashChanged.bind(this));
    window.addEventListener('beforeunload', this.__onBeforeUnload.bind(this));
  }

  destroy() {
    this._eventBus.unregister(EVENTS.QUERY_TYPE_SELECTION, this.__onQueryTypeSelection);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_UPDATE, this.__onQueryTypeUpdate);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_UPDATES_SELECTION, this.__onQueryTypeUpdatesSelection);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_DELETE, this.__onQueryTypeDelete);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_COVERAGE, this.__onQueryTypeCoverage);
  }
}


