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
  return typeof value === "string" ? value : key;
});

Vue.filter("localize-string", (input) => {
  if (typeof input === "string") return input;
  return StringLocalizer.localize(input);
});

// temporary, until overritten by rest call
Vue.filter("taxonomy-title", (input) => {
  return input;
});

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

/**
 * Taxonomy sidebar menu component
*/
Vue.component('search-criteria', {
  template: `
  <div class="">
    <ul v-for="name in criteriaMenu.order"
        class="nav nav-pills nav-sidebar flex-column" data-widget="treeview"
        role="menu" data-accordion="false">
      <li class="nav-item has-treeview menu-open">
        <a href="#" class="nav-link">
          <i class="nav-icon" v-bind:class="criteriaMenu.items[name].icon"></i>
          <p>
            {{criteriaMenu.items[name].title}}
          </p>
        </a>
        <ul class="nav nav-treeview">
          <li class="nav-item" :key="menu.name" v-for="menu in criteriaMenu.items[name].menus">
            <a href="#" class="nav-link" data-toggle="modal" data-target="#taxonomy-modal"
              :title="menu.description | localize-string"
              @click.prevent="onTaxonomySelection(menu.name, name)"><i class="far fa-circle nav-icon"></i><p>{{ menu.title | localize-string }}</p>
            </a>
          </li>
        </ul>
      </li>
    </ul>
  </div>
  `,
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
      // sort and include configured targets
      const visibleMenus = Mica.display.searchCriteriaMenus || [];
      const filteredTargets = payload.filter(p => visibleMenus.indexOf(p.name) > -1);
      filteredTargets.sort((a, b) => {
        const ai = visibleMenus.indexOf(a.name);
        const bi = visibleMenus.indexOf(b.name);
        return ai - bi;
      });

      for (let target of filteredTargets) {
        this.criteriaMenu.items[target.name].title = StringLocalizer.localize(target.title);
        switch (target.name) {
          case 'variable':
            let level = target.terms[0].terms;
            const theRest = target.terms.slice(1);

            if (theRest.length > 0) {
              this.criteriaMenu.items.variable.menus = level.concat(theRest);
            } else {
              this.criteriaMenu.items.variable.menus = level;
            }
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
      EventBus.$emit('taxonomy-selection', {target, taxonomyName: payload});
    }
  },
  mounted() {
    EventBus.register('mica-taxonomy', this.onMicaTaxonomies);
  }
});

/**
 * Component used to filter results by Study className vocabulary
 */
Vue.component('study-filter-shortcut', {
  name: 'StudyFilterShortcut',
  template: `
  <div v-if="visible && showFilter">
    <div class="btn-group" role="group" aria-label="Basic example">
      <button type="button" v-bind:class="{active: selection.all}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('all')">{{tr('all')}}</button>
      <button type="button" v-bind:class="{active: selection.study}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('study')">{{tr('individual')}}</button>
      <button type="button" v-bind:class="{active: selection.harmonization}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('harmonization')">{{tr('harmonization')}}</button>
    </div>
  </div>
  `,
  data() {
    return {
      selection: {all: true, study: false, harmonization: false},
      visible: true
    }
  },
  computed: {
    showFilter: () => Mica.config.isCollectedDatasetEnabled
      && Mica.config.isHarmonizedDatasetEnabled
      && !Mica.config.isSingleStudyEnabled
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
    onLocationChanged(payload) {
      this.selection = MicaTreeQueryUrl.getStudyTypeSelection(payload.tree);
      this.visible = DISPLAYS.GRAPHICS !== payload.display;
    },
    onSelectionClicked(selectionKey) {
      const classNameQuery = new RQL.Query('in', ['Mica_study.className', this.buildClassNameArgs(selectionKey)]);
      EventBus.$emit(EVENTS.QUERY_TYPE_UPDATE, {target: 'study', query: classNameQuery});
      EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
    }
  },
  mounted() {
    EventBus.register(EVENTS.LOCATION_CHANGED, this.onLocationChanged.bind(this));
  },
  beforeDestory() {
    EventBus.unregister(EVENTS.LOCATION_CHANGED, this.onLocationChanged);
  }
});

class TableFixedHeaderUtility {
  static rgbaToRgb(color) {
    var rgbaRegex = /^rgba?[\s+]?\([\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?,[\s+]?(\d+),?[\s+]?(\S+)\)$/i;
    var matches = color.match(rgbaRegex);

    if (matches && matches.length >= 4) {
      var r = parseInt(matches[1], 10);
      var g = parseInt(matches[2], 10);
      var b = parseInt(matches[3], 10);
      var a = parseFloat(matches[4] || "1", 10);

      var rPrime = (1 - a) * 255 + a * r;
      var gPrime = (1 - a) * 255 + a * g;
      var bPrime = (1 - a) * 255 + a * b;

      return 'rgb(' + rPrime + ', ' + gPrime + ', ' + bPrime + ')';
    }

    if (color === '') {
      return 'rgb(255, 255, 255)';
    }

    return color;
  }

  static getWindowScroll() {
    return {
      top: window.pageYOffset,
      left: window.pageXOffset,
    };
  }

  static getElementRectangle(element) {
    var rectangle = element.getBoundingClientRect();
    var windowScroll = TableFixedHeaderUtility.getWindowScroll();

    return {
      left: rectangle.left + windowScroll.left,
      top: rectangle.top + windowScroll.top,
      width: rectangle.width,
      height: rectangle.height,
    };
  }

  static applyTo(tableElement, offset) {
    const PAGE_LENGTH = 5;

    const thead = tableElement.querySelector('thead');
    const initialTheadBackgroundColor = thead.style.backgroundColor;
    const opaqueTheadBackground = TableFixedHeaderUtility.rgbaToRgb(initialTheadBackgroundColor);

    let theadTop = null;

    const allRows = tableElement.querySelectorAll('tbody tr');
    const thresholdRows = tableElement.querySelectorAll('tbody tr:nth-child(' + PAGE_LENGTH + 'n)');
    const numberOfPages = Math.ceil(allRows.length / PAGE_LENGTH);
    let currentPage = 0;

    function onScroll() {
      theadTop = theadTop || TableFixedHeaderUtility.getElementRectangle(thead).top;
      const itemTop = theadTop + (offset || 0);

      if (window.scrollY > itemTop) {
        thead.style.transform = 'translateY(' + Math.max(0, window.scrollY + (offset || 0) - theadTop) + 'px)';
        thead.style.backgroundColor = opaqueTheadBackground;
      } else {
        thead.style.transform = 'translateY(0)';
        thead.style.backgroundColor = initialTheadBackgroundColor;
      }

      let next = thresholdRows[currentPage];

      if (next && window.scrollY > TableFixedHeaderUtility.getElementRectangle(next).top) {
        currentPage++;
        Array.prototype.slice.call(allRows, ((currentPage * PAGE_LENGTH) || PAGE_LENGTH) + 1).forEach(row => row.style.display = 'table-row');
      }
    }

    function onDestroy() {
      window.removeEventListener("scroll", onScroll);
    }

    window.addEventListener("scroll", onScroll);

    Array.prototype.slice.call(allRows, ((currentPage * PAGE_LENGTH) || PAGE_LENGTH) + 1).forEach(row => row.style.display = 'none');

    return onDestroy;
  }
}

(function () {
  class ChartTableTermSorters {
    initialize(taxonomy) {
      this.taxonomy = taxonomy;
    }

    __findVocabulary(target) {
      return this.taxonomy.vocabularies.filter(vocabulary => vocabulary.name === target).pop();
    }

    sort(vocabulary, rows) {
      if (['methods-design', 'populations-dataCollectionEvents-bioSamples'].includes(vocabulary) && (rows || []).length > 0) {
        const found = this.__findVocabulary(vocabulary);
        if (found) {
          console.debug('FOUND', vocabulary)
          const terms = found.terms.map(term => term.name);
          rows.sort((a, b) => {
            return terms.indexOf(a.key) - terms.indexOf(b.key);
          })
        }
      }

      return rows;
    }
  }

  const chartTableTermSorters = new ChartTableTermSorters();

  function genericParseForChart(chartData, vocabulary) {
    let labels = [];
    let data = [];

    if (vocabulary) {
      chartTableTermSorters.sort(vocabulary, chartData);
    }

    chartData.forEach(term => {
      labels.push(term.title);
      data.push(term.count);
    });

    return [labels, { data: data }];
  }

  function genericParseForTable(vocabulary, chartData, forSubAggData) {
    return chartTableTermSorters.sort(vocabulary, chartData).map(term => {
      let row = {
        vocabulary: vocabulary.replace(/model-/, ""),
        key: term.key,
        title: term.title,
        count: term.count
      };

      if (forSubAggData) {
        const subAgg = term.aggs.filter((agg) => agg.aggregation === forSubAggData.agg)[0];
        row.subAgg = (subAgg[forSubAggData.dataKey] || {data: {}}).data[forSubAggData.data] || 0;
      }

      return row;
    });
  }

  const DataTableDefaults = {
    searching: false,
    ordering: false,
    lengthMenu: [10, 20, 50, 100],
    pageLength: 20,
    dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'table-responsive col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>"
  };

  const chartOptions = {
    'geographical-distribution-chart': {

      id: 'geographical-distribution-chart',
      title: Mica.tr['geographical-distribution-chart-title'],
      text: Mica.tr['geographical-distribution-chart-text'],
      type: 'choropleth',
      borderColor: Mica.charts.borderColor,
      agg: 'populations-model-selectionCriteria-countriesIso',
      vocabulary: 'populations-selectionCriteria-countriesIso',
      dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
      parseForChart: function(chartData) {
        let labels = [];
        let data = [];

        let states;
        let featureFinder = function(key) {
          return states.filter(state => state.id === key).pop();
        };
        if (['world'].includes(Mica.map.name)) {
          states = ChartGeo.topojson.feature(Mica.map.topo, Mica.map.topo.objects.countries1).features;
        } else {
          states = ChartGeo.topojson.feature(Mica.map.topo, Mica.map.topo.objects.collection).features;
        }
        chartData.filter(term => term.count>0).forEach(term => {
          labels.push(term.title);
          data.push({
            feature: featureFinder(term.key),
            value: term.count
          });
        });

        return [labels, {
          outline: states,
          data: data
        }];
      },
      parseForTable: genericParseForTable,
      options: {
        showOutline: true,
          showGraticule: false,
          legend: {
          display: false
        },
        scale: {
          projection: 'mercator'//'equalEarth'//'naturalEarth1'
        },
        geo: {
          colorScale: {
            display: true,
          },
        }
      }
    },
    'study-design-chart': {
      id: 'study-design-chart',
      title: Mica.tr['study-design-chart-title'],
      text: Mica.tr['study-design-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'model-methods-design',
      vocabulary: 'methods-design',
      dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
    },
    'number-participants-chart': {
      id: 'number-participants-chart',
      title: Mica.tr['number-participants-chart-title'],
      text: Mica.tr['number-participants-chart-text'],
      type: 'doughnut',
      backgroundColor: Mica.charts.backgroundColors,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'model-numberOfParticipants-participant-number-range',
      vocabulary: 'numberOfParticipants-participant-range',
      dataKey: 'obiba.mica.RangeAggregationResultDto.ranges',
      legend: {
      display: true,
        position: 'right',
        align: 'start',
      }
    },
    'bio-samples-chart': {
      id: 'bio-samples-chart',
      title: Mica.tr['bio-samples-chart-title'],
      text: Mica.tr['bio-samples-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'populations-dataCollectionEvents-model-bioSamples',
      vocabulary: 'populations-dataCollectionEvents-bioSamples',
      dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
    },
    'study-start-year-chart': {
      id: 'study-start-year-chart',
      title: Mica.tr['study-start-year-chart-title'],
      text: Mica.tr['study-start-year-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'model-startYear-range',
      vocabulary: 'start-range',
      dataKey: 'obiba.mica.RangeAggregationResultDto.ranges',
    }
  };

  class TaxonomyTitleFinder {
    initialize(taxonomies) {
      this.taxonomies = taxonomies;
    }

    title(taxonomyName, vocabularyName, termName) {
      if (taxonomyName) {
        const taxonomy = this.taxonomies[taxonomyName];
        if (taxonomy) {
          if (!vocabularyName && !termName) return StringLocalizer.localize(taxonomy.title);
          else if (vocabularyName) {
            let foundVocabulary = (taxonomy.vocabularies || []).filter(vocabulary => vocabulary.name === vocabularyName)[0];

            if (foundVocabulary) {
              if (!termName) return StringLocalizer.localize(foundVocabulary.title);
              else {
                let foundTerm = (foundVocabulary.terms || []).filter(term => term.name === termName)[0];

                if (foundTerm) return StringLocalizer.localize(foundTerm.title);
              }
            }
          }
        }
      }

      return null;
    }
  }

  const taxonomyTitleFinder  = new TaxonomyTitleFinder();

  class MicaQueryAlertListener {
    constructor() {
      EventBus.register(EVENTS.QUERY_ALERT, this.__onQueryAlery.bind(this));
    }

    __getTaxonomyVocabularyNames(query) {
      const parts = (query.name === 'match' ? query.args[1] : query.args[0]).split(/\./);
      return parts.length === 2 ? {taxonomy: parts[0], vocabulary: parts[1]} : {};
    }

    __onQueryAlery(payload) {
      const target = Mica.tr[payload.target];
      const query = payload.query || {};
      const taxonomyInfo = ["and", "or"].indexOf(query.name) === -1 ? this.__getTaxonomyVocabularyNames(query) : undefined;
      const message = Mica.trArgs(
        `criterion.${payload.action}`,
        [taxonomyInfo ? taxonomyTitleFinder.title(taxonomyInfo.taxonomy, taxonomyInfo.vocabulary) : "", target]
      );

      if (message) {
        MicaService.toastSuccess(message);
      }
    }
  }

  /**
    * Registering plugins defined in VueMicaSearch
    */
  Vue.use(VueMicaSearch, {
    mixin: {
      methods: {
        getEventBus: () => EventBus,
        getMicaConfig: () => Mica.config,
        getLocale: () => Mica.locale,
        getDisplayOptions: () => Mica.display,
        normalizePath: (path) => {
          return contextPath + path;
        },
        localize: (entries) => StringLocalizer.localize(entries),
        registerDataTable: (tableId, options) => {
          const mergedOptions = Object.assign(options, DataTableDefaults);
          mergedOptions.language = {
            url: contextPath + '/assets/i18n/datatables.' + Mica.locale + '.json'
          };
          const dTable = $('#' + tableId).DataTable(mergedOptions);
          dTable.on('draw', function() {
            // bs tooltip
            $('[data-toggle="tooltip"]').tooltip();
          });

          // checkboxes only for variables
          if ('vosr-variables-result' === tableId) {
            initSelectDataTable(dTable, options);
          }

          return dTable;
        }
      }
    }
  });

  const queryAlertListener  = new MicaQueryAlertListener();

  new Vue({
    el: '#search-application',
    data() {
      return {
        taxonomies: {},
        targets: [],
        display: DISPLAYS.LISTS,
        message: '',
        selectedTaxonomy: null,
        selectedTaxonomyTitle: null,
        selectedTarget: null,
        queryType: 'variables-list',
        lastList: '',
        queryExecutor: new MicaQueryExecutor(EventBus, DataTableDefaults.pageLength, Mica.querySettings),
        queries: null,
        noQueries: true,
        queryToCopy: null,
        queryToCart: null,
        newVariableSetName: '',
        variableSets: [],
        advanceQueryMode: false,
        downloadUrlObject: '',
        variableSelections: [],
        counts: {
          variables: "0",
          datasets: "0",
          studies: "0",
          networks: "0",
        },
        hasVariableQuery: false,
        hasGraphicsResult: false,
        selectedBucket: BUCKETS.study,
        dceChecked: false,
        bucketTitles: {
          study: Mica.tr.study,
          dataset: Mica.tr.dataset,
          dce: Mica.tr['data-collection-event'],
        },
        chartOptions: Mica.charts.chartIds.map(id => chartOptions[id]),
        canDoFullCoverage: false,
        hasCoverageTermsWithZeroHits: false,
        queryForFullCoverage: null,
        queriesWithZeroHitsToRemove: [],
        coverageFixedHeaderHandler: null,
        loading: false
      };
    },
    methods: {
      updateStudyTypeFilter() {
        setTimeout(() => {
          const filter = document.querySelector('#study-filter-shortcut');
          const tabPane = document.querySelector(".tab-pane .show");
          if (tabPane) {
            const toolbar = tabPane.querySelector('div.toolbar')
            if (toolbar) {
              filter.parentNode.removeChild(filter);
              toolbar.prepend(filter);
            }
          } else {
            filter.parentNode.removeChild(filter);
            document.querySelector('#study-filter-shortcut-container').prepend(filter);
          }
        }, 150);
      },
      refreshQueries() {
        this.queries = MicaTreeQueryUrl.getTreeQueries();
        this.noQueries = true;
        if (this.queries) {
          for (let key of [TARGETS.VARIABLE, TARGETS.DATASET, TARGETS.STUDY, TARGETS.NETWORK]) {
            let target = this.queries[key];
            if (target && target.args && target.args.length > 0) {
              this.noQueries = false;
              break;
            }
          }
        }
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
      onTaxonomySelection(payload) {
        this.selectedTaxonomy = this.taxonomies[payload.taxonomyName];
        this.selectedTarget = payload.target;

        let selectedTaxonomyVocabulariesTitle = '';
        if (this.selectedTaxonomy) {
          this.selectedTaxonomyTitle = this.selectedTaxonomy.title;
          selectedTaxonomyVocabulariesTitle = this.selectedTaxonomy.vocabularies.map(voc => voc.title[0].text).join(', ');
        } else {
          const foundTaxonomyGroup = this.findTaxonomyGroup(payload.taxonomyName, payload.target);
          this.selectedTaxonomy = foundTaxonomyGroup.taxonomies;
          this.selectedTaxonomyTitle = foundTaxonomyGroup.title;
        }

        this.message = '[' + payload.taxonomyName + '] ' + this.selectedTaxonomyTitle[0].text + ': ';
        this.message = this.message + selectedTaxonomyVocabulariesTitle;
      },
      findTaxonomyGroup(taxonomyName, target) {
        let found = {};

        const foundTarget = this.targets.filter(it => it.name === target)[0];
        let foundTaxonomyGroup = foundTarget.terms.filter(it => it.name === taxonomyName)[0];

        if (foundTaxonomyGroup) {
          found.title = foundTaxonomyGroup.title;
          let taxonomies = [];
          foundTaxonomyGroup.terms.forEach(term => {
            const taxonomy = this.taxonomies[term.name];
            if (taxonomy) {
              taxonomies.push(taxonomy);
            }
          });

          found.taxonomies = taxonomies;
        }

        return found;
      },
      onExecuteQuery() {
        console.debug('Executing ' + this.queryType + ' query ...');
        EventBus.$emit(this.queryType, 'I am the result of a ' + this.queryType + ' query');
        this.loading = true;
      },
      onClearQuery() {
        const urlParts = MicaTreeQueryUrl.parseUrl();
        const searchParams = urlParts.searchParams || {};

        const display = urlParts.hash || 'list';
        const type = searchParams.type || TYPES.VARIABLES;

        let params = [`type=${type}`];

        const urlSearch = params.join("&");
        const hash = `${display}?${urlSearch}`;

        window.location.hash = `#${hash}`;
      },
      onLocationChanged(payload) {
        this.downloadUrlObject = MicaTreeQueryUrl.getDownloadUrl(payload);

        let tree = MicaTreeQueryUrl.getTree();

        const target = TYPES_TARGETS_MAP[payload.type];
        if (target) {
          const limitQuery = tree.search((name, args, parent) => 'limit' === name && parent.name === target);
          if (limitQuery) {
            const size = limitQuery.args[1];
            const table = $(`table[id=vosr-${payload.type}-result]`).DataTable();
            if (table) {
              table.page.len(size); // do not use draw() otherwise the query gets executed forever!
            }
          }
        }

        // query string to copy
        tree.findAndDeleteQuery((name) => 'limit' === name);
        this.queryToCopy = tree.serialize();

        // query string for adding variables to cart
        let vQuery = tree.search((name) => name === TARGETS.VARIABLE);
        if (!vQuery) {
          vQuery = new RQL.Query(TARGETS.VARIABLE,[]);
          tree.addQuery(null, vQuery);
        }
        const limitQuery = tree.search((name, args, parent) => 'limit' === name && parent.name === TARGETS.VARIABLE);
        if (limitQuery) {
          limitQuery.args = [0, 100000];
        } else {
          tree.addQuery(vQuery, new RQL.Query('limit', [0, 100000]));
        }
        tree.addQuery(vQuery, new RQL.Query('fields', ['variableType']));
        this.queryToCart = tree.serialize();

        this.refreshQueries();

        // result
        $(`.nav-tabs #${payload.display}-tab`).tab('show');
        $(`.nav-pills #${payload.type}-tab`).tab('show');

        if (payload.bucket) {
          this.selectedBucket = TARGET_ID_BUCKET_MAP[payload.bucket];
          const tabPill = [TARGET_ID_BUCKET_MAP.studyId, TARGET_ID_BUCKET_MAP.dceId].indexOf(this.selectedBucket) > -1
            ? TARGET_ID_BUCKET_MAP.studyId
            : TARGET_ID_BUCKET_MAP.datasetId;
          this.dceChecked = TARGET_ID_BUCKET_MAP.dceId === this.selectedBucket;
          $(`.nav-pills #bucket-${tabPill}-tab`).tab('show');
        }

        const targetQueries = MicaTreeQueryUrl.getTreeQueries();
        this.hasVariableQuery = TARGETS.VARIABLE in targetQueries && targetQueries[TARGETS.VARIABLE].args.length > 0;
      },
      onQueryUpdate(payload) {
        console.debug('query-builder update', payload);
        EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [payload]});

        EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
      },
      onQueryRemove(payload) {
        console.debug('query-builder update', payload);
        EventBus.$emit(EVENTS.QUERY_TYPE_DELETE, payload);

        EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
      },
      onNodeUpdate(payload) {
        console.debug('query-builder node update', payload);
        EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [payload]});

        EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
      },
      onCopyQuery() {
        navigator.clipboard.writeText(this.queryToCopy);
      },
      onAddToCart() {
        const onsuccess = function(cart, oldCart) {
          VariablesSetService.showCount('#cart-count', cart, Mica.locale);
          if (cart.count === oldCart.count) {
            MicaService.toastInfo(Mica.tr['no-variable-added']);
          } else {
            MicaService.toastSuccess(Mica.tr['variables-added-to-cart'].replace('{0}', (cart.count - oldCart.count).toLocaleString(Mica.locale)));
          }
        };

        if (Array.isArray(this.variableSelections) && this.variableSelections.length > 0) {
          VariablesSetService.addToCart(this.variableSelections, onsuccess);
        } else {
          VariablesSetService.addQueryToCart(this.queryToCart, onsuccess);
        }
      },
      onAddToSet(setId) {
        const onsuccess = (set, oldSet) => {
          if (set.count === oldSet.count) {
            MicaService.toastInfo(Mica.tr['no-variable-added-set'].replace('{{arg0}}', '"' + set.name + '"'));
          } else {
            MicaService.toastSuccess(Mica.tr['variables-added-to-set'].replace('{0}', (set.count - oldSet.count).toLocaleString(Mica.locale)).replace('{1}', '"' + set.name + '"'));
          }

          this.newVariableSetName = '';
          VariablesSetService.showSetsCount($('#list-count'), sets => {
            this.variableSets = sets;
          });
        };

        if (setId || (this.newVariableSetName && this.newVariableSetName.length > 0)) {
          if (Array.isArray(this.variableSelections) && this.variableSelections.length > 0) {
            VariablesSetService.addToSet(setId, this.newVariableSetName, this.variableSelections, onsuccess);
          } else {
            VariablesSetService.addQueryToSet(setId, this.newVariableSetName, this.queryToCart, onsuccess);
          }
        }
      },
      onDownloadQueryResult() {
        if (this.downloadUrlObject) {
          const form = document.createElement('form');
          form.setAttribute('class', 'hidden');
          form.setAttribute('method', 'post');

          form.action = this.downloadUrlObject.url;
          form.accept = 'text/csv';

          const input = document.createElement('input');
          input.name = 'query';

          if (Array.isArray(this.variableSelections) && this.variableSelections.length > 0) {
            const queryAsTree = new RQL.QueryTree(RQL.Parser.parseQuery(this.downloadUrlObject.query));
            let variableQuery = queryAsTree.search((name) => name === "variable");
            queryAsTree.addQuery(variableQuery, new RQL.Query('in', ['id', this.variableSelections]));

            input.value = queryAsTree.serialize();
          } else {
            input.value = this.downloadUrlObject.query;
          }

          form.appendChild(input);

          document.body.appendChild(form);
          form.submit();
          form.remove();
        } else {
          MicaService.toastError(Mica.tr['no-coverage-available']);
        }
      },
      onSearchModeToggle() {
        this.advanceQueryMode = !this.advanceQueryMode;
      },
      onSelectBucket(bucket) {
        console.debug(`onSelectBucket : ${bucket} - ${this.dceChecked}`);
        this.selectedBucket = bucket;
        EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {bucket});

        this.loading = true;
      },
      onResult(payload) {
        this.display = DISPLAYS.LISTS;
        const data = payload.response;
        this.counts = {
          variables: "0",
          datasets: "0",
          studies: "0",
          networks: "0",
        };

        if (data && data.variableResultDto && data.variableResultDto.totalHits) {
          this.counts.variables = data.variableResultDto.totalHits.toLocaleString();
        }

        if (data && data.datasetResultDto && data.datasetResultDto.totalHits) {
          this.counts.datasets = data.datasetResultDto.totalHits.toLocaleString();
        }

        if (data && data.studyResultDto && data.studyResultDto.totalHits) {
          this.counts.studies = data.studyResultDto.totalHits.toLocaleString();
        }

        if (data && data.networkResultDto && data.networkResultDto.totalHits) {
          this.counts.networks = data.networkResultDto.totalHits.toLocaleString();
        }

        this.loading = false;

        this.updateStudyTypeFilter();
      },
      onSelectResult(type, target) {
        this.display = DISPLAYS.LISTS;
        EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {display: DISPLAYS.LISTS, type, target});
      },
      onSelectSearch() {
        this.display = DISPLAYS.LISTS;
        EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {display: DISPLAYS.LISTS});
      },
      onSelectCoverage() {
        this.display = DISPLAYS.COVERAGE;
        EventBus.$emit(EVENTS.QUERY_TYPE_COVERAGE, {display: DISPLAYS.COVERAGE});
      },
      onSelectGraphics() {
        this.display = DISPLAYS.GRAPHICS;
        EventBus.$emit(EVENTS.QUERY_TYPE_GRAPHICS, {type: TYPES.STUDIES, display: DISPLAYS.GRAPHICS});
      },
      onGraphicsResult(payload) {
        this.display = DISPLAYS.GRAPHICS;
        this.hasGraphicsResult = payload.response.studyResultDto.totalHits > 0;
        this.loading = false;
      },
      onCoverageResult(payload) {
        this.display = DISPLAYS.COVERAGE;
        if (payload.response.rows !== undefined) { // for filters
          let rowsEligibleForFullCoverage = [];
          payload.response.rows.forEach(row => {
            if (Array.isArray(row.hits)) {
              if (row.hits.filter(hit => hit === 0).length === 0) {
                rowsEligibleForFullCoverage.push(row);
              }
            }
          });

          // filter for full coverage
          let coverageVocabulary = this.selectedBucket.startsWith('dce') ? 'dceId' : 'id';

          let coverageArgs = ['Mica_' + fromBucketToTarget(this.selectedBucket) + '.' + coverageVocabulary];
          coverageArgs.push(rowsEligibleForFullCoverage.map(selection => selection.value));

          const numberOfTerms = payload.response.termHeaders.length;

          this.canDoFullCoverage = rowsEligibleForFullCoverage.length > 0 && rowsEligibleForFullCoverage.length < payload.response.rows.length; // active?

          if (this.canDoFullCoverage) {
            this.queryForFullCoverage = new RQL.Query('in', coverageArgs);
          }

          // filter for subdomains with variables
          const taxonomyNames = Array(numberOfTerms), vocabularyNames = Array(numberOfTerms);
          let lastTaxonomyHeaderIndex = 0, lastVocabularyHeaderIndex = 0;
          payload.response.taxonomyHeaders.forEach(taxonomyHeader => {
            const name = taxonomyHeader.entity.name, termsCount = taxonomyHeader.termsCount;

            taxonomyNames.fill(name, lastTaxonomyHeaderIndex, lastTaxonomyHeaderIndex + termsCount);
            lastTaxonomyHeaderIndex += termsCount;
          });

          payload.response.vocabularyHeaders.forEach(vocabularyHeader => {
            const name = vocabularyHeader.entity.name, termsCount = vocabularyHeader.termsCount;

            vocabularyNames.fill(name, lastVocabularyHeaderIndex, lastVocabularyHeaderIndex + termsCount);
            lastVocabularyHeaderIndex += termsCount;
          });

          this.queriesWithZeroHitsToRemove = [];
          payload.response.termHeaders.forEach((termHeader, index) => {
            const key = taxonomyNames[index] + '.' + vocabularyNames[index], name = termHeader.entity.name;

            if (termHeader.hits === 0) {
              this.queriesWithZeroHitsToRemove.push(new RQL.Query('in', [key, [name]]));
            }
          });

          this.hasCoverageTermsWithZeroHits = this.queriesWithZeroHitsToRemove.length > 0; // active?
        }

        this.loading = false;
      },
      onZeroColumnsToggle() {
        this.queriesWithZeroHitsToRemove.forEach(query => {
          EventBus.$emit(EVENTS.QUERY_TYPE_DELETE, {target: TARGETS.VARIABLE, query, display: DISPLAYS.COVERAGE});
        });
      },
      onFullCoverage() {
        EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [{target: fromBucketToTarget(this.selectedBucket), query: this.queryForFullCoverage, display: DISPLAYS.COVERAGE}]});
      }
    },
    computed: {
      selectedQuery() {
        if (this.selectedTarget) {
          return this.queries[this.selectedTarget];
        }

        return undefined;
      },
      numberOfSetsRemaining() {
        return Mica.maxNumberOfSets - (this.variableSets || []).length;
      }
    },
    beforeMount() {
      console.debug('Before mounted QueryBuilder');
      this.queryExecutor.init();

      EventBus.register("variables-results", this.onResult.bind(this));
      EventBus.register("datasets-results", this.onResult.bind(this));
      EventBus.register("studies-results", this.onResult.bind(this));
      EventBus.register("networks-results", this.onResult.bind(this));
      EventBus.register('coverage-results', this.onCoverageResult.bind(this));
      EventBus.register(EVENTS.QUERY_TYPE_GRAPHICS_RESULTS, this.onGraphicsResult.bind(this));
    },
    mounted() {
      console.debug('Mounted QueryBuilder');
      EventBus.register('taxonomy-selection', this.onTaxonomySelection);
      EventBus.register(EVENTS.LOCATION_CHANGED, this.onLocationChanged.bind(this));

      EventBus.register(EVENTS.CLEAR_RESULTS_SELECTIONS, () => this.variableSelections = []);

      for (const typeKey in TYPES) {
        EventBus.register(`${TYPES[typeKey]}-selections-updated`, payload => this.variableSelections = payload.selections || []);
      }

      // fetch the configured search criteria, in the form of a taxonomy of taxonomies
      axios
        .get(contextPath + '/ws/taxonomy/Mica_taxonomy/_filter?target=taxonomy')
        .then(response => {
          this.targets = response.data.vocabularies;
          EventBus.$emit('mica-taxonomy', this.targets);

          const targetQueries = [];

          for (let target of this.targets) {
            // then load the taxonomies
            targetQueries.push(`${contextPath}/ws/taxonomies/_filter?target=${target.name}`);
          }

          return axios.all(targetQueries.map(query => axios.get(query))).then(axios.spread((...responses) => {
            responses.forEach((response) => {
              for (let taxo of response.data) {
                this.taxonomies[taxo.name] = taxo;
              }
            });

            this.refreshQueries();

            taxonomyTitleFinder.initialize(this.taxonomies);
            chartTableTermSorters.initialize(this.taxonomies['Mica_study']);

            Vue.filter("taxonomy-title", (input) => {
              const [taxonomy, vocabulary, term] = input.split(/\./);
              return  taxonomyTitleFinder.title(taxonomy, vocabulary, term) || input;
            });

            // Emit 'query-type-selection' to pickup a URL query to be executed; if nothing found a Variable query is executed
            EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {});

            return this.taxonomies;
          }));
        });

      const targetQueries = MicaTreeQueryUrl.getTreeQueries();

      let advancedNodeCount = 0;
      for (const target in targetQueries) {
        let advancedOperator = target === TARGETS.VARIABLE ? 'and' : 'or';
        const targetQuery = targetQueries[target];
        if (targetQuery) {
          new RQL.QueryTree(targetQuery).visit(query => {
            if (query.name === advancedOperator) {
              advancedNodeCount++;
            }
          });
        }
      }
      this.advanceQueryMode = advancedNodeCount > 0;

      // don't close sets' dropdown when clicking inside of it
      if (this.$refs.listsDropdownMenu) {
        this.$refs.listsDropdownMenu.addEventListener("click", event => event.stopPropagation());
      }

      VariablesSetService.getSets(data => {
        if (Array.isArray(data)) {
          this.variableSets = data.filter(set => set.name);
        }});
      this.onExecuteQuery();
    },
    updated() {
      $('.tab-content .card').removeClass('card-primary').addClass('card-success');

      let coverageResultTableElement = document.querySelector('#vosr-coverage-result');

      if (this.coverageFixedHeaderHandler) {
        this.coverageFixedHeaderHandler();
        this.coverageFixedHeaderHandler = null;
      }

      if (coverageResultTableElement) {
        this.coverageFixedHeaderHandler = TableFixedHeaderUtility.applyTo(coverageResultTableElement, $("#menubar").outerHeight() + $("#loginbar").outerHeight());
      }
    },
    beforeDestory() {
      console.debug('Before destroy query builder');
      EventBus.unregister(EVENTS.LOCATION_CHANGED, this.onLocationChanged);
      EventBus.unregister('taxonomy-selection', this.onTaxonomySelection);
      EventBus.unregister(EVENTS.QUERY_TYPE_SELECTION, this.onQueryTypeSelection);
      this.queryExecutor.destroy();

      EventBus.unregister("variables-results", this.onResult);
      EventBus.unregister("datasets-results", this.onResult);
      EventBus.unregister("studies-results", this.onResult);
      EventBus.unregister("networks-results", this.onResult);
      EventBus.unregister("coverage-results", this.onCoverageResult);
      EventBus.unregister(EVENTS.QUERY_TYPE_GRAPHICS_RESULTS, this.onGraphicsResult);
    }
  });

})();
