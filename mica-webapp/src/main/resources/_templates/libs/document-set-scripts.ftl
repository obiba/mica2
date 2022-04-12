<!-- Script for the cart and the documents list pages -->
<script src="${assetsPath}/js/mica-tables.js"></script>

<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/js/vue-mica-search/libs/result-parsers.js"></script>
<script src="${assetsPath}/js/vue-mica-search/result.js"></script>
<script src="${assetsPath}/js/mica-query.js"></script>

<script>
  const Mica = {
    config: ${configJson!"{}"},
    locale: "${.lang}",
    defaultLocale: "${defaultLang}"
  };

  Mica.setIsLocked = ${set.locked?c};
  Mica.isAdministrator = ${isAdministrator?c};

  Mica.tr = {
    "variables": "<@message "variables"/>",
    "variable": "<@message "variable"/>",
    "datasets": "<@message "datasets"/>",
    "studies": "<@message "studies"/>",
    "networks": "<@message "networks"/>",
    "network": "<@message "network"/>",
    "initiatives": "<@message "initiatives"/>",
    "initiative": "<@message "initiative"/>",
    "protocol": "<@message "protocol"/>",
    "protocols": "<@message "protocols"/>",
    "name": "<@message "name"/>",
    "label": "<@message "label"/>",
    "annotations": "<@message "annotations"/>",
    "study": "<@message "study"/>",
    "dataset": "<@message "dataset"/>",
    "population": "<@message "population"/>",
    "data-collection-event": "<@message "data-collection-event"/>",
    "dce": "<@message "search.study.dce-name"/>",
    "acronym": "<@message "acronym"/>",
    "valueType": "<@message "value-type"/>",
    "text-type": "<@message "text-type"/>",
    "integer-type": "<@message "integer-type"/>",
    "decimal-type": "<@message "decimal-type"/>",
    "boolean-type": "<@message "boolean-type"/>",
    "binary-type": "<@message "binary-type"/>",
    "date-type": "<@message "date-type"/>",
    "datetime-type": "<@message "datetime-type"/>",
    "point-type": "<@message "point-type"/>",
    "linestring-type": "<@message "linestring-type"/>",
    "polygon-type": "<@message "polygon-type"/>",
    "locale-type": "<@message "locale-type"/>",
    "type": "<@message "type"/>",
    "study-design": "<@message "study-design"/>",
    "data-sources-available": "<@message "data-sources-available"/>",
    "participants": "<@message "participants"/>",
    "individual": "<@message "individual"/>",
    "harmonization": "<@message "harmonization"/>",
    "collected": "<@message "collected"/>",
    "harmonized": "<@message "harmonized"/>",
    "dataschema": "<@message "Dataschema"/>",
    "no-variable-found": "<@message "no-variable-found"/>",
    "no-dataset-found": "<@message "no-dataset-found"/>",
    "no-study-found": "<@message "no-study-found"/>",
    "no-network-found": "<@message "no-network-found"/>",
    "no-variable-added": "<@message "sets.cart.no-variable-added"/>",
    "variables-added-to-cart": "<@message "variables-added-to-cart"/>",
    "no-variable-added-set": "<@message "sets.set.no-variable-added"/>",
    "variables-added-to-set": "<@message "variables-added-to-set"/>",
    "no-study-added": "<@message "sets.cart.no-study-added"/>",
    "studies-added-to-cart": "<@message "studies-added-to-cart"/>",
    "no-network-added": "<@message "sets.cart.no-network-added"/>",
    "networks-added-to-cart": "<@message "networks-added-to-cart"/>",
    "data-access-request": "<@message "data-access-request"/>"
  };

  Mica.querySettings = {
    variable: {
      fields: ['${searchVariableFields?join("', '")}'],
      sortFields: ['${searchVariableSortFields?join("', '")}']
    },
    dataset: {
      fields: ['${searchDatasetFields?join("', '")}'],
      sortFields: ['${searchDatasetSortFields?join("', '")}']
    },
    study: {
      fields: ['${searchStudyFields?join("', '")}'],
      sortFields: ['${searchStudySortFields?join("', '")}']
    },
    network: {
      fields: ['${searchNetworkFields?join("', '")}'],
      sortFields: ['${searchNetworkSortFields?join("', '")}']
    }
  };

  Mica.display = {
    variableColumns: ['${searchVariableColumns?join("', '")}'],
    variableColumnsHarmonization: ['${searchVariableColumnsHarmonization?join("', '")}'],
    variableColumnsIndividual: ['${searchVariableColumnsIndividual?join("', '")}'],

    datasetColumns: ['${searchDatasetColumns?join("', '")}'],
    datasetColumnsHarmonization: ['${searchDatasetColumnsHarmonization?join("', '")}'],
    datasetColumnsIndividual: ['${searchDatasetColumnsIndividual?join("', '")}'],

    studyColumns: ['${searchStudyColumns?join("', '")}'],
    studyColumnsHarmonization: ['${searchStudyColumnsHarmonization?join("', '")}'],
    studyColumnsIndividual: ['${searchStudyColumnsIndividual?join("', '")}'],

    networkColumns: ['${searchNetworkColumns?join("', '")}'],
    networkColumnsHarmonization: ['${searchNetworkColumnsHarmonization?join("', '")}'],
    networkColumnsIndividual: ['${searchNetworkColumnsIndividual?join("', '")}'],

    searchCriteriaMenus: ['${searchCriteriaMenus?join("', '")}']
  };

  Mica.defaultSearchMode = "${defaultSearchMode}";

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

  const taxonomyTitleFinder  = new TaxonomyTitleFinder(); // important initialisation

  // clear any previous selections from local storage
  const variablesCartStorage = new MicaSetStorage('cart');
  const studiesCartStorage = new MicaSetStorage('scart');
  const networksCartStorage = new MicaSetStorage('ncart');

  // cart
  <#if variablesCartEnabled>
  const onVariablesCartAdd = function(id) {
    VariablesSetService.addQueryToCart('variable(in(Mica_variable.sets,' + id + '),limit(0,10000),fields(variableType))', function(cart, oldCart) {
      VariablesSetService.showCount('#cart-count', cart, '${.lang}');
      if (cart.count === oldCart.count) {
        MicaService.toastInfo("<@message "sets.cart.no-variable-added"/>");
      } else {
        MicaService.toastSuccess("<@message "variables-added-to-cart"/>".replace('{0}', (cart.count - oldCart.count).toLocaleString('${.lang}')));
      }
    });
  };
  </#if>

  $(function () {
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

    // base documents data table options
    const dataTableOpts = {
      paging: true,
      lengthMenu: [10, 20, 50, 100],
      pageLength: 20,
      lengthChange: true,
      searching: false,
      ordering: false,
      autoWidth: true,
      language: {
        url: "${assetsPath}/i18n/datatables.${.lang}.json"
      },
      processing: true,
      serverSide: true,
      columnDefs: [{ // the checkbox
        orderable: false,
        className: 'select-checkbox',
        targets: 0
      }, { // the ID
          visible: false,
          searchable: false,
          targets: 1
      }],
      select: {
        style: 'multi',
        selector: 'td:first-child',
        info: false
      },
      fixedHeader: true,
      dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'table-responsive col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>",
      info: true
    };

    new Vue({
      el: "#query-vue-container",
      data() {
        return {
          from: 0,
          size: DEFAULT_PAGE_SIZE,
          variableSelections: [],
          hasResult: false,
          pagination: null,
          pageSizeSelector: null,
          studyClassName: 'Study',
          hasCheckboxes: !Mica.setIsLocked || Mica.isAdministrator
        };
      },
      methods: {
        onResult(payload) {
          const data = payload.response;

          if (data) {
            let dto = 'variableResultDto';

            this.pagination.update((data[dto] || {totalHits: 0}).totalHits, this.size, (this.from/this.size)+1);
            this.hasResult = (data[dto] || {totalHits: 0}).totalHits > 0;
            this.pageSizeSelector.update(this.size);
          }
        },
        onSelectionChanged(payload) {
          variablesCartStorage.deselectAll();
          this.variableSelections = payload.selections || [];

          variablesCartStorage.selectAll(this.variableSelections);

          const count = variablesCartStorage.getSelections().length;
          if (count === 0) {
            $('.selection-count').hide();
            $('#delete-all-message').show();
            $('#delete-selected-message').hide();
          } else {
            $('.selection-count').text(count.toLocaleString('${.lang}')).show();
            $('#delete-all-message').hide();
            $('#delete-selected-message').show();
          }       
        },
        onPagination(data) {
          this.from = data.from;
          $('#loadingSet').show();
          this.doQuery();
        },
        onPageSizeChanged(data) {
          this.size = data.size;
          $('#loadingSet').show();
          this.doQuery();
        },
        onStudyClassNameChange() {
          $('#loadingSet').show();
          this.doQuery();
        },
        doQuery() {
          this.hasResult = false;

          let setId = '${set.id}';
          let tree = new RQL.QueryTree();

          let targetQuery = new RQL.Query(TARGETS.VARIABLE);
          tree.addQuery(null, targetQuery);

          tree.addQuery(targetQuery, new RQL.Query('in', ['Mica_variable.sets', setId]));

          tree.addQuery(targetQuery, new RQL.Query('limit', [this.from, this.size]));
          tree.addQuery(targetQuery, new RQL.Query('fields', ['${searchVariableFields?join("', '")}']));
          tree.addQuery(targetQuery, new RQL.Query('sort', ['${searchVariableSortFields?join("', '")}']));

          let studyTargetQuery = new RQL.Query(TARGETS.STUDY);
          tree.addQuery(null, studyTargetQuery);
          tree.addQuery(studyTargetQuery, new RQL.Query('in', ['Mica_study.className', this.studyClassName]));

          tree.addQuery(null, new RQL.Query('locale', ['${.lang}']));

          let url = '/ws/variables/_rql?query=' + tree.serialize();

          axios
            .get(MicaService.normalizeUrl(url))
            .then(response => {
              $('#loadingSet').hide();

              EventBus.$emit('variables-results', {
                studyTypeSelection: {all: false, study: this.studyClassName === 'Study', harmonization: this.studyClassName === 'HarmonizationStudy'},
                type: 'variables',
                response: response.data,
                from: this.from,
                size: this.size
              });
            });
        }
      },
      beforeMount() {
        EventBus.register("variables-results", this.onResult.bind(this));
      },
      mounted() {
        this.pagination = new OBiBaPagination('obiba-pagination-top', true, this.onPagination),
        this.pageSizeSelector = new OBiBaPageSizeSelector('obiba-page-size-selector-top', DEFAULT_PAGE_SIZES, DEFAULT_PAGE_SIZE, this.onPageSizeChanged);

        EventBus.register('variables-selections-updated', this.onSelectionChanged.bind(this));

        let url = '/ws/taxonomies/_filter?target' + TARGETS.VARIABLE;

        axios.get(MicaService.normalizeUrl(url)).
          then(response => {
            let taxonomies = [];
            console.log('taxonomies', response.data)

            if (Array.isArray(response.data)) {
              for (let taxo of response.data) {
                taxonomies[taxo.name] = taxo;
              }

              taxonomyTitleFinder.initialize(taxonomies);

              Vue.filter("taxonomy-title", (input) => {
                const [taxonomy, vocabulary, term] = input.split(/\./);
                return  taxonomyTitleFinder.title(taxonomy, vocabulary, term) || input;
              });
            }
          });

        this.doQuery();
      },
      beforeDestory() {
        EventBus.unregister("variables-results", this.onResult.bind(this));
      }
    });

    <#if studiesSet??>
    // studies set
    const studyTypeLabels = {
      'individual-study': "<@message "search.study.individual"/>",
      'harmonization-study': "<@message "search.study.harmonization"/>"
    }
    const studiesDataTableOpts = {...dataTableOpts};
    studiesDataTableOpts.ajax = function(data, callback) {
      StudiesSetService.search('${studiesSet.id}', data.start, data.length, function(response) {
        $('#loadingStudiesSet').hide();
        $('#studiesSetTable').show();
        let result = undefined;
        let rows = [];
        if (response.studyResultDto && response.studyResultDto['obiba.mica.StudyResultDto.result']) {
          result = response.studyResultDto;
          const summaries = result['obiba.mica.StudyResultDto.result'].summaries;
          for (const i in summaries) {
            let row = [];
            const summary = summaries[i];
            // checkbox
            row.push('<i class="far fa-square"></i>');
            // ID
            row.push(summary.id);
            row.push('<a href="${contextPath}/study/' + summary.id + '">' + LocalizedValues.forLang(summary.acronym, '${.lang}') + '</a>');
            row.push(LocalizedValues.forLang(summary.name, '${.lang}'));
            row.push(studyTypeLabels[summary.studyResourcePath]);
            rows.push(row);
          }
        }
        callback({
          data: rows,
          recordsTotal: result ? result.totalHits : 0,
          recordsFiltered: result ? result.totalHits : 0
        });
      });
    };
    studiesDataTableOpts.columnDefs = [{ // the checkbox
      orderable: false,
      className: 'studies-select-checkbox',
      targets: 0
    }, { // the ID
      visible: false,
      searchable: false,
      targets: 1
    }];
    initSelectDataTable($("#studiesSetTable").DataTable(studiesDataTableOpts), {
      className: 'studies-select-checkbox',
      isSelected: function(id) {
        return studiesCartStorage.selected(id);
      },
      onSelectionChanged: function (ids, selected) {
        if (selected) {
          studiesCartStorage.selectAll(ids);
        } else {
          studiesCartStorage.deselectAll(ids);
        }
        const count = studiesCartStorage.getSelections().length;
        if (count === 0) {
          $('.studies-selection-count').hide();
          $('#delete-all-studies-message').show();
          $('#delete-selected-studies-message').hide();
        } else {
          $('.studies-selection-count').text(count.toLocaleString('${.lang}')).show();
          $('#delete-all-studies-message').hide();
          $('#delete-selected-studies-message').show();
        }
      }
    });
    $('#studiesSetTable').css('width', '100%').hide();
    </#if>

    <#if networksSet??>
    // networks set
    const networksDataTableOpts = {...dataTableOpts};
    networksDataTableOpts.ajax = function(data, callback) {
      NetworksSetService.search('${networksSet.id}', data.start, data.length, function(response) {
        $('#loadingNetworksSet').hide();
        $('#networksSetTable').show();
        let result = undefined;
        let rows = [];
        if (response.networkResultDto && response.networkResultDto['obiba.mica.NetworkResultDto.result']) {
          result = response.networkResultDto;
          const summaries = result['obiba.mica.NetworkResultDto.result'].networks;
          for (const i in summaries) {
            let row = [];
            const summary = summaries[i];
            // checkbox
            row.push('<i class="far fa-square"></i>');
            // ID
            row.push(summary.id);
            row.push('<a href="${contextPath}/network/' + summary.id + '">' + LocalizedValues.forLang(summary.acronym, '${.lang}') + '</a>');
            row.push(LocalizedValues.forLang(summary.name, '${.lang}'));
            rows.push(row);
          }
        }
        callback({
          data: rows,
          recordsTotal: result ? result.totalHits : 0,
          recordsFiltered: result ? result.totalHits : 0
        });
      });
    };
    initSelectDataTable($("#networksSetTable").DataTable(networksDataTableOpts), {
      isSelected: function(id) {
        return networksCartStorage.selected(id);
      },
      onSelectionChanged: function (ids, selected) {
        if (selected) {
          networksCartStorage.selectAll(ids);
        } else {
          networksCartStorage.deselectAll(ids);
        }
        const count = networksCartStorage.getSelections().length;
        if (count === 0) {
          $('.networks-selection-count').hide();
          $('#delete-all-networks-message').show();
          $('#delete-selected-networks-message').hide();
        } else {
          $('.networks-selection-count').text(count.toLocaleString('${.lang}')).show();
          $('#delete-all-networks-message').hide();
          $('#delete-selected-networks-message').show();
        }
      }
    });
    $('#networksSetTable').css('width', '100%').hide();
    </#if>

  });
</script>
