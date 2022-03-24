class StringLocalizer {
  static __localizeInternal(entries, locale) {
    const result = (Array.isArray(entries) ? entries : [entries]).filter((entry) => entry && (locale === entry.lang || locale === entry.locale)).pop();

    if (result) {
      let value = result.value ? result.value : result.text;
      return value ? value : null;
    }
    return null;
  }

  static localize(entries) {
    if (entries) {
      const result = StringLocalizer.__localizeInternal(entries, Mica.locale)
        || StringLocalizer.__localizeInternal(entries, Mica.defaultLocale)
        || StringLocalizer.__localizeInternal(entries, 'und');

      return result ? result : '';
    } else {
      return '';
    }
  }
}

// Register all filters

Vue.filter("ellipsis", (input, n, link) => {
  if (input.length <= n) { return input; }
  const subString = input.substr(0, n-1); // the original check
  const anchor = link ? ` <a href="${link}">...</a>` : " ...";
  return subString.substr(0, subString.lastIndexOf(" ")) + anchor;
});

Vue.filter("readmore", (input, link, text) => {
  return `${input} <a href="${link}" class="clearfix btn-link">${text}</a>`;
});

Vue.filter("concat", (input, suffix) => {
  return input + suffix;
});

Vue.filter("localize-string", (input) => {
  if (typeof input === "string") return input;
  return StringLocalizer.localize(input);
});

Vue.filter("markdown", (input) => {
  return marked(input);
});

Vue.filter("localize-number", (input) => {
  return (input || 0).toLocaleString();
});

Vue.filter("translate", (key) => {
  let value = Mica.tr[key];
  return typeof value === "string" ? value : key;
});

/**
 * Base class for all entities (Srtudies, Networks, Datasets)
 */
class ObibaEntitiesService {

  __getResource(url, onsuccess, onfailure) {
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        if (onfailure) {
          // onfailure(response);
          console.error(`Failed to retrieve ${studyId} networks: ${response}`);
        }
      });
  }

  __parse() {
    const search = window.location.search;
    let urlParts = {}

    if (search) {
      const index = search.indexOf('?')
      if (index > -1) {
        const params = search.substring(index + 1).split('&');
        params.forEach(param => {
          const parts = param.split('=');
          urlParts[parts[0]] = parts[1] || "";
        });
      }
    }

    return urlParts;
  }

  prepareQuery(locale, from, size) {
    const urlParts = this.__parse();
    let tree = new RQL.QueryTree(RQL.Parser.parseQuery(urlParts.query), {containers: ['variable', 'dataset', 'study', 'network']});
    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery)  {
      targetQuery = new RQL.Query(this.target);
      tree.addQuery(null, targetQuery);
    }

    let limitQuery = tree.search((name,args,parent) => name === 'limit' && targetQuery.name === parent.name);
    if (!limitQuery) {
      tree.addQuery(targetQuery, new RQL.Query('limit', [0, DEFAULT_PAGE_SIZE]));
    } else if (from !== undefined) {
      limitQuery.args[0] = from;
      limitQuery.args[1] = size;
    }

    tree.addQuery(targetQuery, new RQL.Query('fields', this.fields));

    let sortQuery = tree.search((name,args,parent) => name === 'sort' && targetQuery.name === parent.name);
    if (!sortQuery) {
      tree.addQuery(targetQuery, new RQL.Query('sort', ['name']));
    }

    tree.addQuery(null, new RQL.Query('locale', [locale]));

    return tree;
  }

  updateLocation(tree, replace) {
    tree.findAndDeleteQuery((name) => 'fields' === name);
    tree.findAndDeleteQuery((name) => 'locale' === name);
    const query = tree.serialize();

    if (replace) {
      window.history.replaceState(null, "", `?query=${query}`);
    } else {
      window.history.pushState(null, "", `?query=${query}`);
    }
  }

  getLimitQueryValues(tree) {
    let limits = {from: 0, size: DEFAULT_PAGE_SIZE};
    const limitQuery = tree.search((name,args,parent) => name === 'limit' && parent.name === this.target);
    if (limitQuery && limitQuery.args) {
      limits.from = limitQuery.args[0];
      limits.size = limitQuery.args[1];
    }

    return limits;
  }

  getFilterQueryValue(tree) {
    const filterQuery = tree.search((name, args, parent) => name === 'filter' && args[0].name === 'match' && parent.name === this.target);
    if (filterQuery && filterQuery.args) {
      return filterQuery.args[0].args[0];
    } else {
      return '';
    }
  }

  updateFilter(tree, text) {
    if (text && text.length > 1) {
      let targetQuery = tree.search((name) => name === this.target);
      if (!targetQuery)  {
        targetQuery = new RQL.Query(this.target);
        tree.addQuery(null, targetQuery);
      }

      const filterQuery = tree.search((name, args, parent) => name === 'filter' && args[0].name === 'match' && parent.name === this.target);
      if (!filterQuery) {
        tree.addQuery(targetQuery, new RQL.Query('filter', [new RQL.Query('match', [text])]));
      } else {
        filterQuery.args[0].args[0] = text
      }
    } else {
      tree.findAndDeleteQuery((name) => 'filter' === name);
    }
  }

  getEntities(query, onsuccess, onfailure) {
    let url = `/ws/${this.resourcePath}/_rql?query=${query}`;
    this.__getResource(url, onsuccess, onfailure);
  }

  getSuggestions(text, locale, onsuccess, onfailure) {
    let url = `/ws/${this.resourcePath}/_suggest?query=${text}&locale=${locale}`;
    this.__getResource(url, onsuccess, onfailure);
  }

  updateSort(tree, sort) {
    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery)  {
      targetQuery = new RQL.Query(this.target);
      tree.addQuery(null, targetQuery);
    }

    let sortQuery = tree.search((name, args, parent) => name === 'sort' && targetQuery.name === parent.name);
    if (!sortQuery) {
      tree.addQuery(targetQuery, new RQL.Query('sort', [sort]));
    } else {
      sortQuery.args[0] = sort;
    }
  }
}

/**
 * Datasets servive class
 */
class ObibaDatasetsService extends ObibaEntitiesService {

  static newInstance(type) {
    return new ObibaDatasetsService(type);
  }

  constructor(type) {
    super();
    this.type = type;
  }

  get target() {
    return 'dataset';
  }

  get resourcePath() {
    return 'datasets';
  }


  get fields() {
    return ['acronym.*','name.*','description.*','variableType','studyTable.studyId','studyTable.project','studyTable.table','studyTable.populationId','studyTable.dataCollectionEventId','harmonizationTable.studyId','harmonizationTable.project','harmonizationTable.table','harmonizationTable.populationId']
  }

  prepareQuery(locale, from, size) {
    let tree = super.prepareQuery(locale, from, size);

    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery) {
      throw new Error(`Target query ${this.target} not found.`)
    }

    let classNameQuery;
    if (this.type === 'datasets') {
      classNameQuery = tree.search((name, agrs) => agrs.indexOf('Mica_dataset.className') > -1);
      if (classNameQuery) {
        tree.deleteQuery(classNameQuery);
      }
    } else {
      const classType = this.type === 'collected-datasets' ? 'StudyDataset' : 'HarmonizationDataset';

      // add className query
      classNameQuery = tree.search((name, args, parent) => args.indexOf('Mica_dataset.className') > -1 && parent.name === this.target);
      if (!classNameQuery) {
        classNameQuery = new RQL.Query('in');
        tree.addQuery(targetQuery, classNameQuery);
      }

      classNameQuery.name = 'in';
      classNameQuery.args = ['Mica_dataset.className', classType];
    }

    return tree;
  }
}

/**
 * Studies servive class
 */
class ObibaStudiesService extends ObibaEntitiesService {

  static newInstance(type) {
    return new ObibaStudiesService(type);
  }

  constructor(type) {
    super();
    this.type = type;
  }

  get target() {
    return 'study';
  }

  get resourcePath() {
    return 'studies';
  }

  get fields() {
    return ['acronym.*','name.*','objectives.*','logo','model.methods.design','model.numberOfParticipants.participant']
  }

  prepareQuery(locale, from, size) {
    let tree = super.prepareQuery(locale, from, size);

    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery) {
      throw new Error(`Target query ${this.target} not found.`)
    }

    let classNameQuery;
    if (this.type === 'studies') {
      classNameQuery = tree.search((name, agrs) => agrs.indexOf('Mica_study.className') > -1);
      if (classNameQuery) {
        tree.deleteQuery(classNameQuery);
      }
    } else {
      const classType = this.type === 'individual-studies' ? 'Study' : 'HarmonizationStudy';

      // add className query
      classNameQuery = tree.search((name, args, parent) => args.indexOf('Mica_study.className') > -1 && parent.name === this.target);
      if (!classNameQuery) {
        classNameQuery = new RQL.Query('in');
        tree.addQuery(targetQuery, classNameQuery);
      }

      classNameQuery.name = 'in';
      classNameQuery.args = ['Mica_study.className', classType];
    }

    return tree;
  }

  getEntities(query, onsuccess, onfailure) {
    const savedCallback = onsuccess;
    const newOnsuccess = (response) => {
      let dto = response.studyResultDto;
      if (dto && dto['obiba.mica.StudyResultDto.result']) {
        dto['obiba.mica.StudyResultDto.result'].summaries.forEach((summary) => {
          if (summary.content) {
            summary.model = JSON.parse(summary.content);
          }
        })
      }

      if (savedCallback) {
        savedCallback(response);
      }
    };

    super.getEntities(query, newOnsuccess, onfailure);
  }
}

/**
 * Networks service class
 */
class ObibaNetworksService extends ObibaEntitiesService {

  static newInstance() {
    return new ObibaNetworksService();
  }

  get target() {
    return 'network';
  }

  get resourcePath() {
    return 'networks';
  }

  get fields() {
    return ['acronym.*','name.*','description.*','studyIds','logo']
  }
}

/**
 * Component for rendering stat count
 */
const StatItemComponent = {
  props: {
    count: Number,
    singular: String,
    plural: String,
    url: String
  },
  template: `
    <a v-if="count" v-bind:href="url" class="btn btn-sm btn-link col text-left">
      <span class="h6 pb-0 mb-0 d-block">{{count | localize-number}}</span>
      <span class="text-muted"><small>{{count < 2 ? this.singular : this.plural}}</small></span>
    </a>
  `
};

/**
 * Component for rendering variable stat count
 */
const VariableStatItemComponent =  {
  props: {
    type: String,
    stats: Object,
    url: String
  },
  data() {
    return {
      count: 0,
      countLabel: null
    }
  },
  template: `
    <a v-if="count" v-bind:href="url" class="btn btn-sm btn-link col text-left">
      <span class="h6 pb-0 mb-0 d-block">{{count | localize-number}}</span>
      <span class="text-muted"><small>{{this.countLabel}}</small></span>
    </a>
  `,
  mounted: function() {
    let prefix;
    if (['individual-study', 'harmonization-study'].indexOf(this.type) > -1) {
      prefix = `${this.type === 'individual-study' ? '' : 'harmonized-'}`;
    } else if (['Collected', 'Dataschema', 'Harmonized'].indexOf(this.type) > -1){
      prefix = `${this.type === 'Collected' ? '' : 'harmonized-'}`;
    } else {
      throw new Error(`Invalid Datset variableType ${this.type}`);
    }

    this.count = this.stats.variables;
    this.countLabel = this.count < 2 ? Mica.tr[`${prefix}variable`] : Mica.tr[`${prefix}variables`];
  }
};

/**
 * Component for rendering dataset stat count
 */
const DatasetStatItemComponent =  {
  props: {
    type: String,
    stats: Object
  },
  data() {
    return {
      count: 0,
      countLabel: null
    }
  },
  template: `
    <a v-if="count" href="javascript:void(0)" style="cursor: initial;" class="btn btn-sm col text-left">
      <span class="h6 pb-0 mb-0 d-block">{{count | localize-number}}</span>
      <span class="text-muted"><small>{{this.countLabel}}</small></span>
    </a>
  `,
  mounted: function() {
    let key = this.type === 'individual-study' ? 'studyDatasets' : 'harmonizationDatasets';
    let prefix = this.type === 'individual-study' ? 'collected' : 'harmonized';

    this.count = this.stats[key];
    this.countLabel = this.count < 2 ? Mica.tr[`${prefix}-dataset`] : Mica.tr[`${prefix}-datasets`];
  }
};

/**
 * Component for rendering study stat count
 */
const StudyStatItemComponent =  {
  props: {
    type: String,
    stats: Object
  },
  data() {
    return {
      count: 0,
      countLabel: null
    }
  },
  template: `
    <a v-if="count" href="javascript:void(0)" style="cursor: initial;" class="btn btn-sm col text-left">
      <span class="h6 pb-0 mb-0 d-block">{{count | localize-number}}</span>
      <span class="text-muted"><small>{{this.countLabel}}</small></span>
    </a>
  `,
  mounted: function() {
    let singular = '', plural = '';

    if (['individual-study', 'harmonization-study'].indexOf(this.type) > -1) {
      singular = `${this.type === 'individual-study' ? 'study' : 'harmonization-study'}`;
      plural = `${this.type === 'individual-study' ? 'studies' : 'harmonization-studies'}`;
    } else if (['Collected', 'Dataschema', 'Harmonized'].indexOf(this.type) > -1){
      singular = `${this.type === 'Collected' ? 'study' : 'harmonization-study'}`;
      plural = `${this.type === 'Collected' ? 'studies' : 'harmonization-studies'}`;
    } else {
      throw new Error(`Invalid Dataset variableType ${this.type}`);
    }

    this.count = this.count = this.stats.studies;
    this.countLabel = this.count < 2 ? Mica.tr[`${singular}`] : Mica.tr[`${plural}`];
  }
};

/**
 * Component for rendering a sorting widget
 */
const EntitiesSortingComponent = {
  template: `
    <div class="sorting position-relative float-left">
      <div class="dropdown">
        <button type="button" class="btn btn-outline-primary dropdown-toggle" data-toggle="dropdown">
          <span v-html="sortLabel(selectedChoice)"></span>
        </button>

        <div class="dropdown-menu">
          <button class="dropdown-item" type="button" v-for="option in options" :key="option.key" v-html="option.label" @click="changeSort(option.key)"></button>
        </div>
      </div>
    </div>
  `,
  props: {
    optionsTranslations: Object,
    initialChoice: String
  },
  data() {
    return {
      selectedChoice: this.initialChoice || "name",
    };
  },
  watch: {
    initialChoice(value, old) {
      if (value !== old) {
        this.selectedChoice = value || "name";
      }
    }
  },
  computed: {
    options() {
      const output = [];
      Object.keys(this.optionsTranslations).forEach(k => {
        output.push({ key: k, label: `${this.optionsTranslations[k]} <i class="fas fa-long-arrow-alt-up"></i>` });
        output.push({ key: `-${k}`, label: `${this.optionsTranslations[k]} <i class="fas fa-long-arrow-alt-down"></i>` });
      });

      return output;
    }
  },
  methods: {
    changeSort(choice) {
      this.selectedChoice = choice;
      this.$emit("sort-update", this.selectedChoice);
    },
    sortLabel(choice) {
      return this.options.filter(option => option.key === choice)[0].label;
    }
  }
};

/**
 * Typeahead UI component
 */
const TypeaheadComponent = {
  template: `
    <div class="typeahead w-100 position-relative">
      <div class="input-group">
        <input type="text" :placeholder="'listing-typeahead-placeholder'   | translate" class="form-control form-control-sm" v-model="text" @keyup="typing($event)">
        <div class="input-group-append">
          <button type="button" class="btn btn-primary btn-sm" @click="select(text)"><i class="fas fa-filter"></i></button>
        </div>
        <button type="button" class="close position-absolute" style="right: 2em; top: 0.25em;" v-if="text.length > 0" @click="clear"><span aria-hidden="true">&times;</span></button>
      </div>

      <div class="list-group position-absolute mt-1 ml-1 shadow" style="z-index: 100; overflow-y: auto; max-height: 16em;" v-if="showChoices && typeaheadItems.length > 0">
        <button type="button" class="list-group-item list-group-item-action" :class="{ active: index === currentIndexSelection }" v-for="(item, index) in typeaheadItems" :key="item" v-html="highlight(item)" @click="select(quote(item))"></button>
      </div>
    </div>
  `,
  props: {
    items: Array,
    externalText: String,
  },
  watch: {
    externalText(value, old) {
      if (value && value !== old) {
        this.text = value;
      }
    },
  },
  data() {
    return {
      text: this.externalText || "",
      showChoices: false,
      currentIndexSelection: -1,
    };
  },
  computed: {
    typeaheadItems() {
      return (this.items || []).filter((item) => item.toLowerCase().indexOf(this.text.toLowerCase()) >= 0);
    },
  },
  methods: {
    typing(event) {
      if (event.keyCode === 13) {
        this.select(this.text);
        return;
      }

      if (event.keyCode === 27) { // escape
        this.clear();
        return;
      }

      if (event.keyCode === 37 || event.keyCode === 38 || event.keyCode === 39 || event.keyCode === 40) { // arrows
        this.changeIndexSelection(event.keyCode);
        return;
      }

      this.$emit("typing", this.cleanUnclosedDoubleQuotes(this.text));

      this.showChoices = true;
      this.currentIndexSelection = -1;
    },
    select(selected) {
      this.text = this.quote(this.currentIndexSelection === -1 ? selected : this.items[this.currentIndexSelection]);
      this.$emit("select", this.text);

      this.showChoices = false;
      this.currentIndexSelection = -1;
    },
    clear() {
      this.select('');
    },
    changeIndexSelection(keyCode) {
      if (keyCode === 37 || keyCode === 38) { // up
        if (this.currentIndexSelection <= 0) {
          this.currentIndexSelection = this.typeaheadItems.length;
        }

        this.currentIndexSelection--;
      } else if (keyCode === 39 || keyCode === 40) { // down
        if (this.currentIndexSelection >= this.typeaheadItems.length - 1) {
          this.currentIndexSelection = -1;
        }

        this.currentIndexSelection++;
      }
    },
    highlight(item) {
      let output = item;
      const index = item.toLowerCase().indexOf(this.text.toLowerCase());
      if (index >= 0) {
        output = `${output.substring(0, index)}<strong>${output.substring(index, index + this.text.length)}</strong>${output.substring(index + this.text.length)}`;
      }

      return output;
    },
    quote(text) {
      if ((text || "").trim().length > 0) {
        return `"${text.trim().replace(/^"|"$/g, "").replace(/"/, '\\"')}"`;
      }

      return text;
    },
    cleanUnclosedDoubleQuotes(text) {
      let output = (text || "").trim();
      const doubleQuotesRegxp = /"/g;
      const instancesOfDoubleQuoteCharacters = (output.match(doubleQuotesRegxp) || []).length;

      if (instancesOfDoubleQuoteCharacters % 2 !== 0) {
        return output.replace(doubleQuotesRegxp, "");
      }

      return output;
    },
  },
};

/**
 * Base Vue App for Entities (Studies, Networks, Datasets)
 *
 */
const NAVIGATION_POSITIONS = ['bottom', 'top'];

const ObibaEntitiesApp = {
  data() {
    return {
      loading: false,
      locale: 'en',
      pageSizeSelectors: {},
      paginations: {},
      service: null,
      entities: [],
      suggestions: [],
      suggestionTimeoutId: null,
      initialFilter: '',
      total: 0
    }
  },
  mounted: function() {
    window.addEventListener('beforeunload', this.onBeforeUnload.bind(this));
    window.addEventListener('popstate', this.onLocationChanged.bind(this));
    NAVIGATION_POSITIONS.forEach(pos => { //
      this.pageSizeSelectors[`obiba-page-size-selector-${pos}`] = new OBiBaPageSizeSelector(`obiba-page-size-selector-${pos}`, DEFAULT_PAGE_SIZES, DEFAULT_PAGE_SIZE, this.onPageSizeChanged)
      this.paginations[`obiba-pagination-${pos}`] = new OBiBaPagination(`obiba-pagination-${pos}`, true, this.onPagination);
    });
  },
  methods: {
    ensureStudyClassNameQuery: function(studyType, query) {
      const classNameQuery = 'harmonization-study' === studyType ? 'in(Mica_study.className,HarmonizationStudy)' : 'in(Mica_study.className,Study)';
      return query ? `and(${classNameQuery},${query})` : classNameQuery;
    },
    searchModeFromStudyType(studyType) {
      return 'harmonization-study' === studyType ? 'harmonization-search' : 'individual-search';
    },
    onBeforeUnload: function() {
      window.removeEventListener('beforeunload', this.onBeforeUnload.bind(this));
      window.removeEventListener('popstate', this.onLocationChanged.bind(this));
    },
    ensureEvenEntities: function() {
      if (this.entities.length % 2 !== 0) {
        this.entities.push({name:'', description:'',id:''});
      }
    },
    getResultDtoField: function() {
      throw new Error("getResultDtoField() must be deinfed in subclass");
    },
    getEntities: function(queryTree) {
      this.loading = true;
      this.service.getEntities(queryTree.serialize(), (response) => {
        this.loading = false;
        this.setEntities(response);
        const limits = this.service.getLimitQueryValues(queryTree);
        NAVIGATION_POSITIONS.forEach(pos => {
          this.pageSizeSelectors[`obiba-page-size-selector-${pos}`].update(limits.size)
          this.paginations[`obiba-pagination-${pos}`].update(response[this.getResultDtoField()].totalHits, limits.size,(limits.from/limits.size)+1);
        });
        this.service.updateLocation(queryTree, true);
      });
    },
    onLocationChanged: function (event) {
      // Log the state data to the console
      const queryTree = this.service.prepareQuery(this.locale);
      this.service.getEntities(queryTree.serialize(), (response) => {
        const limits = this.service.getLimitQueryValues(queryTree);
        NAVIGATION_POSITIONS.forEach(pos => {
          this.pageSizeSelectors[`obiba-page-size-selector-${pos}`].update(limits.size);
          this.paginations[`obiba-pagination-${pos}`].update(response[this.getResultDtoField()].totalHits, limits.size,(limits.from/limits.size)+1);
        });
        this.setEntities(response);
      });
    },
    onPageSizeChanged: function(data) {
      const queryTree = this.service.prepareQuery(this.locale, 0, data.size);
      this.service.getEntities(queryTree.serialize(), (response) => {
        NAVIGATION_POSITIONS.forEach(pos => {
          if (data.id !== `obiba-page-size-selector-${pos}`) {
            this.pageSizeSelectors[`obiba-page-size-selector-${pos}`].update(data.size);
          }
          this.paginations[`obiba-pagination-${pos}`].changePageSize(data.size);
        });
        this.service.updateLocation(queryTree);
        this.setEntities(response);
      });
    },
    onPagination: function(data) {
      const queryTree = this.service.prepareQuery(this.locale, data.from, data.size);
      this.service.getEntities(queryTree.serialize(), (response) => {
        NAVIGATION_POSITIONS.forEach(pos => {
          if (data.id !== `obiba-page-size-pagination-${pos}`) {
            this.paginations[`obiba-pagination-${pos}`].update(response[this.getResultDtoField()].totalHits, data.size,(data.from/data.size)+1);
          }
        });
        this.setEntities(response);
        this.service.updateLocation(queryTree);
      });
    },
    onType: function(text) {
      this.suggestions = [];
      if (this.suggestionTimeoutId) {
        clearTimeout(this.suggestionTimeoutId);
      }

      this.suggestionTimeoutId = setTimeout(() => this.service.getSuggestions(text, this.locale, (response) => {
        this.suggestions = Array.isArray(response) ? response : [];
      }), 250);
    },
    onSelect: function(selectedText) {
      const queryTree = this.service.prepareQuery(this.locale);
      this.service.updateFilter(queryTree, selectedText);
      this.getEntities(queryTree);

      this.initialFilter = selectedText;
    },
    onSortUpdate: function(sort) {
      const queryTree = this.service.prepareQuery(this.locale);
      this.service.updateSort(queryTree, sort);

      this.getEntities(queryTree);
    }
  }
}

class ObibaDatasetsApp {

  static build(element, type, locale, sortOptionsTranslations) {
    return new Vue({
      locale,
      el: element,
      extends: ObibaEntitiesApp,
      data() {
        return {
          sortOptionsTranslations,
          initialSort: 'name'
        };
      },
      components: {
        'variable-stat-item' : VariableStatItemComponent,
        'study-stat-item' : StudyStatItemComponent,
        'stat-item' : StatItemComponent,
        'typeahead': TypeaheadComponent,
        'sorting': EntitiesSortingComponent
      },
      mounted: function () {
        this.service =  ObibaDatasetsService.newInstance(type);
        const queryTree = this.service.prepareQuery(locale);
        this.service.updateSort(queryTree, this.initialSort);
        this.getEntities(queryTree);

        this.initialFilter = this.service.getFilterQueryValue(this.service.prepareQuery(locale));
      },
      methods: {
        variablesUrl: function(dataset) {
          const studyResourcePath = dataset.variableType === "Collected" ? "individual-study" : "harmonization-study";
          const studyQuery = this.ensureStudyClassNameQuery(studyResourcePath);
          return MicaService.normalizeUrl(`/${this.searchModeFromStudyType(studyResourcePath)}#lists?type=variables&query=study(${studyQuery}),variable(in(Mica_variable.variableType,${dataset.variableType})),dataset(in(Mica_dataset.id,${dataset.id}))`)
        },
        networks: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=networks&query=dataset(in(Mica_dataset.id,${id}))`);
        },
        studies: function(dataset) {
          const studyType = dataset.variableType === 'Dataschema' ? 'harmonization-study' : 'individual-study';
          const studyQuery = this.ensureStudyClassNameQuery(studyType);
          return MicaService.normalizeUrl(`/${this.searchModeFromStudyType(studyType)}#lists?type=studies&query=study(${studyQuery}),dataset(in(Mica_dataset.id,${dataset.id}))`);
        },
        variables: function(dataset) {
          const studyType = dataset.variableType === 'Dataschema' ? 'harmonization-study' : 'individual-study';
          const studyQuery = this.ensureStudyClassNameQuery(studyType);
          return MicaService.normalizeUrl(`/${this.searchModeFromStudyType(studyType)}#lists?type=variables&query=study(${studyQuery}),dataset(in(Mica_dataset.id,${dataset.id}))`);
        },
        hasStats: function(dataset) {
          const countStats = dataset['obiba.mica.CountStatsDto.datasetCountStats'];
          return countStats.variables + countStats.studies + countStats.networks > 0
        },
        getResultDtoField: function () {
          return 'datasetResultDto';
        },
        setEntities: function(response) {
          const dto = response.datasetResultDto;
          if (dto && dto['obiba.mica.DatasetResultDto.result'] && dto['obiba.mica.DatasetResultDto.result'].datasets) {
            this.entities = dto['obiba.mica.DatasetResultDto.result'].datasets;
            this.total = dto.totalHits;
            this.ensureEvenEntities();
          }
        }
      }
    });
  }
}

class ObibaStudiesApp {

  static build(element, type, locale, sortOptionsTranslations) {
    return new Vue({
      el: element,
      extends: ObibaEntitiesApp,
      data() {
        return {
          locale,
          sortOptionsTranslations,
          initialSort: type === 'harmonization-studies' ? '-lastModifiedDate' : 'name'
        };
      },
      components: {
        'dataset-stat-item' : DatasetStatItemComponent,
        'variable-stat-item' : VariableStatItemComponent,
        'typeahead': TypeaheadComponent,
        'sorting': EntitiesSortingComponent
      },
      mounted: function () {
        this.service =  ObibaStudiesService.newInstance(type);

        const queryTree = this.service.prepareQuery(locale);
        this.service.updateSort(queryTree, this.initialSort);
        this.getEntities(queryTree);

        this.initialFilter = this.service.getFilterQueryValue(this.service.prepareQuery(locale));
      },
      methods: {
        variablesUrl: function(study) {
          const studyQuery = this.ensureStudyClassNameQuery(study.studyResourcePath, `in(Mica_study.id,${study.id})`)
          let variableType = study.studyResourcePath === 'individual-study' ? 'Collected' : 'Dataschema';
          return MicaService.normalizeUrl(`/${this.searchModeFromStudyType(study.studyResourcePath)}#lists?type=variables&query=study(${studyQuery}),variable(in(Mica_variable.variableType,${variableType}))`)
        },
        hasStats: function(study) {
          const countStats = study['obiba.mica.CountStatsDto.studyCountStats'];
          const datasetStats = type === 'individual-studies' ? countStats.studyDatasets : countStats.harmonizationDatasets
          let hasModelStats = study.model
            && (study.model.numberOfParticipants && study.model.numberOfParticipants.number
              || study.model.methods && study.model.methods.design);

          return datasetStats + countStats.variables > 0 || hasModelStats;
        },
        getResultDtoField: function () {
          return 'studyResultDto';
        },
        setEntities: function(response) {
          const dto = response.studyResultDto;
          if (dto && dto['obiba.mica.StudyResultDto.result'] && dto['obiba.mica.StudyResultDto.result'].summaries) {
            this.entities = dto['obiba.mica.StudyResultDto.result'].summaries;
            this.total = dto.totalHits;
            this.ensureEvenEntities();
          }
        }
      }
    });
  }
}

class ObibaNetworksApp {

  static build(element, locale, sortOptionsTranslations) {
    return new Vue({
      el: element,
      extends: ObibaEntitiesApp,
      data() {
        return {
          locale,
          sortOptionsTranslations,
          initialSort: "-numberOfStudies",
        };
      },
      components: {
        'stat-item': StatItemComponent,
        'typeahead': TypeaheadComponent,
        'sorting': EntitiesSortingComponent
      },
      mounted: function () {
        this.service = ObibaNetworksService.newInstance();
        const queryTree = this.service.prepareQuery(locale);
        this.service.updateSort(queryTree, this.initialSort);
        this.getEntities(queryTree);

        this.initialFilter = this.service.getFilterQueryValue(this.service.prepareQuery(locale));
      },
      methods: {
        individualStudies: function(id) {
          const studyQuery = this.ensureStudyClassNameQuery('individual-study', 'in(Mica_study.className,Study)');
          return MicaService.normalizeUrl(`/individual-search#lists?type=studies&query=network(in(Mica_network.id,${id})),study(${studyQuery})`);
        },
        individualStudiesWithVariables: function(id) {
          const studyQuery = this.ensureStudyClassNameQuery('individual-study');
          return MicaService.normalizeUrl(`/individual-search#lists?type=studies&query=network(in(Mica_network.id,${id})),variable(in(Mica_variable.variableType,Collected)),study(${studyQuery})`);
        },
        individualStudyVariables: function(id) {
          const studyQuery = this.ensureStudyClassNameQuery('individual-study');
          return MicaService.normalizeUrl(`/individual-search#lists?type=variables&query=network(in(Mica_network.id,${id})),variable(in(Mica_variable.variableType,Collected)),study(${studyQuery})`);
        },
        harmonizationStudies: function(id) {
          return MicaService.normalizeUrl(`/harmonization-search#lists?type=studies&query=network(in(Mica_network.id,${id})),study(in(Mica_study.className,HarmonizationStudy))`);
        },
        harmonizationStudyVariables: function(id) {
          const studyQuery = this.ensureStudyClassNameQuery('harmonization-study');
          return MicaService.normalizeUrl(`/harmonization-search#lists?type=variables&query=network(in(Mica_network.id,${id})),variable(in(Mica_variable.variableType,Dataschema)),study(${studyQuery})`);
        },
        hasStats: function(stats) {
          return stats.individualStudies + stats.studiesWithVariables + stats.studyVariables + stats.dataschemaVariables + stats.harmonizationStudies > 0;
        },
        getResultDtoField: function () {
          return 'networkResultDto';
        },
        setEntities: function(response) {
          const dto = response.networkResultDto;
          if (dto && dto['obiba.mica.NetworkResultDto.result'] && dto['obiba.mica.NetworkResultDto.result'].networks) {
            this.entities = dto['obiba.mica.NetworkResultDto.result'].networks;
            this.total = dto.totalHits;
            this.ensureEvenEntities();
          }
        }
      }
    });
  }
}
