'use strict';

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

const MINIMUM_STUDY_TAXONOMY = {
  name: "Mica_study",
  vocabularies: [
    {
      name: "className",
      attributes: [{key: "forClassName", value: ""}],
      title: [ {locale: "en", text: "Type of Study"}, {locale: "fr", text: "Type d'étude"} ],
      terms: [
        { name: "Study", title: [ {locale: "en", text: "Individual"}, {locale: "fr", text: "Individuelle"} ] },
        { name: "HarmonizationStudy", title: [ {locale: "en", text: "Harmonization"}, {locale: "fr", text: "Harmonisation"} ] }
      ]
    }
  ]
};

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
           {{studyTypeSelection && studyTypeSelection.harmonization ? criteriaMenu.items[name].harmoTitle : criteriaMenu.items[name].title}}
          </p>
        </a>
        <ul class="nav nav-treeview">
          <li class="nav-item" :key="menu.name" v-for="menu in criteriaMenu.items[name].menus" v-if="!(studyTypeSelection && studyTypeSelection.harmonization) || !menu.hideHarmo">
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
  props: {
    studyTypeSelection: Object
  },
  data() {
    return {
      criteriaMenu: {
        items: {
          variable: {
            icon: Mica.icons.variable,
            title: Mica.tr.variables,
            harmoTitle: Mica.tr.variables,
            menus: []
          },
          dataset: {
            icon: Mica.icons.dataset,
            title: Mica.tr.datasets,
            harmoTitle: Mica.tr.protocols,
            menus: []
          },
          study: {
            icon: Mica.icons.study,
            title: Mica.tr.studies,
            harmoTitle: Mica.tr.initiatives,
            menus: []
          },
          network: {
            icon: Mica.icons.network,
            title: Mica.tr.networks,
            harmoTitle: Mica.tr.networks,
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

            this.criteriaMenu.items.variable.menus.forEach(m =>  {
              if (m.name === 'Mlstr_additional') {
                m.hideHarmo = true;
              }
            });
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
  <div>
    <template v-if="!alternate">
    <div v-if="visible && showFilter" class="d-inline-block">
      <div class="btn-group" role="group">
        <button type="button" v-bind:class="{active: selection.all}" class="btn btn-sm btn-light" v-on:click="onSelectionClicked('all')">{{tr('all')}}</button>
        <button type="button" v-bind:class="{active: selection.study}" class="btn btn-sm btn-light" v-on:click="onSelectionClicked('study')">{{tr('individual')}}</button>
        <button type="button" v-bind:class="{active: selection.harmonization}" class="btn btn-sm btn-light" v-on:click="onSelectionClicked('harmonization')">{{tr('harmonization')}}</button>
      </div>
    </div>
    </template>
    <template v-else>
      <ul class="nav nav-tabs h5">
      <li class="nav-item">
          <a v-if="selection.all" href="#" v-bind:class="{active: selection.all}" class="nav-link" @click.stop.prevent="onSelectionClicked('all')">{{tr('all')}}</a>
        </li>
        <li class="nav-item">
          <a href="#" v-bind:class="{active: selection.study}" class="nav-link" @click.stop.prevent="onSelectionClicked('study')">{{tr('individual')}}</a>
        </li>
        <li class="nav-item" v-if="showHarmonization">
          <a href="#" v-bind:class="{active: selection.harmonization}" class="nav-link" @click.stop.prevent="onSelectionClicked('harmonization')">{{tr('harmonization')}}</a>
        </li>
      </ul>
    </template>
  </div>
  `,
  props: {
    alternate: Boolean
  },
  data() {
    return {
      selection: {all: true, study: false, harmonization: false},
      visible: true
    }
  },
  computed: {
    showHarmonization: () => Mica.config.isHarmonizedDatasetEnabled,
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
    }

    function onDestroy() {
      window.removeEventListener("scroll", onScroll);
    }

    window.addEventListener("scroll", onScroll);

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
      withSort: true,
      parseForTable: genericParseForTable
    },
    'study-design-chart': {
      id: 'study-design-chart',
      title: Mica.tr['study-design-chart-title'],
      text: Mica.tr['study-design-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      termsSorterFunction: chartTableTermSorters.sort,
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
      termsSorterFunction: chartTableTermSorters.sort,
      parseForTable: genericParseForTable,
      agg: 'model-numberOfParticipants-participant-number-range',
      vocabulary: 'numberOfParticipants-participant-range',
      dataKey: 'obiba.mica.RangeAggregationResultDto.ranges',
    },
    'bio-samples-chart': {
      id: 'bio-samples-chart',
      title: Mica.tr['bio-samples-chart-title'],
      text: Mica.tr['bio-samples-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      termsSorterFunction: chartTableTermSorters.sort,
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
      termsSorterFunction: chartTableTermSorters.sort,
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

  class MicaQueryChangeListener {
    constructor() {
      this.loading = true;

      EventBus.register(EVENTS.QUERY_TYPE_SELECTION, this.__onQueryExecute.bind(this));
      EventBus.register(EVENTS.QUERY_TYPE_UPDATE, this.__onQueryExecute.bind(this));
      EventBus.register(EVENTS.QUERY_TYPE_UPDATES_SELECTION, this.__onQueryExecute.bind(this));
      EventBus.register(EVENTS.QUERY_TYPE_DELETE, this.__onQueryExecute.bind(this));
      EventBus.register(EVENTS.QUERY_TYPE_PAGINATE, this.__onQueryExecute.bind(this));
      EventBus.register(EVENTS.QUERY_TYPE_COVERAGE, this.__onQueryExecute.bind(this));
      EventBus.register(EVENTS.QUERY_TYPE_GRAPHICS, this.__onQueryExecute.bind(this));

      EventBus.register(EVENTS.QUERY_TYPE_GRAPHICS_RESULTS, this.__onQueryResult.bind(this));
      EventBus.register(`${TYPES.VARIABLES}-results`, this.__onQueryResult.bind(this));
      EventBus.register(`${TYPES.DATASETS}-results`, this.__onQueryResult.bind(this));
      EventBus.register(`${TYPES.STUDIES}-results`, this.__onQueryResult.bind(this));
      EventBus.register(`${TYPES.NETWORKS}-results`, this.__onQueryResult.bind(this));
      EventBus.register(`coverage-results`, this.__onQueryResult.bind(this));
    }

    __onQueryExecute() {
      this.loading = true;
    }

    __onQueryResult() {
      this.loading = false;
    }
  }

  const queryAlertListener  = new MicaQueryAlertListener();

  function foundAttributeIsOk(studyTypeSelection, foundAttr) {
    let isOk = !foundAttr || foundAttr.value.length === 0 || foundAttr.value === 'Network';

    if (studyTypeSelection.study) {
      isOk = isOk || foundAttr.value === 'Study' || foundAttr.value === 'StudyDataset';
    } else if (studyTypeSelection.harmonization) {
      isOk = isOk || foundAttr.value === 'HarmonizationStudy' || foundAttr.value === 'HarmonizationDataset';
    } else {
      isOk = true;
    }

    return isOk
  }

  function processTaxonomyForStudyTypeSelection(studyTypeSelection, taxonomy) {
    if (taxonomy) {
      if (studyTypeSelection.study || studyTypeSelection.harmonization) {
        let clone = JSON.parse(JSON.stringify(taxonomy));
        let clonedVocabularies = clone.vocabularies.filter(voc => {
          let foundAttr = (voc.attributes || []).find(attr => attr.key === 'forClassName');
          return foundAttributeIsOk(studyTypeSelection, foundAttr);
        });

        clonedVocabularies.forEach(voc => {
          if (Array.isArray(voc.terms)) {
            voc.terms = voc.terms.filter(term => {
              let foundAttr = (term.attributes || []).find(attr => attr.key === 'className');
              return foundAttributeIsOk(studyTypeSelection, foundAttr);
            });
          }
        });

        clone.vocabularies = clonedVocabularies;

        return clone;
      }
    }

    return taxonomy;
  }

  new Vue({
    el: '#search-application',
    data() {
      return {
        queryChangeListener: new MicaQueryChangeListener(),
        taxonomies: {},
        targets: [],
        display: DISPLAYS.LISTS,
        currentListType: null,
        message: '',
        selectedTaxonomy: null,
        selectedTaxonomyTitle: null,
        selectedTarget: null,
        studyHasCheckboxes: false,
        networkHasCheckboxes: false,
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
        hasListResult: false,
        hasCoverageResult: false,
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
        queriesWithZeroHitsToUpdate: [],
        coverageFixedHeaderHandler: null,
        currentStudyTypeSelection: null,
        pagination: null,
        pageSizeSelector: null,
        showStudyShortcut: (Mica.isHarmonizedDatasetEnabled && !Mica.isSingleStudyEnabled) || !this.currentStudyTypeSelection || this.currentStudyTypeSelection.all,
      };
    },
    methods: {
      updateStudyTypeFilter() {
        setTimeout(() => {
          const filter = document.querySelector('#study-filter-shortcut');
          const tabPane = document.querySelector(".tab-pane .show");
          if (filter && filter.parentNode) {
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
              let splitQuery = Criterion.splitQuery(target);
              let isLoneStudyClassNameQuery = key === TARGETS.STUDY && splitQuery.length === 1 && Criterion.splitQuery(target)[0].args[0] === 'Mica_study.className';
              if (!isLoneStudyClassNameQuery) {
                this.noQueries = false;
                break;
              }
            }
          }
        }
      },
      getTaxonomyForTarget(target) {
        let studyTypeSelection = MicaTreeQueryUrl.getStudyTypeSelection(MicaTreeQueryUrl.getTree());

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
          if (taxonomy) {
            result.push(taxonomy);
          }
        }

        if (Array.isArray(result[0])) {
          let finalResult = [];
          result[0].forEach(res => {
            const processed = processTaxonomyForStudyTypeSelection(studyTypeSelection, res);
            if (processed ) {
              finalResult.push(processed);
            }
          });
          return finalResult;
        } else {
          return processTaxonomyForStudyTypeSelection(studyTypeSelection, target === 'study' && !result[0] ? MINIMUM_STUDY_TAXONOMY : result[0]);
        }
      },
      // show a modal with all the vocabularies/terms of the selected taxonomy
      // initialized by the query terms and update/trigger the query on close
      onTaxonomySelection(payload) {
        let studyTypeSelection = MicaTreeQueryUrl.getStudyTypeSelection(MicaTreeQueryUrl.getTree());

        let selectedTaxonomy = this.taxonomies[payload.taxonomyName];
        this.selectedTarget = payload.target;

        let selectedTaxonomyVocabulariesTitle = '';
        if (selectedTaxonomy) {
          this.selectedTaxonomyTitle = selectedTaxonomy.title;

          if (selectedTaxonomy.name.startsWith('Mica_')) {
            selectedTaxonomy = processTaxonomyForStudyTypeSelection(studyTypeSelection, selectedTaxonomy);
          }

          selectedTaxonomyVocabulariesTitle = selectedTaxonomy.vocabularies.map(voc => voc.title[0].text).join(', ');
        } else {
          const foundTaxonomyGroup = this.findTaxonomyGroup(payload.taxonomyName, payload.target);
          selectedTaxonomy = foundTaxonomyGroup.taxonomies;
          this.selectedTaxonomyTitle = foundTaxonomyGroup.title;
        }

        this.selectedTaxonomy = selectedTaxonomy;

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
        this.checkCurrentModeAndRectify();
      },
      findProperType() {
        if (Mica.config.isSingleStudyEnabled) {
          if (Mica.config.isCollectedDatasetEnabled || Mica.config.isHarmonizedDatasetEnabled) {
            return TYPES.VARIABLES;
          } else if (!Mica.config.isCollectedDatasetEnabled) {
            return TYPES.NETWORKS;
          }
        } else {
          return TYPES.STUDIES;
        }
      },
      setLocation(searchQuery) {
        const urlParts = MicaTreeQueryUrl.parseUrl();
        const searchParams = urlParts.searchParams || {};

        const display = urlParts.hash || 'list';
        const type = searchParams.type || this.findProperType();

        let params = [`type=${type}`];
        if (searchQuery) {
          params.push(`query=${searchQuery}`);
        }

        const urlSearch = params.join("&");
        const hash = `${display}?${urlSearch}`;

        window.location.hash = `#${hash}`;
      },

      onClearQuery() {
        let currentPathName = window.location.pathname;
        let studyClassName = Mica.defaultSearchMode;

        if (currentPathName.startsWith("/harmonization")) {
          studyClassName = 'HarmonizationStudy';
        } else if (currentPathName.startsWith("/individual")) {
          studyClassName = 'Study';
        }

        let tree = new RQL.QueryTree();
        // create target and add query as child, done!
        let targetQuery = new RQL.Query(TARGETS.STUDY);
        tree.addQuery(null, targetQuery);
        tree.addQuery(targetQuery, new RQL.Query('in', ['Mica_study.className', studyClassName]));

        this.setLocation(tree.serialize());
      },
      onLocationChanged(payload) {
        this.downloadUrlObject = MicaTreeQueryUrl.getDownloadUrl(payload);

        let tree = MicaTreeQueryUrl.getTree();
        this.currentStudyTypeSelection = MicaTreeQueryUrl.getStudyTypeSelection(tree);

        // query string to copy
        tree.findAndDeleteQuery((name) => 'limit' === name);
        this.queryToCopy = tree.serialize();

        // make query string for adding documents to cart
        const makeQueryToCart = function(tree, target, fields) {
          tree.findAndDeleteQuery((name) => 'fields' === name);
          tree.findAndDeleteQuery((name) => 'limit' === name);
          let vQuery = tree.search((name) => name === target);
          if (!vQuery) {
            vQuery = new RQL.Query(target,[]);
            tree.addQuery(null, vQuery);
          }
          let limitQuery = tree.search((name, args, parent) => 'limit' === name && parent.name === target);
          if (limitQuery) {
            limitQuery.args = [0, 100000];
          } else {
            tree.addQuery(vQuery, new RQL.Query('limit', [0, Mica.config.maxItemsPerSet]));
          }
          tree.addQuery(vQuery, new RQL.Query('fields', fields));
          return tree.serialize();
        }

        this.queryToVariablesCart = makeQueryToCart(tree, TARGETS.VARIABLE, ['variableType']);
        this.queryToStudiesCart = makeQueryToCart(tree, TARGETS.STUDY, ['acronym.*']);
        this.queryToNetworksCart = makeQueryToCart(tree, TARGETS.NETWORK, ['acronym.*']);

        this.refreshQueries();

        // result
        $(`.nav-pills #${payload.display}-tab`).tab('show');
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
      checkCurrentModeAndRectify() {
        let tree = MicaTreeQueryUrl.getTree() || new RQL.QueryTree();
        let foundStudyClassName = tree ? tree.search((name, args, parent) => 'in' === name && args[0] === 'Mica_study.className' && parent.name === TARGETS.STUDY) : null;

        let currentPathName = window.location.pathname;
        let studyClassName = Mica.defaultSearchMode;

        if (currentPathName.startsWith("/harmonization")) {
          studyClassName = 'HarmonizationStudy';
        } else if (currentPathName.startsWith("/individual")) {
          studyClassName = 'Study';
        }

        if (foundStudyClassName &&
          ((Array.isArray(foundStudyClassName.args[1]) && foundStudyClassName.args[1].length === 1 && foundStudyClassName.args[1][0] !== studyClassName) ||
          (!Array.isArray(foundStudyClassName.args[1]))) && foundStudyClassName.args[1] !== studyClassName) {
          tree.findAndUpdateQuery((name, args) => args[0] === 'Mica_study.className', ['Mica_study.className', studyClassName]);
          this.setLocation(tree.serialize());
        } else if (!foundStudyClassName) {
          let targetQuery = tree.search((name) => name === TARGETS.STUDY);
          if (!targetQuery) {
            // create target and add query as child, done!
            targetQuery = new RQL.Query(TARGETS.STUDY);
            tree.addQuery(null, targetQuery);

          }

          tree.addQuery(targetQuery, new RQL.Query('in', ['Mica_study.className', studyClassName]));
          this.setLocation(tree.serialize());
        }
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
      normalizeSetName(set) {
        if (set.name.startsWith('dar:')) {
          return set.name.replace('dar:', '') + ' [' + Mica.tr['data-access-request'] + ']';
        }
        return set.name;
      },
      onCompare() {
        if (this.isStudiesToolsVisible) {
          window.location = contextPath + '/compare?type=studies&query=' + this.queryToStudiesCart.replace('limit(0,' + Mica.config.maxItemsPerSet + ')', 'limit(0,' + Mica.config.maxItemsPerCompare + ')');
        } else if (this.isNetworksToolsVisible) {
          window.location = contextPath + '/compare?type=networks&query=' + this.queryToNetworksCart.replace('limit(0,' + Mica.config.maxItemsPerSet + ')', 'limit(0,' + Mica.config.maxItemsPerCompare + ')');
        }
      },
      onAddToCart() {
        const makeOnSuccess = function(type) {
          return function (cart, oldCart) {
            SetService.showCount('#cart-count', cart, Mica.locale);
            if (cart.count === oldCart.count) {
              MicaService.toastInfo(Mica.tr[type === 'variables' ? 'no-variable-added' :
                (type === 'studies' ? 'no-study-added' : 'no-network-added')]);
            } else {
              MicaService.toastSuccess(Mica.tr[type + '-added-to-cart'].replace('{0}', (cart.count - oldCart.count).toLocaleString(Mica.locale)));
            }
          };
        };

        if (this.downloadUrlObject) {
          if (this.isVariablesToolsVisible) {
            if (Array.isArray(this.variableSelections) && this.variableSelections.length > 0) {
              VariablesSetService.addToCart(this.variableSelections, makeOnSuccess('variables'));
            } else {
              VariablesSetService.addQueryToCart(this.queryToVariablesCart, makeOnSuccess('variables'));
            }
          } else if (this.isStudiesToolsVisible) {
            StudiesSetService.addQueryToCart(this.queryToStudiesCart, makeOnSuccess('studies'));
          } else if (this.isNetworksToolsVisible) {
            NetworksSetService.addQueryToCart(this.queryToNetworksCart, makeOnSuccess('networks'));
          }
        }
      },
      onAddToSet(setId) {
        const onsuccess = (set, oldSet) => {
          if (set.count === oldSet.count) {
            MicaService.toastInfo(Mica.tr['no-variable-added-set'].replace('{{arg0}}', '"' + this.normalizeSetName(set) + '"'));
          } else {
            MicaService.toastSuccess(Mica.tr['variables-added-to-set'].replace('{0}', (set.count - oldSet.count).toLocaleString(Mica.locale)).replace('{1}', '"' + this.normalizeSetName(set) + '"'));
          }

          this.newVariableSetName = '';
          VariablesSetService.showSetsCount($('#list-count'), sets => {
            this.variableSets = sets.filter(set => set.name && !set.locked);
          });
        };

        if (setId || (this.newVariableSetName && this.newVariableSetName.length > 0)) {
          if (Array.isArray(this.variableSelections) && this.variableSelections.length > 0) {
            VariablesSetService.addToSet(setId, this.newVariableSetName, this.variableSelections, onsuccess);
          } else {
            VariablesSetService.addQueryToSet(setId, this.newVariableSetName, this.queryToVariablesCart, onsuccess);
          }
        }
      },
      updateFormForDownload(type, form) {
        const studyTypeSelection = MicaTreeQueryUrl.getStudyTypeSelection(MicaTreeQueryUrl.getTree());
        const studyType = studyTypeSelection.study ? 'individual-study' : studyTypeSelection.harmonization ? 'harmonization-study' : null;
        const inputStudyType = document.createElement('input');
        inputStudyType.name = 'studyType';
        inputStudyType.value = studyType;
        form.appendChild(inputStudyType);

        let columnsToHide = [];

        switch (type) {
          case TYPES.NETWORKS:
            if (studyTypeSelection.harmonization) {
              columnsToHide = ['showNetworksStudyDatasetsColumn', 'showNetworksStudyVariablesColumn']
            } else if (studyTypeSelection.study) {
              columnsToHide = ['showNetworksHarmonizationDatasetsColumn', 'showNetworksDataschemaVariablesColumn']
            }
            break;

          case TYPES.STUDIES:
            if (studyTypeSelection.harmonization) {
              columnsToHide = ['showStudiesStudyDatasetsColumn',
                'showStudiesStudyVariablesColumn',
                'showStudiesParticipantsColumn',
                'showStudiesDesignColumn',
                'showStudiesQuestionnaireColumn',
                'showStudiesPmColumn',
                'showStudiesBioColumn',
                'showStudiesOtherColumn'];
            } else if (studyTypeSelection.study) {
              columnsToHide = ['showStudiesHarmonizationDatasetsColumn', 'showStudiesDataschemaVariablesColumn'];
            }
            break;

          case TYPES.VARIABLES:
            if (studyTypeSelection.harmonization) {
              columnsToHide = ['showVariablesPopulationsColumn', 'showVariablesDataCollectionEventsColumn'];
            }
            break;
        }

        columnsToHide.forEach(column => {
          let checkbox = document.createElement('input');
          checkbox.type = 'checkbox';
          checkbox.name = 'columnsToHide';
          checkbox.value = column;
          checkbox.checked = true;

          form.appendChild(checkbox);
        });
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
          this.updateFormForDownload(this.downloadUrlObject.type, form);

          document.body.appendChild(form);
          form.submit();
          form.remove();
        } else {
          MicaService.toastError(Mica.tr['no-coverage-available']);
        }
      },
      onDownloadExportQueryResult() {
        if (this.downloadUrlObject) {
          const form = document.createElement('form');
          form.setAttribute('class', 'hidden');
          form.setAttribute('method', 'post');

          form.action = this.downloadUrlObject.url.replace('_rql_csv', '_export');
          form.accept = 'application/octet-stream';

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

        if (data) {
          let dto;
          switch (payload.type) {
            case TYPES.VARIABLES:
              dto = 'variableResultDto';
              this.currentListType = TYPES.VARIABLES;
              break;
            case TYPES.DATASETS:
              dto = 'datasetResultDto';
              this.currentListType = TYPES.DATASETS;
              break;
            case TYPES.STUDIES:
              dto = 'studyResultDto';
              this.currentListType = TYPES.STUDIES;
              break;
            case TYPES.NETWORKS:
              dto = 'networkResultDto';
              this.currentListType = TYPES.NETWORKS;
              break;
          }

          if (!dto) {
            throw new Error(`Payload has invalid type ${payload.type}`);
          }

          this.hasListResult = (data[dto] || {totalHits: 0}).totalHits > 0;

          if (data.variableResultDto && data.variableResultDto.totalHits) {
            this.counts.variables = data.variableResultDto.totalHits.toLocaleString();
          }

          if (data.datasetResultDto && data.datasetResultDto.totalHits) {
            this.counts.datasets = data.datasetResultDto.totalHits.toLocaleString();
          }

          if (data.studyResultDto && data.studyResultDto.totalHits) {
            this.counts.studies = data.studyResultDto.totalHits.toLocaleString();
          }

          if (data.networkResultDto && data.networkResultDto.totalHits) {
            this.counts.networks = data.networkResultDto.totalHits.toLocaleString();
          }

          let tree = MicaTreeQueryUrl.getTree();
          const target = TYPES_TARGETS_MAP[payload.type];
          if (target) {
            const limitQuery = tree.search((name, args, parent) => 'limit' === name && parent.name === target);
            if (limitQuery) {
              if (limitQuery.args[0] > data[dto].totalHits) {
                limitQuery.args[0] = 0;

                const urlParts = MicaTreeQueryUrl.parseUrl();
                const searchParams = urlParts.searchParams || {};

                const display = urlParts.hash || 'list';
                const type = searchParams.type || TYPES.VARIABLES;

                let params = [`type=${type}`, `query=${tree.serialize()}`];

                const urlSearch = params.join("&");
                const hash = `${display}?${urlSearch}`;

                // for pagination and size selector
                let from = limitQuery.args[0];
                let size = limitQuery.args[1];
                this.pagination.update((data[dto] || {totalHits: 0}).totalHits, size, (from/size)+1);
                this.pageSizeSelector.update(size);

                window.location.hash = `#${hash}`;
              } else {
                let from = limitQuery.args[0];
                let size = limitQuery.args[1];
                this.pagination.update((data[dto] || {totalHits: 0}).totalHits, size, (from/size)+1);
                this.pageSizeSelector.update(size);
              }
            }
          }
        }

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
      },
      onCoverageResult(payload) {
        this.display = DISPLAYS.COVERAGE;
        this.hasCoverageResult = payload.response.rows !== undefined;
        if (this.hasCoverageResult) { // for filters
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

          this.queriesWithZeroHitsToUpdate = [];
          const taxonomyTermsMap = {};
          const termsWithZeroHits = {};
          payload.response.termHeaders.forEach((termHeader, index) => {
            const key = taxonomyNames[index] + '.' + vocabularyNames[index], name = termHeader.entity.name;

            if (!Array.isArray(taxonomyTermsMap[key])) {
              taxonomyTermsMap[key] = [];
            }
            taxonomyTermsMap[key].push(name);

            if (termHeader.hits === 0) {
              if (!Array.isArray(termsWithZeroHits[key])) {
                termsWithZeroHits[key] = [];
              }

              termsWithZeroHits[key].push(name);
            }
          });

          this.hasCoverageTermsWithZeroHits = Object.keys(termsWithZeroHits).length > 0; // active?
          if (this.hasCoverageTermsWithZeroHits) {
            for (const queryKey in termsWithZeroHits) {
              this.queriesWithZeroHitsToUpdate.push(new RQL.Query('in', [queryKey, taxonomyTermsMap[queryKey].filter(x => !termsWithZeroHits[queryKey].includes(x))]));
            }
          }
        }
      },
      onZeroColumnsToggle() {
        this.queriesWithZeroHitsToUpdate.forEach(query => {
          EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [{target: fromBucketToTarget(this.selectedBucket), query, display: DISPLAYS.COVERAGE}]});
        });
      },
      onFullCoverage() {
        EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [{target: fromBucketToTarget(this.selectedBucket), query: this.queryForFullCoverage, display: DISPLAYS.COVERAGE}]});
      },
      onPagination(data) {
        EventBus.$emit(EVENTS.QUERY_TYPE_PAGINATE, {
          display: DISPLAYS.LISTS,
          type: this.currentListType,
          target: TYPES_TARGETS_MAP[this.currentListType],
          from: data.from,
          size: data.size
        });
      },
      onPageSizeChanged(data) {
        EventBus.$emit(EVENTS.QUERY_TYPE_PAGINATE, {
          display: DISPLAYS.LISTS,
          type: this.currentListType,
          target: TYPES_TARGETS_MAP[this.currentListType],
          from: 0,
          size: data.size
        });
      }
    },
    computed: {
      loading() {
        return this.queryChangeListener.loading;
      },
      selectedQuery() {
        if (this.selectedTarget) {
          return this.queries[this.selectedTarget];
        }

        return undefined;
      },
      numberOfSetsRemaining() {
        return Mica.maxNumberOfSets - (this.variableSets || []).length;
      },
      isVariablesToolsVisible() {
        let downloadUrlObject = this.downloadUrlObject || {};
        return downloadUrlObject.type === 'variables' || downloadUrlObject.type === 'datasets';
      },
      isStudiesToolsVisible() {
        return (this.downloadUrlObject || {}).type === 'studies';
      },
      isNetworksToolsVisible() {
        return (this.downloadUrlObject || {}).type === 'networks';
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

      this.pagination = new OBiBaPagination('obiba-pagination-top', true, this.onPagination),
      this.pageSizeSelector = new OBiBaPageSizeSelector('obiba-page-size-selector-top', DEFAULT_PAGE_SIZES, DEFAULT_PAGE_SIZE, this.onPageSizeChanged);

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
                TaxonomyHelper.newInstance().sortVocabulariesTerms(taxo);

                if (taxo.name === 'Mica_study') {
                  let studyClassNameVocabulary = taxo.vocabularies.find(vocabulary => vocabulary.name === "className");
                  if (studyClassNameVocabulary) {
                    if (!Array.isArray(studyClassNameVocabulary.attributes)) {
                      studyClassNameVocabulary.attributes = [];
                    }

                    studyClassNameVocabulary.attributes.push({"key": "uiTermsReadOnly", "value": "true"});
                    studyClassNameVocabulary.attributes.push({"key": "uiHideInBuilder", "value": "true"});
                  }
                }

                this.taxonomies[taxo.name] = taxo;
              }
            });

            if (!this.taxonomies['Mica_study']) {
              this.taxonomies['Mica_study'] = MINIMUM_STUDY_TAXONOMY;
            }

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
          this.variableSets = data.filter(set => set.name && !set.locked);
        }});
      this.onExecuteQuery();
    },
    updated() {
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
