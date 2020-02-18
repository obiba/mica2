class EntityQuery {
  constructor() {
    this._fields = [];
    this._sortFields = [];
  }

  getFields() {
    return this._fields;
  }

  getFieldsCsv() {
    return this._fields.join(',');
  }

  getSortFields() {
    return this._sortFields;
  }

  getSortFieldsCsv() {
    return this._sortFields.join(',');
  }

  prepareQuery(query, from, size) {
    return "";
  }

  prepareQueryForUrl(query, from, size) {
    return ""
  }

}

class VariableQuery extends EntityQuery {
  constructor() {
    super();
    this._fields = ['attributes.label.*', 'variableType', 'datasetId', 'datasetAcronym', 'attributes.Mlstr_area*'];
    this._sortFields = ['variableType,containerId', 'populationWeight', 'dataCollectionEventWeight', 'datasetId', 'index', 'name'];
  }

  /**
   *
   * @param query
   * @param from
   * @param size
   * @returns {string}
   */
  prepareQuery(query, limit) {
    return `variable(limit(${limit.from},${limit.size}),fields((${this.getFieldsCsv()})),sort(${this.getSortFieldsCsv()}))`;
  }

  prepareQueryForUrl(query, limit) {
    return `variable(limit(${limit.from},${limit.size}))`;
  }

}

class MicaQueryExecutor {

  constructor(eventBus) {
    this._eventBus = eventBus;
    this._query = {
      'variables-list': new VariableQuery(),
      'datasets-list': new EntityQuery(),
      'studies-list': new EntityQuery(),
      'networks-list': new EntityQuery()
    };
  }

  __prepareAndExecuteQuery(payload) {
    switch (payload.type) {
      case 'variables-list':
        const limit = {
          from: payload.from || 0,
          size: payload.size || 20
        };

        const entityQuery = this._query[payload.type];
        const query = entityQuery.prepareQuery(null, limit);
        this.__executeQuery(`${query},locale(en)`,payload.type.replace(/-.*$/,""));
        this.__updateLocation(payload.type, entityQuery.prepareQueryForUrl(null, limit));
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

  __executeQuery(query, target, from) {
    axios
      .get(`../ws/${target}/_rql?query=${query}`)
      .then(response => {
        EventBus.$emit('variables-results', {response: response.data, from: from});
      });
  }

  __onHashChanged(event) {
    console.log(`On hash changed ${event}`);
    // TODO URL changed, need to update the query and execute it
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
    this.__prepareAndExecuteQuery(payload);
  }

  __updateLocation(type, query) {
    const urlSearch = [`query=${query}`].join('&');
    const hash = `${type}?${urlSearch}`;
    history.replaceState(null, null, `#${hash}`);
  }

  init() {
    this._eventBus.register('query-type-selection', this.__onQueryTypeSelection.bind(this));
    window.addEventListener('hashchange', this.__onHashChanged.bind(this));
    window.addEventListener('beforeunload', this.__onBeforeUnload.bind(this));
  }

  destroy() {
    this._eventBus.unregister('query-type-selection', this.__onQueryTypeSelection);
  }
}


