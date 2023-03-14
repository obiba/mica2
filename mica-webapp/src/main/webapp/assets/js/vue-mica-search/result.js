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

const COVERAGE_PAGE_SIZE = 48;
const ROW_POPUP_MARGIN = 15;

const DataTableDefaults = {
  searching: false,
  ordering: false,
  destroy: true,
  lengthMenu: [10, 20, 50, 100],
  pageLength: 20,
  dom: "<<'toolbar d-inline-block'><'float-right'<'d-inline-block pr-2'l><'d-inline-block'p>>> <''<'table-responsive 'tr> > <<'float-right'<'d-inline-block pr-2'l><'d-inline-block'p>>>",
  preDrawCallback: function (settings) {
    const api = new $.fn.dataTable.Api(settings);
    const data  = api.data();
    const paginationAndLength = $(this)
      .closest('.dataTables_wrapper')
      .find('.dataTables_paginate, .dataTables_length');
    if (api.page.info().pages > 1) paginationAndLength.removeClass('invisible').addClass('visible');
    else paginationAndLength.removeClass('visible').addClass('invisible');

    if (data) {
      const searchAndPageInfo = $(this)
        .closest('.dataTables_wrapper')
        .find('.dataTables_info');
      searchAndPageInfo.toggle(false);
    }
  }
};

const EntityResult = {
  props: {
    showCheckboxes: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      dataTable: null,
      ajaxCallback: null,
      type: null,
      target: null,
      showResult: false,
      selections: [],
      studyTypeSelection: {all: true, study: false, harmonization: false},
      parsed: []
    };
  },
  computed: {
    withNetworks: function() {
      return this.getMicaConfig().isNetworkEnabled && !this.getMicaConfig().isSingleNetworkEnabled;
    },
    withStudies: function() {
      return !this.getMicaConfig().isSingleStudyEnabled
    },
    withCollectedDatasets: function() {
      return this.getMicaConfig().isCollectedDatasetEnabled;
    },
    withHarmonizedDatasets: function() {
      return this.getMicaConfig().isHarmonizedDatasetEnabled;
    }
  },
  methods: {
    /**
     * Callback invoked when request response arrives
     */
    onResults(payload) {
      let displayOptions = this.getDisplayOptions();
      displayOptions.showCheckboxes = this.showCheckboxes;

      this.studyTypeSelection = payload.studyTypeSelection || this.studyTypeSelection;
      this.parsed = this.parser.parse(payload.response, this.getMicaConfig(), this.localize, displayOptions, this.studyTypeSelection);
      this.showResult = this.parsed.totalHits > 0;
      if (!this.showResult) this.parsed = [];
    },
    clearSelections() {
      this.selections = [];
    },
    isSelected(id) {
      return this.selections && this.selections.includes(id)
    },
    onSelectionChanged(ids, selected) {
      if (selected) {
        if (Array.isArray(ids)) {
          ids.forEach(id => {
            if (!this.selections.includes(id)) {
              this.selections.push(id);
            }
          });
        }
      } else {
        if (Array.isArray(ids)) {
          ids.forEach(id => {
            const idx = this.selections.indexOf(id);
            if (idx > -1) {
              this.selections.splice(idx, 1);
            }
          });
        } else {
          this.selections = [];
        }
      }

      this.getEventBus().$emit(`${this.type}-selections-updated`, {selections: this.selections});
    },
    getEventBus: () => EventBus,
    getMicaConfig: () => Mica.config,
    getLocale: () => Mica.locale,
    getDisplayOptions: () => Mica.display,
    normalizePath: (path) => {
      return contextPath + path;
    },
    headerSelectionEventHandler(event) {
      const tableSelector = `#vosr-${this.type}-result`;
      // fa icons
      const checkedIconClassName = 'fa-check-square';
      const notCheckedIconClassName = 'fa-square';

      let headerSelectionIcon = event.target;
      let isHeaderChecked = headerSelectionIcon.classList.contains(checkedIconClassName);

      if (isHeaderChecked) {
        headerSelectionIcon.classList.remove(checkedIconClassName);
        headerSelectionIcon.classList.add(notCheckedIconClassName);
      } else {
        headerSelectionIcon.classList.remove(notCheckedIconClassName);
        headerSelectionIcon.classList.add(checkedIconClassName);
      }

      let selectionIds = [];
      document.querySelectorAll(`${tableSelector} tbody i[data-item-id]`).forEach((row) => {
        if (isHeaderChecked) {
          row.classList.remove(checkedIconClassName);
          row.classList.add(notCheckedIconClassName);
        } else {
          row.classList.remove(notCheckedIconClassName);
          row.classList.add(checkedIconClassName);
        }
        selectionIds.push(row.dataset.itemId);
      });

      if (selectionIds.length > 0) {
        this.onSelectionChanged(selectionIds, !isHeaderChecked);
      }
    },
    rowItemSelectionEventHandler(event) {
      // fa icons
      const checkedIconClassName = 'fa-check-square';
      const notCheckedIconClassName = 'fa-square';

      let element = event.target;
      let isElementChecked = element.classList.contains(checkedIconClassName);

      if (isElementChecked) {
        element.classList.remove(checkedIconClassName);
        element.classList.add(notCheckedIconClassName);
      } else {
        element.classList.remove(notCheckedIconClassName);
        element.classList.add(checkedIconClassName);
      }

      this.onSelectionChanged([element.dataset.itemId], !isElementChecked);
    },
    setHeaderSelectionClickEvent(tableSelector) {
      let headerSelectionIcon = document.querySelector(`${tableSelector} thead i:not(.fa-plus-square)`);

      if (headerSelectionIcon) {
        headerSelectionIcon.removeEventListener('click', this.headerSelectionEventHandler);
        headerSelectionIcon.addEventListener('click', this.headerSelectionEventHandler);
      }
    },
    setResultSelectionClickEvent(element) {
      if (element) {
        element.removeEventListener('click', this.rowItemSelectionEventHandler);
        element.addEventListener('click', this.rowItemSelectionEventHandler);
      }
    },
    setCheckBoxesCheckStatusAndEvents() {
      // fa icons
      const checkedIconClassName = 'fa-check-square';
      const notCheckedIconClassName = 'fa-square';

      const tableSelector = `#vosr-${this.type}-result`;
      this.setHeaderSelectionClickEvent(tableSelector);

      let headerSelectionIcon = document.querySelector(`${tableSelector} thead i.far:not(.fa-plus-square)`);

      if (headerSelectionIcon) {
        headerSelectionIcon.classList.remove(checkedIconClassName);
        headerSelectionIcon.classList.add(notCheckedIconClassName);
      }

      let tableResultRows = [...document.querySelectorAll(`${tableSelector} tbody i[data-item-id]`)];

      tableResultRows.forEach((row) => {
        let itemId = row.dataset.itemId;

        if (this.isSelected(itemId)) {
          row.classList.remove(notCheckedIconClassName);
          row.classList.add(checkedIconClassName);
        } else {
          row.classList.remove(checkedIconClassName);
          row.classList.add(notCheckedIconClassName);
        }

        this.setResultSelectionClickEvent(row);
      });
    },
    localize: (entries) => StringLocalizer.localize(entries)
  },
  updated() {
    this.$nextTick(() => this.setCheckBoxesCheckStatusAndEvents());
  },
  mounted() {
    this.getEventBus().register(`${this.type}-results`,this.onResults.bind(this));
    this.getEventBus().register("clear-results-selections", this.clearSelections.bind(this));
  },
  beforeDestroy() {
    this.dataTable = null;
    this.getEventBus().unregister(`${this.type}-results`, this.onResults);
    this.getEventBus().unregister("clear-results-selections", this.clearSelections);
  }
}

const GraphicResult = {
  template: `
  <div v-bind:id="cardId" class="card card-primary card-outline">
    <div v-if="!hideHeader" class="card-header">
      <h3 class="card-title">{{chartDataset.options.title | translate}}</h3>
      <div class="card-tools float-right">
        <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" v-bind:title="'collapse' | translate">
          <i class="fas fa-minus"></i>
        </button>
        </div>
    </div>
    <div class="card-body">
      <p class="text-muted">{{chartDataset.options.text | translate}}</p>
      <div v-bind:id="containerId" class="row">
        <div v-bind:id="chartContainerId" class="col-sm-12 col-xl-6 my-auto"></div>

        <div v-bind:id="tableContainerId" class="col-sm-12 col-xl-6 overflow-auto" style="max-height: 24em">
          <table id="vosr-datasets-result" class="table table-striped" width="100%">
            <thead>
              <tr class="row" v-on:click.prevent="resetSort()">
                <th class="col" v-for="(col, index) in chartDataset.tableData.cols" v-bind:key="index">
                  <span>{{ col }}</span>
                  <button v-if="chartDataset.options.withSort" v-on:click.stop="toggleSortColumn(index)" type="button" class="btn btn-xs ml-1"><i :class="'fas fa-' + sortClass(index)"></i></button>
                </th>
              </tr>
            </thead>
            <tbody>
                <tr class="row" v-for="(row, index) in rows" v-bind:key="index">
                  <td class="col">{{row.title}}</td>

                  <td class="col" v-bind:title="totals ? (100 * row.count/totals.countTotal).toFixed(2) + '%' : ''" v-if="row.count > 0">
                    <a href="" v-on:click="onCountClick($event, row.vocabulary, row.key, row.queryOverride)" class="query-anchor">{{row.count}}</a>
                    <small class="ml-1" v-if="chartDataset.options.withTotals && chartDataset.options.withPercentages">({{totals ? (100 * row.count/totals.countTotal).toFixed(2) + '%' : ''}})</small>
                  </td>

                  <td class="col" v-bind:title="totals ? (0).toFixed(2) + '%' : ''" v-if="row.count === 0">
                    <span class="text-muted">{{row.count}}</span>
                    <small v-if="chartDataset.options.withTotals && chartDataset.options.withPercentages" class="ml-1 text-muted">({{totals ? (0).toFixed(2) + '%' : ''}})</small>
                  </td>

                  <td class="col" v-bind:title="totals ? (100 * row.subAgg/totals.subAggTotal).toFixed(2) + '%' : ''" v-if="row.subAgg !== undefined">
                    <span v-bind:class="{ 'text-muted': row.subAgg !== undefined && row.subAgg === 0 }">{{row.subAgg !== undefined && row.subAgg === 0 ? '-' : row.subAgg.toLocaleString()}}</span>
                  </td>
                </tr>
            </tbody>
            <tfoot v-if="totals">
              <tr class="row">
                  <th class="col">{{ 'graphics.total' | translate }}</th>
                  <th class="col">
                    <span>{{totals.countTotal.toLocaleString()}}</span>
                    <small class="ml-1" v-if="chartDataset.options.withTotals && chartDataset.options.withPercentages">({{(100).toFixed(2) + '%'}})</small>
                  </th>
                  <th class="col" v-if="totals.subAggTotal !== undefined">{{totals.subAggTotal.toLocaleString()}}</th>
                </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  </div>
  `,
  name: 'graphic-result',
  props: {
    position: Number,
    totalHits: Number,
    chartDataset: Object,
    hideHeader: Boolean
  },
  data: function() {
    const agg = this.chartDataset.options.agg;

    return {
      chart: null,
      cardId: this.chartDataset.options.id,
      containerId: `vosrs-charts-container-${this.position}`,
      chartContainerId: `vosrs-charts-${agg}-${this.position}`,
      tableContainerId: `vosrs-charts-${agg}-${this.position}-table`,
      canvasId: `vosrs-charts-${agg}-${this.position}-canvas`,
      sort: {
        index: undefined,
        direction: undefined
      }
    }
  },
  computed: {
    rows() {
      return this.chartDataset.tableData.rows.map(r => r);
    },
    totals() {
      let totals = this.chartDataset.options.withTotals ? {countTotal: 0, subAggTotal: 0} : null;

      if (this.chartDataset.options.withTotals) {
        this.chartDataset.tableData.rows.forEach(row => {
          totals.countTotal += row.count;
          if (row.subAgg !== undefined) {
            totals.subAggTotal += row.subAgg;
          } else {
            totals.subAggTotal = undefined;
          }
        });
      }

      return totals;
    }
  },
  methods: {
    getEventBus: () => EventBus,
    renderCanvas() {
      let layout = this.chartDataset.plotData.layout || {};

      if ((this.chartDataset.options.type || 'bar') === 'bar') {
        layout.height = (2*1.42857)*12*(this.chartDataset.plotData.data[0] || {}).y.length;
      }

      Plotly.react(this.chartContainerId, this.chartDataset.plotData.data, layout, {responsive: true, displaylogo: false, modeBarButtonsToRemove: ['select2d', 'lasso2d', 'pan', 'zoom', 'autoscale', 'zoomin', 'zoomout', 'resetscale']});
    },
    onCountClick(event, vocabulary, term, queryOverride) {
      event.preventDefault();
      console.debug(`onCountClicked ${vocabulary}, ${term}`);

      const updates = [{
        target: 'study',
        query: new RQL.Query('in', ['Mica_study.className', 'Study']),
        operator: 'and'
      }];

      updates.push({target:'study', query: (queryOverride ? queryOverride : new RQL.Query('in', [`Mica_study.${vocabulary}`, `${term}`]))});

      this.getEventBus().$emit('query-type-updates-selection', {display: 'lists', type: `studies`, updates});
    },
    resetSort() {
      this.sort.index = null;
      this.sort.direction = null;

      this.rows = this.chartDataset.tableData.rows.map(r => r);
    },
    sortClass(index) {
      if (this.sort.index !== index) {
        return 'sort';
      } else {
        return `sort-${this.sort.direction}`;
      }
    },
    toggleSortColumn(index, goDown) {
      if (this.sort.index !== index) {
        this.sort.index = index;
        this.sort.direction = 'up';
      } else {
        this.sort.direction = this.sort.direction === 'up' ? 'down' : 'up';
      }

      if (goDown) {
        this.sort.direction = 'down';
      }

      const sortFields = ['title', 'count', 'subAgg'];

      this.rows.sort((rowA, rowB) => {
        let multiplier = 1;

        if (this.sort.direction === 'up') {
          multiplier = -1;
        }

        const a = rowA[sortFields[this.sort.index]];
        const b = rowB[sortFields[this.sort.index]];

        if (typeof a === 'number' || typeof b === 'number') {
          return (a - b) * multiplier;
        } else {
          return a.toString().localeCompare(b.toString()) * multiplier;
        }
      });
    }
  },
  mounted() {
    this.renderCanvas();

    if (this.chartDataset.options.initialSortIndex !== undefined) {
      this.toggleSortColumn(this.chartDataset.options.initialSortIndex, this.chartDataset.options.initialSortDirection === 'down');
    }
  },
   watch: {
    chartDataset(val) {
      if (val) this.renderCanvas();
    }
  }
};

const GraphicsResult = {
  template: `
  <div>
    <div v-for="(chartDataset, index) in chartDatasets" v-bind:key="index">
      <graphic-result v-bind:chart-dataset="chartDataset" v-bind:total-hits="totalHits" v-bind:position="index" v-bind:hideHeader="hideHeader"></graphic-result>
    </div>
    <div id="vosr-charts-container">
    </div>
  </div>
  `,
  name: 'graphics-result',
  props: {
    chartOptions: Array,
    hideHeader: Boolean,
    taxonomy: Object
  },
  components: {
    GraphicResult
  },
  data() {
    return {
      totalHits: 0,
      chartDatasets: null,
      parser: new GraphicsResultParser(this.normalizePath),
    }
  },
  methods: {
    onResults(payload) {
      this.chartDatasets = []
      // TODO make sure any resultDto can be used
      const studyResult = payload.response.studyResultDto;
      this.totalHits = studyResult.totalHits;

      if (this.totalHits > 0) {
        this.chartOptions.forEach((options) => {
          const aggData = studyResult.aggs.filter((item => item.aggregation === options.agg)).pop();
          if (aggData) {
            if (this.taxonomy) {
              options.taxonomy = this.taxonomy;
            }

            const [plotData, tableData] = this.parser.parse(aggData, options, this.totalHits);
            if (tableData.rows.length>0) {
              this.chartDatasets.push({plotData, tableData, options});
            }
          }
        });
      }
    },
    getEventBus: () => EventBus
  },
  mounted() {
    console.debug(`Prop ${this.options} AGGS ${this.aggs}`);
    this.getEventBus().register('query-type-graphics-results',this.onResults.bind(this));
  },
  beforeDestroy() {
    this.getEventBus().unregister('query-type-graphics-results', this.onResults);
  }
};

const VariablesResult = {
  template: `
  <div>
    <div class="row" v-show="showResult">
      <div class="col">
        <table id="vosr-variables-result" class="table table-striped" width="100%">
          <thead>
            <tr>
              <th v-if="showCheckboxes"><i class="far fa-square"></i></th>
              <th class="column-name">{{ "name" | translate }}</th>
              <th v-for="(column, index) in variableColumnNames" :key="index" :class="'column-'+ column" >{{ column | translate }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="parsedItem in parsed.data">
              <td v-for="cell in parsedItem" v-html="cell"></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
  `,
  name: 'variables-result',
  extends: EntityResult,
  data () {
    return {
      parser: new VariablesResultParser(this.normalizePath),
      type: "variables",
      target: "variable"
    }
  },
  computed: {
    // table headers
    variableColumnNames: function() {
      let displayOptions = this.getDisplayOptions();
      let columnKey = 'variableColumns';
      if (this.studyTypeSelection) {
        if (this.studyTypeSelection.study) {
          columnKey = 'variableColumnsIndividual';
        } else if(this.studyTypeSelection.harmonization) {
          columnKey = 'variableColumnsHarmonization';
        }
      }

      return (displayOptions[columnKey] || displayOptions.variableColumns)
        .filter(col => {
          if (col === 'type') {
            return this.withCollectedDatasets && this.withHarmonizedDatasets;
          } else if (col === 'study' || col === 'initiative') {
            return this.withStudies;
          }
          return true;
        })
        .map(col => col === 'label+description' ? 'label' : col);
    }
  }
}

const StudiesResult = {
  template: `
  <div>
    <div class="row" v-show="showResult">
      <div class="col">
        <table id="vosr-studies-result" class="table table-striped" width="100%">
          <thead>
            <tr>
              <th v-if="showCheckboxes" rowspan="2"><i class="far fa-square"></i></th>
              <th class="column-acronym" rowspan="2">{{ "acronym"  | translate }}</th>
              <th v-for="(item, index) in studyColumnItems" :key="index"
                :class="'column-'+ item.name"
                :rowspan="item.rowspan"
                :colspan="item.colspan">
                {{ item.name | translate }}
              </th>
            </tr>
            <tr>
              <th v-for="(item, index) in studyColumnItems2" :key="index" :class="'column-'+ item.name" :title="item.title | taxonomy-title">
              <span>
                <i v-if="item.icon" :class="item.icon"></i>
                {{ item.name | translate }}
              </span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="parsedItem in parsed.data">
              <td v-for="cell in parsedItem" v-html="cell"></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
  `,
  name: 'studies-result',
  extends: EntityResult,
  data () {
    return {
      parser: new StudiesResultParser(this.normalizePath, this.getLocale()),
      type: "studies",
      target: "study"
    }
  },
  computed:{
    // study headers, 1st row
    studyColumnItems: function() {
      let displayOptions = this.getDisplayOptions();
      let columnKey = 'studyColumns';
      if (this.studyTypeSelection) {
        if (this.studyTypeSelection.study) {
          columnKey = 'studyColumnsIndividual';
        } else if(this.studyTypeSelection.harmonization) {
          columnKey = 'studyColumnsHarmonization';
        }
      }

      return (displayOptions[columnKey] || displayOptions.studyColumns)
        .filter(col => {
          if (col === 'type') {
            return this.withCollectedDatasets && this.withHarmonizedDatasets;
          } else if (col === 'networks') {
            return this.withNetworks;
          } else if (col === 'individual') {
            return false;
          } else if (col === 'harmonization') {
            return false;
          } else if (['datasets', 'variables'].includes(col)) {
            return this.withCollectedDatasets || this.withHarmonizedDatasets;
          }
          return true;
        })
        .map(col => {
          return {
            name: col,
            rowspan: (['name', 'type', 'study-design', 'participants', 'networks'].includes(col) ? 2 : 1),
            colspan: (['name', 'type', 'study-design', 'participants', 'networks'].includes(col) ? 1 : (col === 'data-sources-available' ? 6 : 2))
          }
        });
    },
    // study headers, 2nd row
    studyColumnItems2: function() {
      let displayOptions = this.getDisplayOptions();
      let columnKey = 'studyColumns';
      if (this.studyTypeSelection) {
        if (this.studyTypeSelection.study) {
          columnKey = 'studyColumnsIndividual';
        } else if(this.studyTypeSelection.harmonization) {
          columnKey = 'studyColumnsHarmonization';
        }
      }

      const items2 = [];
      (displayOptions[columnKey] || displayOptions.studyColumns)
        .filter(col => {
          if (col === 'individual') {
            return this.withCollectedDatasets;
          } else if (col === 'harmonization') {
            return this.withHarmonizedDatasets;
          } else if (['datasets', 'variables'].includes(col)) {
            return this.withCollectedDatasets || this.withHarmonizedDatasets;
          }
          return col === 'data-sources-available';
        })
        .forEach((col, id) => {
          if (['individual', 'harmonization'].includes(col)) {
            items2.push({id: id, name: (this.studyTypeSelection.harmonization ? 'protocols' : 'datasets'), title: ''});
            items2.push({id: id, name: 'variables', title: ''});
          } else if (['datasets', 'variables'].includes(col)) {
            if (this.withCollectedDatasets) {
              items2.push({id: id, name: 'individual', title: ''});
            }
            if (this.withHarmonizedDatasets) {
              items2.push({id: id, name: 'harmonization', title: ''});
            }
          } else if (col === 'data-sources-available') {
            items2.push({
              id: id,
              title: 'Mica_study.populations-dataCollectionEvents-dataSources.questionnaires',
              icon: 'fa fa-file-alt'
              });
            items2.push({
              id: id,
              title: 'Mica_study.populations-dataCollectionEvents-dataSources.physical_measures',
              icon: 'fa fa-stethoscope'
              });
            items2.push({
              id: id,
              title: 'Mica_study.populations-dataCollectionEvents-dataSources.biological_samples',
              icon: 'fa fa-flask'
              });
            items2.push({
              id: id,
              title: 'Mica_study.populations-dataCollectionEvents-dataSources.cognitive_measures',
              icon: 'fas fa-brain'
              });
            items2.push({
              id: id,
              title: 'Mica_study.populations-dataCollectionEvents-dataSources.administratives_databases',
              icon: 'fas fa-database'
              });
            items2.push({
              id: id,
              title: 'Mica_study.populations-dataCollectionEvents-dataSources.others',
              icon: 'far fa-plus-square'
              });
          }
        });
      return items2;
    }
  },
  methods: {
    onAnchorClicked(event) {
      console.debug('Study onAnchorClicked');
      event.preventDefault();
      const anchor = $(event.target);
      const target = anchor.attr('data-target');
      const targetId = anchor.attr('data-target-id');
      const type = anchor.attr('data-type');
      const studyType = anchor.attr('data-study-type');

      const updates = [{target, query: new RQL.Query('in', ['Mica_study.id',targetId])}];

      if ("" !== studyType) {
        updates.push({target: 'study', query: new RQL.Query('in', ['Mica_study.className', studyType])});
      }

      this.getEventBus().$emit('query-type-updates-selection', {type: `${type}`, updates});
    }
  },
  mounted() {
    console.debug('Studies Result Mounted...');
    $('#vosr-studies-result').on('click', 'a.query-anchor', this.onAnchorClicked);
  }
};

const NetworksResult = {
  template: `
  <div>
    <div class="row" v-show="showResult">
      <div class="col">
        <table id="vosr-networks-result" class="table table-striped" width="100%">
          <thead v-if="withCollectedDatasets && withHarmonizedDatasets">
            <tr>
              <th v-if="showCheckboxes" rowspan="2"><i class="far fa-square"></i></th>
              <th class="column-acronym" rowspan="2">{{ "acronym" | translate }}</th>
              <th v-for="(item, index) in networkColumnItems" :key="index"
                :class="'column-' + item.name"
                :rowspan="item.rowspan"
                :colspan="item.colspan">
                {{ item.name | translate }}
              </th>
            </tr>
            <tr v-if="withCollectedDatasets || withHarmonizedDatasets">
              <th v-for="(item, index) in networkColumnItems2" :key="index" :class="'column-' + item.name">
                {{ item.name | translate }}
              </th>
            </tr>
          </thead>
          <thead v-else>
            <tr>
              <th v-if="showCheckboxes"><i class="far fa-square"></i></th>
              <th>{{ "acronym" | translate }}</th>
              <th v-for="(item, index) in networkColumnItems" :key="index" :class="'column-' + item.name">
                {{ item.name | translate }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="parsedItem in parsed.data">
              <td v-for="cell in parsedItem" v-html="cell"></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
  `,
  name: 'networks-result',
  extends: EntityResult,
  data () {
    return {
      dataTable: null,
      parser: new NetworksResultParser(this.normalizePath, this.getLocale()),
      type: "networks",
      target: "network"
    }
  },
  computed: {
    // network headers, 1st row
    networkColumnItems: function() {
      let displayOptions = this.getDisplayOptions();
      let columnKey = 'networkColumns';
      if (this.studyTypeSelection) {
        if (this.studyTypeSelection.study) {
          columnKey = 'networkColumnsIndividual';
        } else if(this.studyTypeSelection.harmonization) {
          columnKey = 'networkColumnsHarmonization';
        }
      }

      return (displayOptions[columnKey] || displayOptions.networkColumns)
        .filter(col => {
          if (col === 'type') {
            return this.withCollectedDatasets && this.withHarmonizedDatasets;
          } else if (col === 'individual') {
            return false;
          } else if (col === 'harmonization') {
            return false;
          } else if (col === 'studies' || col === 'initiatives') {
            return this.withStudies;
          } else if (col === 'datasets') {
            return this.withCollectedDatasets || this.withHarmonizedDatasets;
          } else if (col === 'variables') {
            return this.withCollectedDatasets || this.withHarmonizedDatasets;
          }
          return true;
        })
        .map(col => {
          return {
            name: col,
            rowspan: (['name', 'studies', 'initiatives'].includes(col) ? 2 : 1),
            colspan: (['name', 'studies', 'initiatives'].includes(col) ? 1 : 2)
          }
        });
    },
    // network headers, 2nd row
    networkColumnItems2: function() {
      let displayOptions = this.getDisplayOptions();
      let columnKey = 'networkColumns';
      if (this.studyTypeSelection) {
        if (this.studyTypeSelection.study) {
          columnKey = 'networkColumnsIndividual';
        } else if(this.studyTypeSelection.harmonization) {
          columnKey = 'networkColumnsHarmonization';
        }
      }

      const items2 = [];
      (displayOptions[columnKey] || displayOptions.networkColumns)
        .filter(col => {
          if (col === 'datasets') {
            return this.withCollectedDatasets || this.withHarmonizedDatasets;
          } else if (col === 'individual') {
            return this.withCollectedDatasets;
          } else if (col === 'harmonization') {
            return this.withHarmonizedDatasets;
          } else if (col === 'variables') {
            return this.withCollectedDatasets || this.withHarmonizedDatasets;
          }
          return false;
        })
        .forEach((col, id) => {
          if (['individual', 'harmonization'].includes(col)) {
            items2.push({id: id, name: (this.studyTypeSelection.harmonization ? 'initiatives' : 'studies'), title: ''});
            items2.push({id: id, name: (this.studyTypeSelection.harmonization ? 'protocols' : 'datasets'), title: ''});
            items2.push({id: id, name: 'variables', title: ''});
          } else if (col === 'datasets') {
            items2.push({ name: 'collected'});
            items2.push({ name: 'harmonized'});
          } else if (col === 'variables') {
            items2.push({ name: 'collected'});
            items2.push({ name: 'harmonized'});
          }
        });
      return items2;
    }
  },
  methods: {
    onAnchorClicked(event) {
      console.debug('Network onAnchorClicked');
      event.preventDefault();
      const anchor = $(event.target);
      const target = anchor.attr('data-target');
      const targetId = anchor.attr('data-target-id');
      const type = anchor.attr('data-type');
      const studyType = anchor.attr('data-study-type');

      const updates = [{target, query: new RQL.Query('in', ['Mica_network.id',targetId])}];

      if ("" !== studyType) {
        updates.push({target: 'study', query: new RQL.Query('in', ['Mica_study.className', studyType])});
      }

      this.getEventBus().$emit('query-type-updates-selection', {type: `${type}`, updates});
    }
  },
  mounted() {
    console.debug('Networks Result Mounted...');
    $('#vosr-networks-result').on('click', 'a.query-anchor', this.onAnchorClicked);
  }
}

const DatasetsResult = {
  template: `
  <div>
    <div class="row" v-show="showResult">
      <div class="col">
        <table id="vosr-datasets-result" class="table table-striped" width="100%">
          <thead>
            <tr>
              <th class="column-acronym" >{{ "acronym" | translate }}</th>
              <th v-for="(column, index) in datasetColumnNames" :key="index" :class="'column-' + column">{{ column | translate }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="parsedItem in parsed.data">
              <td v-for="cell in parsedItem" v-html="cell"></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
  `,
  name: 'datasets-result',
  extends: EntityResult,
  data () {
    return {
      parser: new DatasetsResultParser(this.normalizePath, this.getLocale()),
      type: "datasets",
      target: "dataset"
    }
  },
  props: {
    micaConfig: String
  },
  computed: {
    // dataset headers
    datasetColumnNames: function() {
      let displayOptions = this.getDisplayOptions();
      let columnKey = 'datasetColumns';
      if (this.studyTypeSelection) {
        if (this.studyTypeSelection.study) {
          columnKey = 'datasetColumnsIndividual';
        } else if(this.studyTypeSelection.harmonization) {
          columnKey = 'datasetColumnsHarmonization';
        }
      }

      return (displayOptions[columnKey] || displayOptions.datasetColumns)
        .filter(col => {
          if (col === 'type') {
            return this.withCollectedDatasets && this.withHarmonizedDatasets;
          } else if (col === 'networks') {
            return this.withNetworks;
          } else if (col === 'studies' || col === 'initiatives' || col === 'study' || col === 'initiative') { // studies and initiatives should be deprecated
            return this.withStudies;
          }
          return true;
        });
    },
  },
  methods: {
     onAnchorClicked(event) {
      event.preventDefault();
      const anchor = $(event.target);
      const query = new RQL.Query('in', ['Mica_dataset.id', `${anchor.attr('data-target-id')}`]);
      this.getEventBus().$emit(
        'query-type-update',
        {
          type: `${anchor.attr('data-type')}`,
          target: `${anchor.attr('data-target')}`,
          query
        });

      console.debug(`${anchor.attr('data-target')} - ${anchor.attr('data-target-id')}`);
    }
  },
  mounted() {
    console.debug('Datasets Result Mounted...');
    $('#vosr-datasets-result').on('click', 'a.query-anchor', this.onAnchorClicked);
  }
};

class RowPopupState {

  constructor() {
    this.element = null;
    this.model = null;
  }

  update(target, model) {
    this.element = "TR"  === target.tagName ? target : target.closest("TR");
    this.model = model;
  }

  reset() {
    this.element = null;
    this.model = null;
  }

  getElement() {
    return this.element;
  }

  getModel() {
    return this.model;
  }

}

const RowPopup = {
  template: `
  <div v-show="visible === true" class="coverage-row-popup" id="row-popup">
    <div class="coverage-row-popup-content">
        <table class="table table-striped table-condensed p-0 m-0">
          <tr>
            <th v-for="(value, index) in headers" v-bind:key="index">{{value}}</th>
          </tr>
          <tr>
            <td v-for="(value, index) in content" v-bind:key="index">{{value}}</td>
          </tr>
        </table>
    </div>
  </div>
  `,
  name: 'row-popup',
  props: {
    state: RowPopupState,
    typeSelection: Object
  },
  data() {
    return {
      container: null,
      element: null,
      scrollHandler: null,
      mouseMoveHandler: null,
      timeoutId: null,
      headersMap: {},
      headers: null,
      content: null,
      visible: true
    }
  },
  mounted() {
    this.container = document.querySelector("#coverage-table-container");
    this.element = document.querySelector("#row-popup");
    this.scrollHandler = this.onScroll.bind(this);
    this.mouseMoveHandler = this.onMouseMove.bind(this);

    const translate = (key) => Vue.filter('translate')(key);

    this.headersMap = {
      dceId: [
        translate('search.coverage-dce-cols.study'),
        translate('search.coverage-dce-cols.population'),
        translate('search.coverage-dce-cols.dce')
      ],
      datasetId: [translate('search.coverage-buckets.dataset')],
      studyId: [translate('search.coverage-buckets.study')],
      harmonization: [translate('search.coverage-dce-cols.harmonization')]
    };

  },
  methods: {
    initContent() {
      const model = this.state.getModel();
      let content = model.title.trim().split(/:/);
      this.content = this.typeSelection && this.typeSelection.harmonization ? [content[0]] : content;
      this.headers = this.typeSelection && this.typeSelection.harmonization ? this.headersMap['harmonization'] : this.headersMap[model.field].slice(0);
    },
    beforeDestroy() {
      clearTimeout(this.timeoutId);
      this.container.removeEventListener("scroll", this.scrollHandler);
      window.removeEventListener("mousemove", this.mouseMoveHandler);
    },
    onMouseMove(event) {
      const rect = this.element.getBoundingClientRect();
      const windowWidth = (window.innerWidth || document.documentElement.clientWidth);
      const windowHeight = (window.innerHeight || document.documentElement.clientHeight);
      const xInViewPort = windowWidth - rect.width - ROW_POPUP_MARGIN > event.clientX;
      const yInViewPort = windowHeight - rect.height - ROW_POPUP_MARGIN > event.clientY;
      this.element.style.left = (xInViewPort ? event.clientX  + ROW_POPUP_MARGIN : event.clientX - ROW_POPUP_MARGIN - rect.width) + "px";
      this.element.style.top = (yInViewPort ? event.clientY + ROW_POPUP_MARGIN : event.clientY - ROW_POPUP_MARGIN - rect.height) + "px";
    },
    onScroll() {
      this.visible =
        this.container.getBoundingClientRect().left > this.state.getElement().children[1].getBoundingClientRect().x;
    }
  },
  watch: {
    state: function() {
      if (this.state) {
        this.initContent();
      }

      this.$nextTick(() => {
        if (this.state) {
          this.container.addEventListener("scroll", this.scrollHandler);
          window.addEventListener("mousemove", this.mouseMoveHandler);
          this.onScroll();
        } else {
          this.container.removeEventListener("scroll", this.scrollHandler);
          window.removeEventListener("mousemove", this.mouseMoveHandler);
          this.content = null;
          this.headers = null;
        }
      });
    }
  }
}

const CoverageResult = {
  template: `
  <div>
    <div v-show="showResult">
      <div class="row">
        <div id="coverage-table-container" class="col table-responsive">
          <row-popup :type-selection="studyTypeSelection" :state="rowPopupState"></row-popup>
          <table v-if="table" id="vosr-coverage-result" class="table table-striped" width="100%">
            <thead>
              <tr>
                <th v-bind:rowspan="bucketStartsWithDce ? 1 : 2" v-bind:colspan="studyTypeSelection && studyTypeSelection.harmonization ? 3 : table.cols.colSpan">
                  <span v-if="!studyTypeSelection || !studyTypeSelection.harmonization">{{ (bucketName === 'dce' ? '' : ('coverage-buckets-' + bucketName)) | translate}}</span>
                  <span v-else>{{ 'coverage-buckets-harmonization' | translate}}</span>
                </th>
                <th v-for="(header, index) in table.vocabularyHeaders" v-bind:key="index" v-bind:colspan="header.termsCount">
                  <!-- TODO popover -->
                  <span>{{ header.entity.titles | localize-string }} </span>
                  <small>
                    <a href v-on:click="removeVocabulary($event, header)">
                      <i class="fa fa-times"></i>
                    </a>
                  </small>
                </th>
              </tr>
              <tr>
                <th v-if="bucketStartsWithDce" v-bind:colspan="studyTypeSelection && studyTypeSelection.harmonization ? 3 : 1">{{ (studyTypeSelection && studyTypeSelection.harmonization ? "coverage-buckets-harmonization" : "study") | translate }}</th>
                <th v-if="bucketStartsWithDce" v-show="!studyTypeSelection.harmonization">{{ "population" | translate }}</th>
                <th v-if="bucketStartsWithDce" v-show="!studyTypeSelection.harmonization">{{ "data-collection-event" | translate }}</th>

                <th v-for="(header, index) in table.termHeaders" v-bind:key="index">
                  <!-- TODO popover -->
                  <span>{{ header.entity.titles | localize-string }} </span>
                  <small>
                    <a ng-if="header.canRemove" href v-on:click="removeTerm($event, header)">
                      <i class="fa fa-times"></i>
                    </a>
                  </small>
                </th>
              </tr>
              <tr>
                <th v-bind:colspan="studyTypeSelection && studyTypeSelection.harmonization ? 3 : table.cols.colSpan"></th>
                <th v-for="(header, index) in table.termHeaders" v-bind:key="index" v-bind:title="header.entity.descriptions | localize-string">
                  <a href v-on:click="updateQuery($event, null, header, 'variables')">
                    <span>{{header.hits.toLocaleString()}}</span>
                  </a>
                </th>
              </tr>
            </thead>

            <tbody>
              <tr v-if="currentPage > 0">
                <td :colspan="table.termHeaders.length + (bucketStartsWithDce ? 3 : 1)">
                  <button type="button" class="btn btn-sm btn-secondary coverage-pager" @click="previous()"><i class="fas fa-chevron-up"></i></button>
                </td>
              </tr>

              <tr v-for="(row, rindex) in filteredRows" v-bind:key="rindex"
                v-show="table.termHeaders.length == row.hits.length"
                v-on:mouseover="onMouseOver($event, row)"
                v-on:mouseleave="onMouseLeave()">

                <td v-for="(col, cindex) in table.cols.ids[row.value]"
                  v-bind:key="cindex"
                  v-bind:colspan="cindex === 0 && studyTypeSelection.harmonization ? 3 : 1"
                  v-show="!(col.id === '-' && (isSingleStudyEnabled || studyTypeSelection.harmonization))">

                  <span v-show="col.id === '-'">-</span>
                  <a v-show="col.rowSpan !== 0  && col.id !== '-'" v-bind:title="col.description" v-bind:href="col.url">{{col.title}}</a>
                  <div style="text-align: center" v-show="col.start && bucketStartsWithDce">
                    <div>
                      <small class="help-block no-margin" v-show="col.end">
                        {{col.start}} {{'to' | translate }} {{col.end}}
                      </small>
                      <small class="help-block no-margin" v-show="!col.end">
                        {{col.start}}, {{'coverage-end-date-ongoing' | translate}}
                      </small>
                    </div>
                    <div class="progress no-margin" style="height: 0.5em">
                      <div class="progress-bar progress-bar-transparent progress-bar-thin" role="progressbar" :aria-valuenow="col.start" :aria-valuemin="col.min"
                        :aria-valuemax="col.start" v-bind:style="{'width': col.progressStart + '%'}">
                      </div>
                      <div v-bind:class="'progress-bar progress-bar-' + col.progressClass" role="progressbar" v-bind:aria-valuenow="col.current" v-bind:aria-valuemin="col.start"
                        v-bind:aria-valuemax="col.end ? col.end : col.current" v-bind:style="{'width': col.progress + '%'}">
                      </div>
                    </div>
                  </div>
                </td>

                <td v-for="(h, hindex) in table.termHeaders" v-bind:key="'h'+hindex">
                  <a href="" v-on:click="updateQuery($event, row.value, h, 'variables')">
                    <span class="badge badge-primary" v-show="row.hitsTitles[hindex]">{{row.hitsTitles[hindex]}}</span>
                  </a>
                  <span v-show="!row.hitsTitles[hindex]">0</span>
                </td>
              </tr>

              <tr v-if="currentPage < (pages - 1)">
                <td :colspan="table.termHeaders.length + (bucketStartsWithDce ? 3 : 1)">
                  <button type="button" class="btn btn-sm btn-secondary coverage-pager" @click="next()"><i class="fas fa-chevron-down"></i></button>
                </td>
              </tr>
            </tbody>

          </table>
        </div>
      </div>
    </div>
  </div>
  `,
  name: 'coverage-result',
  components: {
    RowPopup
  },
  data() {
    return {
      studyTypeSelection: {all: true},
      isSingleStudyEnabled: false,
      dataTable: null,
      ajaxCallback: null,
      parser: new CoverageResultParser(this.getMicaConfig(), this.getLocale(), this.normalizePath),
      table: null,
      vocabulariesTermsMap: null,
      bucketStartsWithDce: false,
      showResult: false,
      filteredRows: [],
      rowPopupState: null,
      popupState: new RowPopupState(),
      pages: 0,
      currentPage: 0
    };
  },
  methods: {
    getEventBus: () => EventBus,
    getMicaConfig: () => Mica.config,
    getLocale: () => Mica.locale,
    getDisplayOptions: () => Mica.display,
    normalizePath: (path) => {
      return contextPath + path;
    },
    localize: (entries) => StringLocalizer.localize(entries),
    registerDataTable(tableId, options) {
      const mergedOptions = Object.assign(options, DataTableDefaults);
      mergedOptions.language = {
        url: contextPath + '/assets/i18n/mlstr-datatables.' + Mica.locale + '.json'
      };
      const dTable = $('#' + tableId).DataTable(mergedOptions);
      dTable.on('draw.dt', function() {
        // bs tooltip
        $('[data-toggle="tooltip"]').tooltip();
      });

      // checkboxes only for variables
      if ('vosr-variables-result' === tableId) {
        initSelectDataTable(dTable, options);
      }

      return dTable;
    },
    onResults(payload) {
      console.debug('On coverage result');

      // Header initialization
      this.showResult = (payload.response.rows || []).length > 0;
      if (!this.showResult) return;
      this.isSingleStudyEnabled = this.getMicaConfig().isSingleStudyEnabled;
      this.studyTypeSelection = payload.studyTypeSelection;
      this.rows = payload.response.rows;

      this.bucket = payload.bucket;
      this.bucketName = this.bucket.replace(/Id$/,"");
      this.bucketStartsWithDce = payload.bucket.startsWith('dce')
      let headersData = this.parser.parseHeaders(payload.bucket, payload.response);
      this.table = headersData.table;
      this.vocabulariesTermsMap = headersData.vocabulariesTermsMap;

      if (this.dataTable) {
        this.dataTable.destroy();
        this.dataTable = null;
      }

      this.currentPage = -1;
      this.pages = Math.ceil((this.rows || []).length / COVERAGE_PAGE_SIZE);

      this.next();
    },
    removeVocabulary(event, vocabulary) {
      console.debug(`removeVocabulary ${vocabulary}`);
      event.preventDefault();
      this.getEventBus().$emit('query-type-delete', {target: 'variable', query: new RQL.Query('exists', [`${vocabulary.taxonomyName}.${vocabulary.entity.name}`])});
    },
    removeTerm(event, term) {
      console.debug(`removeVocabulary ${term}`);
      event.preventDefault();

      const argsToKeep = this.table.termHeaders.filter(t => t.vocabularyName === term.vocabularyName && t.entity.name !== term.entity.name).map(term => term.entity.name);
      if (argsToKeep.length === 0) {
        this.getEventBus().$emit('query-type-delete', {target: 'variable', query: new RQL.Query('exists', [`${term.taxonomyName}.${term.vocabularyName}`])});
      } else {
        this.getEventBus().$emit('query-type-update', {target: 'variable', query: new RQL.Query('in', [`${term.taxonomyName}.${term.vocabularyName}`, argsToKeep])});
      }
    },
    onMouseOver(event, row) {
      this.popupState.update(event.target, row);
      this.rowPopupState = this.popupState;
    },
    onMouseLeave() {
      this.popupState.reset();
      this.rowPopupState = null;
    },
    next() {
      if (this.currentPage < this.pages) {
        this.currentPage++;
        this.filteredRows = this.getPage(this.currentPage);

        window.scrollTo(0, 0);
      }
    },
    previous() {
      if (this.currentPage > 0) {
        this.currentPage--;
        this.filteredRows = this.getPage(this.currentPage);

        window.scrollTo(0, 0);
      }
    },
    getPage(currentPage) {
      return (this.rows || []).slice(currentPage * COVERAGE_PAGE_SIZE, (currentPage + 1) * COVERAGE_PAGE_SIZE);
    },
    updateQuery(event, id, term, type) {
      console.debug(`Id: ${id} Term: ${term} Type: ${type}`);

      event.preventDefault();
      const updates = [{
        target: 'variable',
        query: new RQL.Query('in', [`${term.taxonomyName}.${term.vocabularyName}`, term.entity.name]),
        operator: 'or',
        reduceKey: `${term.taxonomyName}.${term.vocabularyName}`
      }];

      if (id !== null) {
        const bucketTargetMap = {
          studyId: {target: 'study', queryKey: 'Mica_study.id'},
          datasetId: {target: 'dataset', queryKey: 'Mica_dataset.id'},
          dceId: {target: 'variable', queryKey: 'Mica_variable.dceId'}
        };
        const targetData = bucketTargetMap[this.bucket];

        updates.push({target: targetData.target, query: new RQL.Query('in', [targetData.queryKey,id])});
      }

      this.getEventBus().$emit('query-type-updates-selection', {display: 'lists', type: `${type}`, updates});

    }
  },
  mounted() {
    console.debug('Mounted CoverageResult');
    this.getEventBus().register('coverage-results',this.onResults.bind(this));
  },
  beforeDestroy() {
    this.dataTable = null;
    this.getEventBus().unregister('coverage-results', this.onResults);
  }
}

Vue.component(GraphicsResult.name, GraphicsResult);
Vue.component(VariablesResult.name, VariablesResult);
Vue.component(StudiesResult.name, StudiesResult);
Vue.component(NetworksResult.name, NetworksResult);
Vue.component(DatasetsResult.name, DatasetsResult);
Vue.component(CoverageResult.name, CoverageResult);
