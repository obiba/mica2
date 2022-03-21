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

const BUCKETS = {
  study: 'studyId',
  dce: 'dceId',
  dataset: 'datasetId'
};

const TARGET_ID_BUCKET_MAP = {
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
  GRAPHICS: 'graphics'
};

const DISPLAY_TABS = {
  LISTS: 'tab-lists',
  COVERAGE: 'tab-coverage',
  GRAPHICS: 'tab-graphics'
};

function fromBucketToTarget(bucket) {
  let target;
  if (bucket === 'dce') {
    target = TARGETS.VARIABLE;
  } else if (bucket === 'study') {
    target = TARGETS.STUDY;
  } else {
    target = TARGETS.DATASET;
  }

  return target;
}

const QueryTreeOptions  = {containers: Object.values(TARGETS)};

const EVENTS = {
  QUERY_TYPE_SELECTION: 'query-type-selection',
  QUERY_TYPE_UPDATE: 'query-type-update',
  QUERY_TYPE_UPDATES_SELECTION: 'query-type-updates-selection',
  QUERY_TYPE_DELETE: 'query-type-delete',
  QUERY_TYPE_PAGINATE: 'query-type-paginate',
  QUERY_TYPE_COVERAGE: 'query-type-coverage',
  QUERY_TYPE_GRAPHICS: 'query-type-graphics',
  QUERY_TYPE_GRAPHICS_RESULTS: 'query-type-graphics-results',
  QUERY_ALERT: 'query-alert',
  LOCATION_CHANGED: 'location-changed',
  CLEAR_RESULTS_SELECTIONS: 'clear-results-selections'
};

const OPERATOR_NODES = ['and','or'];
const CRITERIA_NODES = ['contains', 'in', 'out', 'eq', 'gt', 'ge', 'lt', 'le', 'between', 'match', 'exists', 'missing'];
const AGGREGATE = 'aggregate';
const BUCKET = 'bucket';
const FACET = 'facet';


/**
 * Base class used to build queries and for CRUD operations
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

  __ensureLimitFieldsSizeAndLocaleQueries(tree, targetQuery) {
    let limitQuery = tree.search((name, args, parent) => name === 'limit' && this._target === parent.name);

    if (!limitQuery) {
      tree.addQuery(targetQuery, new RQL.Query('limit', [0, this._defaultSize]));
    }

    tree.addQuery(targetQuery, new RQL.Query('fields', this.getFields()));
    tree.addQuery(targetQuery, new RQL.Query('sort', this.getSortFields()));

    let localeQuery = tree.search((name, args, parent) => name === 'locale');
    if (!localeQuery) {
      localeQuery = new RQL.Query("locale", [Mica.locale]);
      tree.addQuery(null, localeQuery);
    }
  }

  /**
   * Searches for a query by searching for the criterion key (<Taxonomy>_<vocabulary>)
   *
   * @returns Query
   * @private
   */
  __findQuery(tree, query) {
    if (OPERATOR_NODES.indexOf(query.name) === -1) {
      const fnSearch = query.name === 'match'
        ? (name, args) => args.indexOf(query.args[1]) > -1
        : (name, args) => args.indexOf(query.args[0]) > -1;

      return tree.search(fnSearch);
    } else {
      const fnSearch = (name, args) => (name === query.name && (args || []).every((arg, index) => arg.name === (query.args[index] || {}).name));
      return tree.search(fnSearch);
    }
  }

  __getOpOrCriteriaChild(tree, targetQuery) {
    return targetQuery.args
      .filter(query =>
        OPERATOR_NODES.indexOf(query.name) > -1 ||
        CRITERIA_NODES.indexOf(query.name) > -1
      ).pop();
  }

  __isOpalTaxonomy(args) {
    return args.some(arg => typeof arg === 'string' && arg.search(/Mlstr/) > -1);
  }

  __reduce(parent, query) {
    if (parent.name === 'or') {
      let grandparent = parent.parent;
      const parentIndex = grandparent.args.indexOf(parent);
      grandparent.args[parentIndex] = query;
      if (grandparent.name !== TARGETS.VARIABLE) {
        this.__reduce(grandparent, query);
      }
    }
    else if (query.name !== TARGETS.VARIABLE && parent.name === 'and') {
      // Reduce until parent is Variable node or another AND node
      this.__reduce(parent.parent, parent);
    }
  }

  /**
   * @param tree
   * @param updates
   * @returns null or the query to reduce to
   */
  __findQueryToReduceTo(tree, updates) {
    const update = updates.filter(update => "reduceKey" in update).pop();
    if (update) {
      return tree.search((name, args) => args.indexOf(update.reduceKey) > -1);
    }

    return null;
  }

  /**
   * Final preparation by making sure the limit, size and especially the type related target query is added
   *
   * @param type
   * @param tree
   */
  prepareForQuery(type, tree) {
    this.__ensureLimitFieldsSizeAndLocaleQueries(tree, this.__ensureDestinationTargetQuery(type, tree));
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
      EventBus.$emit(EVENTS.QUERY_ALERT, {target, query: updateQuery, action: 'created'});
    } else {
      query.name = updateQuery.name;
      query.args = updateQuery.args;
      EventBus.$emit(EVENTS.QUERY_ALERT, {target, query: updateQuery, action: 'updated'});
    }

    return theTree;
  }

  /**
   * Updates several queries and modifies the tree.
   * TODO merge children of matching operators as to reduce the tree branching
   *
   * @param tree
   * @param type
   * @param updates - [{target, query, operator}]
   */
  prepareForUpdatesAndSelection(tree, type, updates, noAlerts) {
    let theTree = tree || new RQL.QueryTree(null, QueryTreeOptions);

    (updates || []).forEach(info => {
      let alertEventData;

      if (info.target && info.query) {
        let operator = ({and: 'and', or: 'or'})[info.operator];
        if (!operator) {
          operator = this.__isOpalTaxonomy(info.query.args) ? 'or' : 'and';
        }

        let targetQuery = theTree.search((name) => name === info.target);
        if (!targetQuery) {
          // create target and add query as child, done!
          targetQuery = new RQL.Query(info.target);
          theTree.addQuery(null, targetQuery);
          theTree.addQuery(targetQuery, info.query)
          alertEventData = {target: info.target, query: info.query, action: 'created'};
        } else {
          let theQuery = this.__findQuery(theTree, info.query);
          if (theQuery) {
            // query exists, just update
            theQuery.name = info.newName || info.query.name;
            theQuery.args = info.query.args;
            alertEventData =   {target: info.target, query: info.query, action: 'updated'};
          } else {
            let opOrCriteriaChild = this.__getOpOrCriteriaChild(theTree, targetQuery);
            if (opOrCriteriaChild) {
              // there is an operator or criteria child, use the proposed operator and modify theTree
              const operatorQuery = new RQL.Query(operator);
              theTree.addQuery(targetQuery, operatorQuery);
              theTree.deleteQuery(opOrCriteriaChild);
              theTree.addQuery(operatorQuery, opOrCriteriaChild);
              theTree.addQuery(operatorQuery, info.query);
              alertEventData = {target: info.target, query: info.query, action: 'created'};
            } else {
              // target has no operator or crtieria child, just add criteria
              theTree.addQuery(targetQuery, info.query);
              alertEventData = {target: info.target, query: info.query, action: 'created'};
            }
          }
        }
      }

      if (!noAlerts) {
        EventBus.$emit(EVENTS.QUERY_ALERT, alertEventData);
      }
    });

    // Weed out irrelevant queries once the tree is updated
    const queryToReduceTo = this.__findQueryToReduceTo(tree, updates);
    if (queryToReduceTo) {
      this.__reduce(queryToReduceTo.parent, queryToReduceTo);
    }

    return theTree;
  }

  prepareForDelete(tree, type, target, deleteQuery) {
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
        const coveragePossible = tree.search((name, args) => this.__isOpalTaxonomy(args));

        if (coveragePossible) {
          // aggregation
          tree.addQuery(variableQuery, new RQL.Query(AGGREGATE, [new RQL.Query(BUCKET, [bucket])]));
          return tree;
        }
      }
    }

    return null;
  }

  prepareForGraphics(tree) {
  }

}

class VariableQuery extends EntityQuery {
  static get FIELDS() {
    return Mica.querySettings.variable.fields ? Mica.querySettings.variable.fields
      : ['attributes.label.*', 'attributes.description.*', 'variableType', 'valueType', 'categories.*', 'populationId', 'dceId', 'datasetId', 'datasetAcronym', 'attributes.Mlstr_area*'];
  }

  static get SORT_FIELDS() {
    return Mica.querySettings.variable.sortFields ? Mica.querySettings.variable.sortFields
      : ['studyId', 'datasetId', 'index', 'name'];
  }

  constructor(defaultSize, settings) {
    super(TYPES.VARIABLES, TARGETS.VARIABLE, defaultSize);
    this._fields = settings.fields ? settings.fields : VariableQuery.FIELDS;
    this._sortFields = settings.sortFields ? settings.sortFields : VariableQuery.SORT_FIELDS;
  }
}

class DatasetQuery extends EntityQuery {
  static get FIELDS() {
    return Mica.querySettings.dataset.fields ? Mica.querySettings.dataset.fields
      : ['acronym.*','name.*','variableType','studyTable.studyId','studyTable.project','studyTable.table','studyTable.populationId','studyTable.dataCollectionEventId','harmonizationTable.studyId','harmonizationTable.project','harmonizationTable.table','harmonizationTable.populationId'];
  }

  static get SORT_FIELDS() {
    return Mica.querySettings.dataset.sortFields ? Mica.querySettings.dataset.sortFields
      : ['studyTable.studyId','studyTable.populationWeight','studyTable.dataCollectionEventWeight','acronym'];
  }

  constructor(defaultSize, settings) {
    super(TYPES.DATASETS, TARGETS.DATASET, defaultSize);
    this._fields = settings.fields ? settings.fields : DatasetQuery.FIELDS;
    this._sortFields = settings.sortFields ? settings.sortFields : DatasetQuery.SORT_FIELDS;
  }
}

class StudyQuery extends EntityQuery {
  static get FIELDS() {
    return Mica.querySettings.study.fields ? Mica.querySettings.study.fields
      : ['acronym.*','name.*','model.methods.design','populations.dataCollectionEvents.model.dataSources','model.numberOfParticipants.participant'];
  }

  static get SORT_FIELDS() {
    return Mica.querySettings.study.sortFields ? Mica.querySettings.study.sortFields
      : ['acronym'];
  }


  constructor(defaultSize, settings) {
    super(TYPES.STUDIES, TARGETS.STUDY, defaultSize);
    this._fields = settings.fields ? settings.fields : StudyQuery.FIELDS;
    this._sortFields = settings.sortFields ? settings.sortFields : StudyQuery.SORT_FIELDS;
  }

  prepareForGraphics(tree) {
    const buckets = [
      'Mica_study.methods-design',
      'Mica_study.start-range',
      'Mica_study.populations-dataCollectionEvents-bioSamples',
      'Mica_study.populations-selectionCriteria-countriesIso',
    ];

    const aggregations = [
      'Mica_study.populations-selectionCriteria-countriesIso',
      'Mica_study.populations-dataCollectionEvents-bioSamples',
      'Mica_study.numberOfParticipants-participant-number'
    ];

    const query = new RQL.Query('in', ['Mica_study.className', 'Study']);
    let theTree = this.prepareForUpdatesAndSelection(tree, TYPES.STUDIES, [{target: TARGETS.STUDY, query, operator: 'and'}], true);
    let studyQuery = theTree.search(name => TARGETS.STUDY === name);
    theTree.addQuery(studyQuery, new RQL.Query(AGGREGATE, [...aggregations, ...[new RQL.Query(BUCKET,buckets)]]));
    theTree.addQuery(null, new RQL.Query(FACET));
    return tree;
  }
}

class NetworkQuery extends EntityQuery {
  static get FIELDS() {
    return Mica.querySettings.network.fields ? Mica.querySettings.network.fields
      : ['acronym.*','name.*','studyIds'];
  }

  static get SORT_FIELDS() {
    return Mica.querySettings.network.sortFields ? Mica.querySettings.network.sortFields
      : ['acronym'];
  }

  constructor(defaultSize, settings) {
    super(TYPES.NETWORKS, TARGETS.NETWORK, defaultSize);
    this._fields = settings.fields ? settings.fields : NetworkQuery.FIELDS;
    this._sortFields = settings.sortFields ? settings.sortFields : NetworkQuery.SORT_FIELDS;
  }
}

/**
 * Utility class to retrieve query related operation from URL; all operations are R/O
 */
class MicaTreeQueryUrl {
  /**
   * @returns {{hash: string, searchParams: {}}}
   */
  static parseUrl() {
    let urlParts = {
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
   * @param urlSearchParams
   * @returns {QueryTree|null}
   */
  static getQueryTreeFromUrl(urlSearchParams) {
    if (Object.keys(urlSearchParams).length > 0) {
      return new RQL.QueryTree(RQL.Parser.parseQuery(urlSearchParams.query), QueryTreeOptions);
    }

    return null;
  }

  static getTree(parts) {
    const urlParts = parts || MicaTreeQueryUrl.parseUrl();
    return MicaTreeQueryUrl.getQueryTreeFromUrl(urlParts.searchParams);
  }

  static getTreeQueries(parts) {
    let queries = {};
    const tree = MicaTreeQueryUrl.getTree(parts);

    if (tree) {
      const validNodes = CRITERIA_NODES.concat(OPERATOR_NODES);

      for (const key in TARGETS) {
        let targetQuery = tree.search((name) => {
          return name === TARGETS[key]
        });

        if (targetQuery) {
          targetQuery.args = targetQuery.args.filter(arg => validNodes.indexOf(arg.name) > -1);
          queries[TARGETS[key]] = targetQuery;
        }
      }
    }

    return queries;
  }

  static getStudyTypeSelection(tree) {
    const isAll = values => !values || Array.isArray(values) && values.length > 1;
    const isClassName = (name, values) => Array.isArray(values)
      ? values.length === 1 && values.indexOf(name) > -1
      : values === name;

    let selection = {all: true, study: false, harmonization: false};
    const theTree = tree || MicaTreeQueryUrl.getTree();
    const classNameQuery = theTree.search((name, args) => args.indexOf('Mica_study.className') > -1);
    if (classNameQuery) {
      const values = classNameQuery.args[1];
      selection.all = isAll.apply(null, [values]);
      selection.study = isClassName.apply(null, ['Study', values]);
      selection.harmonization = isClassName.apply(null, ['HarmonizationStudy', values]);
    } else {
      selection = {all: true, study: false, harmonization: false};
    }

    return selection;
  }

  static getDownloadUrl(payload) {
    let url, query;
    const tree = MicaTreeQueryUrl.getTree();

    if (DISPLAYS.COVERAGE === payload.display) {
      url = `${contextPath}/ws/variables/_coverage_download`;
      query = MicaTreeQueryUrl.getCoverageDownloadUrl(payload.bucket, tree);
    } else {
      url = `${contextPath}/ws/${payload.type}/_rql_csv`;
      query = MicaTreeQueryUrl.getSearchDownloadUrl(payload.type, tree);
    }

    if (query) {
      return {string: `${url}?query=${query}`, url, query, type: payload.type};
    } else {
      return null;
    }
  }

  static getSearchDownloadUrl(type, tree) {
    const target = TYPES_TARGETS_MAP[type];
    let fields, sortFields;

    switch (target) {
      case TARGETS.VARIABLE:
        fields = VariableQuery.FIELDS;
        sortFields = VariableQuery.SORT_FIELDS;
        break;
      case TARGETS.DATASET:
        fields = DatasetQuery.FIELDS;
        sortFields = DatasetQuery.SORT_FIELDS;
        break;
      case TARGETS.STUDY:
        fields = StudyQuery.FIELDS;
        sortFields = StudyQuery.SORT_FIELDS;
        break;
      case TARGETS.NETWORK:
        fields = NetworkQuery.FIELDS;
        sortFields = NetworkQuery.SORT_FIELDS;
        break;
    }

    let targetQuery = tree.search((name) => name === target);
    if (!targetQuery) {
      targetQuery = new RQL.Query(target);
      tree.addQuery(null, targetQuery);
    }

    tree.addQuery(targetQuery, new RQL.Query('fields', fields));
    tree.addQuery(targetQuery, new RQL.Query('sort', sortFields));

    let limitQuery = tree.search((name, args, parent) => 'limit' === name && target === parent.name);
    if (limitQuery) {
      limitQuery.args = [0, 100000]
    } else {
      tree.addQuery(targetQuery, new RQL.Query('limit', [0, 100000]));
    }

    tree.addQuery(null, new RQL.Query('locale', [Mica.locale]));

    return tree.serialize();
  }

  static getCoverageDownloadUrl(bucket, tree) {
    let variableQuery = tree.search(name => TARGETS.VARIABLE === name);

    if (variableQuery) {
      tree.addQuery(variableQuery, new RQL.Query(AGGREGATE, [new RQL.Query(BUCKET, [bucket])]));
      return tree.serialize();
    } else {
      return null;
    }
  }
}

class MicaQueryExecutor {

  constructor(eventBus, defaultSize, settings) {
    this._eventBus = eventBus;
    this._query = {};
    this._query[TYPES.VARIABLES] = new VariableQuery(defaultSize, settings.variable);
    this._query[TYPES.DATASETS] = new DatasetQuery(defaultSize, settings.dataset);
    this._query[TYPES.STUDIES] = new StudyQuery(defaultSize, settings.study);
    this._query[TYPES.NETWORKS] = new NetworkQuery(defaultSize, settings.network);
  }

  /**
   * Ensure a valid type either supplied by the payload or url
   */
  __ensureValidType(urlSearchParams, type) {
    let theType = TYPES[(type || urlSearchParams.type || "").toUpperCase()];
    if (!theType) {
      if (Mica.config.isSingleStudyEnabled) {
        if (Mica.config.isCollectedDatasetEnabled || Mica.config.isHarmonizedDatasetEnabled) {
          theType = TYPES.VARIABLES;
        } else if (!Mica.config.isSingleNetworkEnabled && Mica.config.isNetworkEnabled) {
          theType = TYPES.NETWORKS;
        }
      } else {
        theType = TYPES.STUDIES;
      }
    }
    return theType;
  }

  /**
   * Ensure a valid type either supplied by the payload or url
   */
  __ensureValidBucketType(urlSearchParams, bucket, display) {
    if (DISPLAYS.COVERAGE === display) {
      return BUCKETS[bucket] || (urlSearchParams.hasOwnProperty(BUCKET) ? BUCKETS[urlSearchParams[BUCKET]]: BUCKETS.study);
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
   * Common handler for all query-type events
   *
   * @param eventId
   * @param payload
   * @private
   */
  __prepareAndExecuteQuery(eventId, payload) {
    const urlParts = MicaTreeQueryUrl.parseUrl();
    const type = this.__ensureValidType(urlParts.searchParams, payload.type);
    const display = this.__ensureValidDisplay(urlParts.hash, payload.display);

    switch (type) {
      case TYPES.VARIABLES:
      case TYPES.DATASETS:
      case TYPES.STUDIES:
      case TYPES.NETWORKS:
        const entityQuery = this._query[type];
        const bucket = this.__ensureValidBucketType(urlParts.searchParams, payload.bucket, display);
        let tree = MicaTreeQueryUrl.getTree(urlParts);

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
        }

        if (tree) {
          switch (display) {
            case DISPLAYS.COVERAGE:
              const coverageTree = entityQuery.prepareForCoverage(tree, bucket);
              if (!coverageTree) {
                this.__ignoreCoverage(tree, type, display, payload.noUrlUpdate, bucket);
              } else {
                this.__executeCoverage(coverageTree, type, display, payload.noUrlUpdate, bucket);
              }
              break;

            case DISPLAYS.LISTS:
              this.__executeQuery(entityQuery.prepareForQuery(type, tree), type, display, payload.noUrlUpdate);
              break;

            case DISPLAYS.GRAPHICS:
              const graphicsTree = new RQL.QueryTree(RQL.Parser.parseQuery(tree.serialize()), QueryTreeOptions);
              this.__executeGraphicsQuery(tree, entityQuery.prepareForGraphics(graphicsTree), type, display, payload.noUrlUpdate);
              break;
          }
        }
    }
  }

  /**
   * Corrects LimitQuery when totalHits is inferior to 'from'
   *
   * @param type
   * @param response
   * @param limitQuery
   * @returns {boolean}
   * @private
   */
  __limitQueryCorrected(type, response, limitQuery) {
    const dto = `${TYPES_TARGETS_MAP[type]}ResultDto`;
    if (dto in response.data) {
      const totalHits = response.data[dto].totalHits;
      const from = limitQuery.args[0];

      if (totalHits > 0 && from >= totalHits) {
        console.debug(`Limit query needs correction: ${from} ${totalHits}`);
        const size = limitQuery.args[1];
        const pages = Math.ceil(totalHits / size);
        // correct the from
        limitQuery.args[0] = (pages - 1) * size;
        return true;
      }
    }

    return false;
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
    console.debug(`__executeQuery`);

    let studyTypeSelection = MicaTreeQueryUrl.getStudyTypeSelection(tree);

    axios
      .get(`${contextPath}/ws/${type}/_rql?query=${tree.serialize()}`)
      .then(response => {
        // remove hidden queries
        const limitQuery = tree.search((name, args, parent) => 'limit' === name && TYPES_TARGETS_MAP[type] === parent.name);

        if (this.__limitQueryCorrected(type, response, limitQuery)) {
          this.__executeQuery(tree, type, display, noUrlUpdate);
        } else {
          tree.findAndDeleteQuery((name) => 'fields' === name);
          tree.findAndDeleteQuery((name) => 'sort' === name);
          tree.findAndDeleteQuery((name) => 'locale' === name);
          this.__updateLocation(type, display, tree, noUrlUpdate);
          EventBus.$emit(`${type}-results`, {studyTypeSelection, type, response: response.data, from: limitQuery.args[0], size: limitQuery.args[1]});
        }
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
    console.debug(`__executeCoverage`);

    axios
      .get(`${contextPath}/ws/variables/_coverage?query=${tree.serialize()}`)
      .then(response => {
        tree.findAndDeleteQuery((name) => AGGREGATE === name);
        this.__updateLocation(type, display, tree, noUrlUpdate, bucket);
        EventBus.$emit(
          `coverage-results`,
          {
            bucket, response:
            response.data,
            studyTypeSelection:  MicaTreeQueryUrl.getStudyTypeSelection(tree)
          }
        );
      });
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
  __executeGraphicsQuery(tree, graphicsTree, type, display, noUrlUpdate) {
    console.debug(`__executeGraphicsQuery`);
    axios
      .get(`${contextPath}/ws/${type}/_rql?query=${graphicsTree.serialize()}`)
      .then(response => {
        // remove hidden queries
        tree.findAndDeleteQuery((name) => 'fields' === name);
        tree.findAndDeleteQuery((name) => 'sort' === name);
        tree.findAndDeleteQuery((name) => 'locale' === name);

        this.__updateLocation(type, display, tree, noUrlUpdate);
        EventBus.$emit(EVENTS.QUERY_TYPE_GRAPHICS_RESULTS, {response: response.data});
      });
  }

  __updateLocation(type, display, tree, replace, bucket) {
    const query = tree.serialize();
    let studyTypeSelection = MicaTreeQueryUrl.getStudyTypeSelection(tree);

    console.debug(`__updateLocation ${type} ${display} ${query} - history states ${history.length}`);
    let params = [`type=${type}`, `query=${query}`];
    if (bucket) {
      params.push(`bucket=${TARGET_ID_BUCKET_MAP[bucket]}`);
    }

    const urlSearch = params.join('&');
    const hash = `${display}?${urlSearch}`;
    if(replace) {
      history.replaceState(null, "", `#${hash}`);
    } else {
      history.pushState(null, "", `#${hash}`);
    }

    this._eventBus.$emit(EVENTS.LOCATION_CHANGED, {type, display, tree, bucket, studyTypeSelection});
  }

  /**
   * Handles hash changes and emits a query-type-selection event
   *
   * @param event
   * @private
   */
  __onHashChanged(event) {
    console.debug(`On hash changed ${event}`);
    const urlParts = MicaTreeQueryUrl.parseUrl();
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
    console.debug('On before unload', event);
    window.removeEventListener('hashchange', this.__onHashChanged);
    window.removeEventListener('beforeunload', this.__onBeforeUnload);
  }

  __onQueryTypeSelection(payload) {
    console.debug('__onQueryTypeSelection', payload);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_SELECTION, payload);
  }

  __onQueryTypeUpdate(payload) {
    console.debug('__onQueryTypeSelection', payload);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_UPDATE, payload);
  }

  __onQueryTypeUpdatesSelection(payload) {
    console.debug('__onQueryTypeUpdatesSelection', payload);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_UPDATES_SELECTION, payload);
  }

  __onQueryTypeDelete(payload) {
    console.debug('__onQueryTypeSelection', payload);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_DELETE, payload);
  }

  __onQueryTypePaginate(payload) {
    console.debug('__onQueryTypeSelection', payload);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_PAGINATE, payload);
  }

  __onQueryTypeCoverage(payload) {
    console.debug('__onQueryTypeSelection', payload);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_COVERAGE, payload);
  }

  __onQueryTypeGraphics(payload) {
    console.debug('__onQueryTypeGraphics', payload);
    this.__prepareAndExecuteQuery(EVENTS.QUERY_TYPE_GRAPHICS, payload);
  }

  init() {
    this._eventBus.register(EVENTS.QUERY_TYPE_SELECTION, this.__onQueryTypeSelection.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_UPDATE, this.__onQueryTypeUpdate.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_UPDATES_SELECTION, this.__onQueryTypeUpdatesSelection.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_DELETE, this.__onQueryTypeDelete.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_PAGINATE, this.__onQueryTypePaginate.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_COVERAGE, this.__onQueryTypeCoverage.bind(this));
    this._eventBus.register(EVENTS.QUERY_TYPE_GRAPHICS, this.__onQueryTypeGraphics.bind(this));
    window.addEventListener('hashchange', this.__onHashChanged.bind(this));
    window.addEventListener('beforeunload', this.__onBeforeUnload.bind(this));
  }

  destroy() {
    this._eventBus.unregister(EVENTS.QUERY_TYPE_SELECTION, this.__onQueryTypeSelection);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_UPDATE, this.__onQueryTypeUpdate);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_UPDATES_SELECTION, this.__onQueryTypeUpdatesSelection);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_DELETE, this.__onQueryTypeDelete);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_COVERAGE, this.__onQueryTypeCoverage);
    this._eventBus.unregister(EVENTS.QUERY_TYPE_GRAPHICS, this.__onQueryTypeGraphics);
  }
}


