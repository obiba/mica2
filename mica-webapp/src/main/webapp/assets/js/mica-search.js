'use strict';

// an EventBus is a Vue app without element
// its data are callback functions, registered by event name
const EventBus = new Vue({
  data: {
    callbacks: {}
  },
  methods: {
    register: function (eventName, callback) {
      if (!this.callbacks[eventName]) {
        this.callbacks[eventName] = [];
        this.$on(eventName, function (payload) {
          for (let callback of this.callbacks[eventName]) {
            callback(payload);
          }
        });
      }
      this.callbacks[eventName].push(callback);
      //console.dir(this.callbacks)
    },
    unregister: function (eventName) {
      this.callbacks[eventName] = undefined;
    }
  }
});

// global translate filter for use in imported components
Vue.filter("translate", (key) => {
  let value = Mica.tr[key];
  return value !== undefined || value !== null ? value : key;
});

const DataTableDefaults = {
  searching: false,
  ordering: false,
  lengthMenu: [10, 20, 50, 100],
  pageLength: 20,
  dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>"
};

//
// Search criteria Vue
// sidebar menus for taxonomy selection. Terms selection is delegated to the main app (query builder)
//

/**
 * Taxonomy sidebar menu component
 */
Vue.component('taxonomy-menu', {
  props: ['taxonomy'],
  template: `
  <li class="nav-item">
      <a href="#" class="nav-link" data-toggle="modal" data-target="#taxonomy-modal" :title="taxonomy.description[0].text" @click.prevent="$emit('taxonomy-selection', taxonomy.name)"><i class="far fa-circle nav-icon"></i><p>{{ taxonomy.title[0].text }}</p></a>
  </li>
`
});

new Vue({
  el: '#search-criteria',
  data() {
    return {
      criteriaMenu: {
        items: {
          variable: {
            icon: Mica.icons.variable,
            title: Mica.tr.variables,
            menus: []
          },
          dataset: {
            icon: Mica.icons.dataset,
            title: Mica.tr.datasets,
            menus: []
          },
          study: {
            icon: Mica.icons.study,
            title: Mica.tr.studies,
            menus: []
          },
          network: {
            icon: Mica.icons.network,
            title: Mica.tr.networks,
            menus: []
          },
        },
        order: []
      }
    };
  },
  methods: {
    // make the menu
    onMicaTaxonomies: function (payload) {
      for (let target of payload) {
        this.criteriaMenu.items[target.name].title = target.title[0].text;
        switch (target.name) {
          case 'variable':
            // TODO handle multi level
            this.criteriaMenu.items.variable.menus = target.terms[0].terms;
            break;
          case 'dataset':
          case 'study':
          case 'network':
            this.criteriaMenu.items[target.name].menus = target.terms;
            break;
        }
        if (this.criteriaMenu.items[target.name].menus && this.criteriaMenu.items[target.name].menus.length > 0) {
          this.criteriaMenu.order.push(target.name);
        }
      }
    },
    // forward taxonomy selection
    onTaxonomySelection: function (payload, target) {
      console.dir(payload);
      EventBus.$emit('taxonomy-selection', {target, taxonomyName: payload});
    }
  },
  mounted() {
    EventBus.register('mica-taxonomy', this.onMicaTaxonomies);
  }
});

//
// Results Vue
// results display app, with some filtering criteria selection, and requests for query execution
//

/**
 * Component used to filter results by Study className vocabulary
 */
const StudyFilterShortcutComponent = Vue.component('study-filter-shortcut', {
  name: 'StudyFilterShortcut',
  template: `
  <div v-if="showFilter">
    <div class="btn-group" role="group" aria-label="Basic example">
      <button type="button" v-bind:class="{active: selection.all}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('all')">{{tr('all')}}</button>
      <button type="button" v-bind:class="{active: selection.study}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('study')">{{tr('individual')}}</button>
      <button type="button" v-bind:class="{active: selection.harmonization}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('harmonization')">{{tr('harmonization')}}</button>
    </div>
  </div>
  `,
  data() {
    return {
      selection: {all: true, study: false, harmonization: false}
    }
  },
  computed: {
    showFilter: () => Mica.config.isCollectedDatasetEnabled && Mica.config.isHarmonizedDatasetEnabled
  },
  methods: {
    tr(key) {
      return Mica.tr[key]
    },
    buildClassNameArgs(key) {
      switch (key) {
        case 'study':
          return 'Study';

        case 'harmonization':
          return 'HarmonizationStudy';
      }

      return ['Study', 'HarmonizationStudy'];
    },
    isAll(values) {
      return !values || Array.isArray(values) && values.length > 1;
    },
    isClassName(name, values) {
      return Array.isArray(values) ? values.length === 1 && values.indexOf(name) > -1 : values === name;
    },
    onLocationChanged(payload) {
      console.log('StudyFilterShortcut::onLocationChanged()');
      const tree = payload.tree;
      const classNameQuery = tree.search((name, args) => args.indexOf('Mica_study.className') > -1);
      if (classNameQuery) {
        const values = classNameQuery.args[1];
        this.selection.all = this.isAll(values);
        this.selection.study = this.isClassName('Study', values);
        this.selection.harmonization = this.isClassName('HarmonizationStudy', values);
      } else {
        this.selection = {all: true, study: false, harmonization: false};
      }
    },
    onSelectionClicked(selectionKey) {
      const classNameQuery = new RQL.Query('in', ['Mica_study.className', this.buildClassNameArgs(selectionKey)]);
      EventBus.$emit('query-type-update', {target: 'study', query: classNameQuery});
    }
  },
  mounted() {
    console.log('Mounted study-filter-shortcut');
    EventBus.register('location-changed', this.onLocationChanged.bind(this));
  },
  beforeDestory() {
    EventBus.unregister('location-changed', this.onLocationChanged);
  }
});

/**
 * Registering plugins defined in VueObibaSearchResult
 */
Vue.use(VueObibaSearchResult, {
  mixin: {
    methods: {
      getEventBus: () => EventBus,
      getMicaConfig: () => Mica.config,
      tr: (key) => {
        return Mica.tr[key] ? Mica.tr[key] : key;
      },
      registerDataTable: (tableId, options) => {
        const mergedOptions = Object.assign(options, DataTableDefaults);
        return $('#' + tableId).DataTable(mergedOptions);
      }
    }
  }
});

/**
 * Querybuilder Vue
 *
 * Main app that orchestrates the query display, criteria selection, query execution and dispatch of the results
 *
 */
new Vue({
  el: '#query-builder',
  data() {
    return {
      taxonomies: {},
      message: '',
      selectedTaxonomy: null,
      selectedTarget: null,
      selectedQuery: null,
      queryType: 'variables-list',
      lastList: '',
      queryExecutor: new MicaQueryExecutor(EventBus, DataTableDefaults.pageLength),
      queries: null
    };
  },
  methods: {
    refreshQueries() {
      this.queries = this.queryExecutor.getTreeQueries();
    },
    getTaxonomyForTarget(target) {
      let result = [];

      if (TARGETS.VARIABLE === target) {
        let taxonomies = [];
        for (let taxonomy in this.taxonomies) {
          if (taxonomy === `Mica_${target}` || !taxonomy.startsWith('Mica_')) {
            const found = this.taxonomies[taxonomy];
            if (found) taxonomies.push(found);
          }
        }

        result.push(taxonomies);
      } else {
        let taxonomy = this.taxonomies[`Mica_${target}`];
        result.push(taxonomy);
      }

      return result[0];
    },
    // show a modal with all the vocabularies/terms of the selected taxonomy
    // initialized by the query terms and update/trigger the query on close
    onTaxonomySelection: function (payload) {
      this.selectedTaxonomy = this.taxonomies[payload.taxonomyName];
      this.selectedTarget = payload.target;

      this.selectedQuery = this.queries[this.selectedTarget];
      this.message = '[' + payload.taxonomyName + '] ' + this.selectedTaxonomy.title[0].text + ': ';
      this.message = this.message + this.selectedTaxonomy.vocabularies.map(voc => voc.title[0].text).join(', ');
    },
    // set the type of query to be executed, on result component selection
    onQueryTypeSelection: function (payload) {
      // TODO need cleaning, may be use the QueryExecutor for taxos as well
      // if (payload.type === 'lists') {
      //   this.queryType = this.lastList;
      // } else {
      //   this.queryType = payload.type;
      //   if (this.queryType.endsWith('-list')) {
      //     this.lastList = payload.type;
      //   }
      // }
      // this.onExecuteQuery();
    },
    onExecuteQuery: function () {
      console.log('Executing ' + this.queryType + ' query ...');
      EventBus.$emit(this.queryType, 'I am the result of a ' + this.queryType + ' query');
    },
    onLocationChanged: function (payload) {
      $(`.nav-pills #${payload.display}-tab`).tab('show');
      $(`.nav-pills #${payload.type}-tab`).tab('show');
      this.refreshQueries();
    },
    onQueryUpdate(payload) {
      console.log('query-builder update', payload);
      EventBus.$emit(EVENTS.QUERY_TYPE_UPDATE, payload);
    },
    onQueryRemove(payload) {
      console.log('query-builder update', payload);
      EventBus.$emit(EVENTS.QUERY_TYPE_DELETE, payload);
    }
  },
  beforeMount() {
    this.queryExecutor.init();
    console.log('Before mounted QueryBuilder');
  },
  mounted() {
    console.log('Mounted QueryBuilder');
    EventBus.register('taxonomy-selection', this.onTaxonomySelection);
    EventBus.register('query-type-selection', this.onQueryTypeSelection);
    EventBus.register(EVENTS.LOCATION_CHANGED, this.onLocationChanged.bind(this));

    // Emit 'query-type-selection' to pickup a URL query to be executed; if nothing found a Variable query is executed
    EventBus.$emit('query-type-selection', {});

    // fetch the configured search criteria, in the form of a taxonomy of taxonomies
    axios
      .get('../ws/taxonomy/Mica_taxonomy/_filter?target=taxonomy')
      .then(response => {
        let targets = response.data.vocabularies;
        EventBus.$emit('mica-taxonomy', targets);

        const targetQueries = [];

        for (let target of targets) {
          // then load the taxonomies

          targetQueries.push(`../ws/taxonomies/_filter?target=${target.name}`);
        }

        return axios.all(targetQueries.map(query => axios.get(query))).then(axios.spread((...responses) => {
          responses.forEach((response) => {
            for (let taxo of response.data) {
              this.taxonomies[taxo.name] = taxo;
            }
          });

          this.refreshQueries();
          return this.taxonomies;
        }));
      });  
    this.onExecuteQuery();
  },
  beforeDestory() {
    console.log('Before destroy query builder');
    EventBus.unregister(EVENTS.LOCATION_CHANGED, this.onLocationChanged);
    EventBus.unregister('taxonomy-selection', this.onTaxonomySelection);
    EventBus.unregister('query-type-selection', this.onQueryTypeSelection);
    this.queryExecutor.destroy();
  }
});


/**
 * Component for all results related functionality
 */
new Vue({
  el: '#results-tab-content',
  components: {
    'study-filter-shortcut': StudyFilterShortcutComponent
  },
  data() {
    return {
      counts: {}
    }
  },
  methods: {
    onSelectResult(type, target) {
      EventBus.$emit('query-type-selection', {display: 'list', type, target});
    },
    onSelectSearch() {
      EventBus.$emit('query-type-selection', {display: DISPLAYS.LISTS});
    },
    onSelectCoverage() {
      EventBus.$emit('query-type-coverage', {display: DISPLAYS.COVERAGE});
    },
    onSelectGraphics() {
      EventBus.$emit('query-type-selection', {display: DISPLAYS.GRAPHICS});
    },
    onResult(payload) {
      const data = payload.response;
      this.counts = {
        variables: "0",
        datasets: "0",
        studies: "0",
        networks: "0",
      };

      if (data && data.variableResultDto && data.variableResultDto.totalHits) {
        // $('#variable-count').text(data.variableResultDto.totalHits.toLocaleString());
        this.counts.variables = data.variableResultDto.totalHits.toLocaleString();
      }

      if (data && data.datasetResultDto && data.datasetResultDto.totalHits) {
        // $('#dataset-count').text(data.datasetResultDto.totalHits.toLocaleString());
        this.counts.datasets = data.datasetResultDto.totalHits.toLocaleString();
      }

      if (data && data.studyResultDto && data.studyResultDto.totalHits) {
        // $('#study-count').text(data.studyResultDto.totalHits.toLocaleString());
        this.counts.studies = data.studyResultDto.totalHits.toLocaleString();
      }

      if (data && data.networkResultDto && data.networkResultDto.totalHits) {
        // $('#network-count').text(data.networkResultDto.totalHits.toLocaleString());
        this.counts.networks = data.networkResultDto.totalHits.toLocaleString();
      }
    }
  },
  beforeMount() {
    EventBus.register('variables-results', this.onResult);
    EventBus.register('datasets-results', this.onResult);
    EventBus.register('studies-results', this.onResult);
    EventBus.register('networks-results', this.onResult);
  },
  beforeDestory() {
    EventBus.unregister('variables-results', this.onResult);
    EventBus.unregister('datasets-results', this.onResult);
    EventBus.unregister('studies-results', this.onResult);
    EventBus.unregister('networks-results', this.onResult);
  }
});
