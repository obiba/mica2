const TYPES = {
  VARIABLES: 'variables',
  DATASETS: 'datasts',
  STUDIES: 'studies',
  NETWORKS: 'networks'
};

const TARGETS = {
  VARIABLE: 'variable',
  DATASET: 'datast',
  STUDY: 'study',
  NETWORK: 'network'
};

const TYPES_TARGETS_MAP = {
  variables: 'variable',
  datasets: 'datast',
  studies: 'study',
  networks: 'network'
};

const TARGETS_TYPES_MAP = {
  variable: 'variables',
  dataset: 'datasts',
  study: 'studies',
  network: 'networks'
};

const QUERY_ACTIONS = {
  SELECTION: 'selection',
  UPDATE: 'update',
  DELETE: 'delete',
  PAGINATE: 'paginate'
};

const QueryTreeOptions  = {containers: Object.values(TARGETS)};

const EVENTS = {
  QUERY_TYPE_SELECTION: 'query-type-selection',
  QUERY_TYPE_UPDATE: 'query-type-update',
  QUERY_TYPE_DELETE: 'query-type-delete',
  QUERY_TYPE_PAGINATE: 'query-type-paginate',
};

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

  __ensureLimitFieldsSizeQueries(tree, targetQuery) {
    let limitQuery = tree.search((name, args, parent) => name === 'limit' && this._target === parent.name);

    if (!limitQuery) {
      tree.addQuery(targetQuery, new RQL.Query('limit', [0, this._defaultSize]));
    }

    tree.addQuery(targetQuery, new RQL.Query('fields', this.getFields()));
    tree.addQuery(targetQuery, new RQL.Query('sort', this.getSortFields()));
  }

  prepareForSelection(tree) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);
    let targetQuery = theTree.search((name) => name === this._target);

    if (!targetQuery) {
      targetQuery = new RQL.Query(this._target, []);
      theTree.addQuery(null, targetQuery);
    }

    this.__ensureLimitFieldsSizeQueries(theTree, targetQuery);

    return theTree;
  }

  prepareForUpdate(tree, target, updateQuery) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);
    let targetQuery = theTree.search((name) => name === target);

    if (!targetQuery) {
      targetQuery = new RQL.Query(target);
      theTree.addQuery(null, targetQuery);
    }

    const fnSearch = updateQuery.name === 'match'
      ? (name, args) => args.indexOf(updateQuery.args[1]) > -1
      : (name, args) => args.indexOf(updateQuery.args[0]) > -1;

    let query = theTree.search(fnSearch);

    if (!query) {
      theTree.addQuery(targetQuery, updateQuery);
    } else {
      query.name = updateQuery.name;
      query.args = updateQuery.args;
    }

    this.__ensureLimitFieldsSizeQueries(theTree, targetQuery);

    return theTree;
  }

  prepareForDelete(tree, target, deleteQuery) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);
    let targetQuery = theTree.search((name) => name === target);

    if (!targetQuery) {
      console.debug(`Cannot delete query, target ${target} does not exits.`);
      return;
    }

    const fnSearch = deleteQuery.name === 'match'
      ? (name, args) => args.indexOf(deleteQuery.args[1]) > -1
      : (name, args) => args.indexOf(deleteQuery.args[0]) > -1;

    let query = theTree.search(fnSearch);

    if (query) {
      theTree.deleteQuery(query);
    }

    this.__ensureLimitFieldsSizeQueries(theTree, targetQuery);

    return theTree;
  }

  prepareForPaginate(tree, target, from, size) {
    let limitQuery = tree.search((name, args, parent) => 'limit' === name && target === parent.name);
    if (limitQuery) {
      limitQuery.args[0] = from;
      limitQuery.args[1] = size;
    }

    return this.prepareForSelection(tree);
  }
}

class VariableQuery extends EntityQuery {
  constructor(defaultSize) {
    super('variables', 'variable', defaultSize);
    this._fields = ['attributes.label.*', 'variableType', 'datasetId', 'datasetAcronym', 'attributes.Mlstr_area*'];
    this._sortFields = ['variableType,containerId', 'populationWeight', 'dataCollectionEventWeight', 'datasetId', 'index', 'name'];
  }
}

class MicaQueryExecutor {

  constructor(eventBus, defaultSize) {
    this._eventBus = eventBus;
    this._query = {
      'variables': new VariableQuery(defaultSize),
      'datasets': new EntityQuery('datasets', 'dataset', defaultSize),
      'studies': new EntityQuery('studies', 'study', defaultSize),
      'networks': new EntityQuery('networks', 'network', defaultSize)
    };
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
   * If the URL has query param, a QueryTree is returned
   *
   * @param type
   * @param display
   * @returns {QueryTree|QueryTree|null}
   * @private
   */
  __getQueryTreeFromUrl(type, display) {
    const urlParts = this.__parseUrl();
    if (urlParts.hash === display && Object.keys(urlParts.searchParams).length > 0) {
      return new RQL.QueryTree(RQL.Parser.parseQuery(urlParts.searchParams.query), QueryTreeOptions);
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
    switch (payload.type) {
      case 'variables':
        const type = payload.type;
        const display = payload.display;
        const entityQuery = this._query[type];
        if (!entityQuery) {
          throw new Error(`Invalid entity query ${type}`);
        }

        let tree = this.__getQueryTreeFromUrl(type, display);

        switch (eventId) {
          case EVENTS.QUERY_TYPE_SELECTION:
            tree = entityQuery.prepareForSelection(tree);
            break;
          case EVENTS.QUERY_TYPE_UPDATE:
            tree = entityQuery.prepareForUpdate(tree, payload.target, payload.query);
            break;
          case EVENTS.QUERY_TYPE_DELETE:
            tree = entityQuery.prepareForDelete(tree, payload.target, payload.query);
            break;
          case EVENTS.QUERY_TYPE_PAGINATE:
            tree = entityQuery.prepareForPaginate(tree, payload.target, payload.from, payload.size);
            break;
        }
        this.__executeQuery(tree, type, display, payload.noUrlUpdate);
        break;

      case 'datasets-list':
      case 'studies-list':
      case 'networks-list':
        break;


      // for other queries
      default:
        break;
    }
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
    axios
      .get(`../ws/${type}/_rql?query=${tree.serialize()}`)
      .then(response => {
        // remove hidden queries
        tree.findAndDeleteQuery((name) => 'fields' === name);
        tree.findAndDeleteQuery((name) => 'sort' === name);
        const limitQuery = tree.search((name, args, parent) => 'limit' === name && TYPES_TARGETS_MAP[type] === parent.name);

        if (!noUrlUpdate) {
          this.__updateLocation(type, display, tree.serialize());
        }

        EventBus.$emit('variables-results', {response: response.data, from: limitQuery.args[0]});
      });
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

  __onQueryTypeDelete(payload) {
    console.log(`__onQueryTypeSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_DELETE, payload);
  }

  __onQueryTypePaginate(payload) {
    console.log(`__onQueryTypeSelection ${payload}`);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_PAGINATE, payload);
  }

  __updateLocation(type, display, query) {
    const urlSearch = [`type=${type}`, `query=${query}`].join('&');
    const hash = `${display}?${urlSearch}`;
    history.pushState(null, null, `#${hash}`);
  }

  init() {
    this._eventBus.register('query-type-selection', this.__onQueryTypeSelection.bind(this));
    this._eventBus.register('query-type-update', this.__onQueryTypeUpdate.bind(this));
    this._eventBus.register('query-type-delete', this.__onQueryTypeDelete.bind(this));
    this._eventBus.register('query-type-paginate', this.__onQueryTypePaginate.bind(this));
    window.addEventListener('hashchange', this.__onHashChanged.bind(this));
    window.addEventListener('beforeunload', this.__onBeforeUnload.bind(this));
  }

  destroy() {
    this._eventBus.unregister('query-type-selection', this.__onQueryTypeSelection);
    this._eventBus.unregister('query-type-update', this.__onQueryTypeUpdate);
    this._eventBus.unregister('query-type-delete', this.__onQueryTypeDelete);
    this._eventBus.unregister('query-type-paginate', this.__onQueryTypePaginate);
  }
}


