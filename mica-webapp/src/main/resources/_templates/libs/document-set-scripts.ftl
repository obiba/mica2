<!-- Script for the cart and the documents list pages -->
<script src="${assetsPath}/js/mica-tables.js"></script>

<script src="${assetsPath}/libs/node_modules/@vue/compat/dist/vue.global.js"></script>
<script>
  // Configure Vue 3 compat mode to allow Vue 2 APIs
  if (Vue && Vue.configureCompat) {
    Vue.configureCompat({
      MODE: 2  // Use Vue 2 compatibility mode
    });
  }

  Vue.mixin(MicaFilters.asMixin());
</script>
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
  Mica.useOnlyOpalTaxonomiesForCoverage = ${useOnlyOpalTaxonomiesForCoverage?c};

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
    "data-access-request": "<@message "data-access-request"/>",
    "count-warning": "<@message "count-warning"/>"
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
    variableColumns: ["label+description", "valueType", "annotations", "type", "study", "population", "data-collection-event", "dataset"],
    variableColumnsHarmonization: ["label+description", "valueType", "annotations", "initiative", "protocol"],
    variableColumnsIndividual: ["label+description", "valueType", "annotations", "study", "population", "data-collection-event", "dataset"],

    studyColumns: ["name", "type", "study-design", "data-sources-available", "participants"],
    studyColumnsHarmonization: ["name"],
    studyColumnsIndividual: ["name", "study-design", "data-sources-available", "participants"],

    networkColumns: ["name"],
    networkColumnsHarmonization: ["name"],
    networkColumnsIndividual: ["name"]
  };

  Mica.defaultSearchMode = "${defaultSearchMode}";

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

  const totalCounts = {
    <#if variablesCartEnabled>
    variablesCount: parseInt("${sets.variablesCart.count}".replace(',', '')),
    </#if>

    <#if studiesCartEnabled>
    studiesCount: parseInt("${sets.studiesCart.count}".replace(',', '')),
    </#if>

    <#if networksCartEnabled>
    networksCount: parseInt("${sets.networksCart.count}".replace(',', '')),
    </#if>

    <#if rc.requestUri?starts_with("/list/")>
    currentSetIdentifiersCount: parseInt("${set.identifiers?size}".replace(',', '')),
    </#if>
  };


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
          studySelections: [],
          networkSelections: [],
          hasResult: false,
          pagination: null,
          pageSizeSelector: null,
          studyClassName: Mica.defaultSearchMode,
          hasCheckboxes: !Mica.setIsLocked || Mica.isAdministrator,
          individualSubCount: "0",
          harmonizationSubCount: "0",
          countWarning: false
        };
      },
      methods: {
        onResult(payload) {
          this.countWarning = false;
          const data = payload.response;

          if (data) {
            let dto = 'variableResultDto';
            let totalCount = 0;

            switch (payload.type) {
              case TYPES.VARIABLES:
                dto = 'variableResultDto';
                totalCount = (window.location.pathname === '/cart' ? totalCounts.variablesCount : totalCounts.currentSetIdentifiersCount) || 0;
                break;
              case TYPES.STUDIES:
                dto = 'studyResultDto';
                totalCount = totalCounts.studiesCount || 0;
                break;
              case TYPES.NETWORKS:
                dto = 'networkResultDto';
                totalCount = totalCounts.networksCount || 0;
                break;
            }

            let result = (data[dto] || {totalHits: 0});

            let badTotals = totalCount - result.totalHits < 0;

            if (this.studyClassName === 'Study') {
              this.individualSubCount = totalCount === 0 ? totalCount : (result.totalHits).toLocaleString(Mica.locale);
              this.harmonizationSubCount = totalCount === 0 ? totalCount : (Math.max(totalCount - result.totalHits, 0)).toLocaleString(Mica.locale);
            } else {
              this.harmonizationSubCount = totalCount === 0 ? totalCount : (result.totalHits).toLocaleString(Mica.locale);
              this.individualSubCount = totalCount === 0 ? totalCount : (Math.max(totalCount - result.totalHits, 0)).toLocaleString(Mica.locale);
            }

            this.pagination.update(result.totalHits, this.size, (this.from/this.size)+1);
            this.hasResult = result.totalHits > 0;
            this.pageSizeSelector.update(this.size);

            if (badTotals) {
              MicaService.toastWarning("<@message "count-warning"/>");

              this.countWarning = true;
              this.verifyTotalCount();
            }
          }
        },
        onSelectionChanged(payload) {
          let count = 0;

          let defaultType;
          if (Mica.config.isCartEnabled && (Mica.config.isCollectedDatasetEnabled || Mica.config.isHarmonizedDatasetEnabled)) {
            defaultType = TYPES.VARIABLES
          } else if (Mica.config.isStudiesCartEnabled && !Mica.config.isSingleStudyEnabled) {
            defaultType = TYPES.STUDIES;
          } else if (Mica.config.isNetworksCartEnabled && Mica.config.isNetworkEnabled && !Mica.config.isSingleNetworkEnabled) {
            defaultType = TYPES.NETWORKS;
          }

          let type = this.currentWindowLocationSearch()['type'] || defaultType;
          let selectionCountClassName = '.selection-count';

          switch (type) {
            case TYPES.VARIABLES:
              variablesCartStorage.deselectAll();
              this.variableSelections = payload.selections || [];
              variablesCartStorage.selectAll(this.variableSelections);
              count = variablesCartStorage.getSelections().length;
              break;
            case TYPES.STUDIES:
              studiesCartStorage.deselectAll();
              this.studySelections = payload.selections || [];
              studiesCartStorage.selectAll(this.studySelections);
              count = studiesCartStorage.getSelections().length;

              selectionCountClassName = '.studies-selection-count';
              break;
            case TYPES.NETWORKS:
              networksCartStorage.deselectAll();
              this.networkSelections = payload.selections || [];
              networksCartStorage.selectAll(this.networkSelections);
              count = networksCartStorage.getSelections().length;

              selectionCountClassName = '.networks-selection-count';
              break;
          }

          if (count === 0) {
            $(selectionCountClassName).hide();
            $('#delete-all-message').show();
            $('#delete-selected-message').hide();
          } else {
            $(selectionCountClassName).text(count.toLocaleString('${.lang}')).show();
            $('#delete-all-message').hide();
            $('#delete-selected-message').show();
          }
        },
        onTabChanged(tab) {
          this.size = DEFAULT_PAGE_SIZE;
          this.from = 0;
          this.doQuery(tab);
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
        onStudyClassNameChange(newClassName) {
          this.studyClassName = newClassName;
          this.from = 0;

          $('#loadingSet').show();
          this.doQuery();
        },
        currentWindowLocationSearch() {
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
        },
        convertNumber(num, locale) {
          const { format } = new Intl.NumberFormat(locale);
          const [, decimalSign] = /^0(.)1$/.exec(format(0.1));
          return +num
          .replace(new RegExp('[^' + decimalSign + '\\d]', 'g'), '')
          .replace(decimalSign, '.');
        },
        verifyTotalCount() {
          let tab = this.currentWindowLocationSearch()['type'];

          let setId = '${set.id}';

          <#if studiesSet??>
          let studySetId = '${studiesSet.id}';
          <#else>
          let studySetId = '';
          </#if>

          <#if networksSet??>
          let networkSetId = '${networksSet.id}';
          <#else>
          let networkSetId = '';
          </#if>

          let totalVerificationTree = null;
          let resultDto = 'variableResultDto';
          let totalCount = 0;

          if (!tab || tab === 'variables') {
            totalVerificationTree = new RQL.QueryTree();
            let targetQuery = new RQL.Query(TARGETS.VARIABLE);
            totalVerificationTree.addQuery(null, targetQuery);

            totalVerificationTree.addQuery(targetQuery, new RQL.Query('in', ['Mica_variable.sets', setId]));
            totalVerificationTree.addQuery(targetQuery, new RQL.Query('limit', [0, 0]));

            totalVerificationTree.addQuery(null, new RQL.Query('locale', ['${.lang}']));

            resultDto = 'variableResultDto';
            totalCount = (window.location.pathname === '/cart' ? totalCounts.variablesCount : totalCounts.currentSetIdentifiersCount) || 0;
          } else if (tab && tab === 'studies' && studySetId) {
            totalVerificationTree = new RQL.QueryTree();
            let studyTargetQuery = new RQL.Query(TARGETS.STUDY);
            totalVerificationTree.addQuery(null, studyTargetQuery);

            totalVerificationTree.addQuery(studyTargetQuery, new RQL.Query('in', ['Mica_study.sets', studySetId]));
            totalVerificationTree.addQuery(studyTargetQuery, new RQL.Query('limit', [0, 0]));

            totalVerificationTree.addQuery(null, new RQL.Query('locale', ['${.lang}']));

            resultDto = 'studyResultDto';
            totalCount = totalCounts.studiesCount || 0;
          } else if (tab && tab === 'networks' && networkSetId) {
            totalVerificationTree = new RQL.QueryTree();
            let networkTargetQuery = new RQL.Query(TARGETS.NETWORK);
            totalVerificationTree.addQuery(null, networkTargetQuery);

            totalVerificationTree.addQuery(networkTargetQuery, new RQL.Query('in', ['Mica_network.sets', networkSetId]));
            totalVerificationTree.addQuery(networkTargetQuery, new RQL.Query('limit', [0, 0]));

            totalVerificationTree.addQuery(null, new RQL.Query('locale', ['${.lang}']));

            resultDto = 'networkResultDto';
            totalCount = totalCounts.networksCount || 0;
          }

          if (totalVerificationTree) {
            let url = '/ws/' + (!tab ? 'variables' : tab) + '/_rql?query=' + totalVerificationTree.serialize();
            axios.get(MicaService.normalizeUrl(url)).then(function (response){

              if (response.data) {
                let result = (response.data[resultDto] || {totalHits: 0});

                if (result.totalHits !== totalCount && convertedIndividualCount + convertedHarmoCount !== totalCount) {
                  this.countWarning = true;
                } else {
                  this.countWarning = false;
                }

                let convertedIndividualCount = this.convertNumber(this.individualSubCount, '${.lang}');
                let convertedHarmoCount = this.convertNumber(this.harmonizationSubCount, '${.lang}');

                if (this.studyClassName === 'Study') {
                  this.harmonizationSubCount = (Math.max(result.totalHits - convertedIndividualCount, 0)).toLocaleString(Mica.locale);
                } else {
                  this.individualSubCount = (Math.max(result.totalHits - convertedHarmoCount, 0)).toLocaleString(Mica.locale);
                }
              }

            }.bind(this));
          }
        },
        doQuery(tab) {
          tab = tab ? tab : this.currentWindowLocationSearch()['type'];

          if (!tab) {
            if (Mica.config.isCartEnabled && (Mica.config.isCollectedDatasetEnabled || Mica.config.isHarmonizedDatasetEnabled)) {
              tab = TYPES.VARIABLES
            } else if (Mica.config.isStudiesCartEnabled && !Mica.config.isSingleStudyEnabled) {
              tab = TYPES.STUDIES;
            } else if (Mica.config.isNetworksCartEnabled && Mica.config.isNetworkEnabled && !Mica.config.isSingleNetworkEnabled) {
              tab = TYPES.NETWORKS;
            }
          }

          let studyTypeSelection = {all: false, study: this.studyClassName === 'Study', harmonization: this.studyClassName === 'HarmonizationStudy'};

          this.hasResult = false;

          let setId = '${set.id}';

          <#if studiesSet??>
          let studySetId = '${studiesSet.id}';
          <#else>
          let studySetId = '';
          </#if>

          <#if networksSet??>
          let networkSetId = '${networksSet.id}';
          <#else>
          let networkSetId = '';
          </#if>

          let resultEventName = 'variables-results';
          let resultType = 'variables';

          let tree = new RQL.QueryTree();

          if (!tab || tab === 'variables') {
            if (!Mica.config.isCollectedDatasetEnabled) {
              this.studyClassName = 'HarmonizationStudy';
              studyTypeSelection = {all: false, study: this.studyClassName === 'Study', harmonization: this.studyClassName === 'HarmonizationStudy'};
            }

            let targetQuery = new RQL.Query(TARGETS.VARIABLE);
            tree.addQuery(null, targetQuery);

            tree.addQuery(targetQuery, new RQL.Query('in', ['Mica_variable.sets', setId]));

            tree.addQuery(targetQuery, new RQL.Query('limit', [this.from, this.size]));
            tree.addQuery(targetQuery, new RQL.Query('fields', ['${searchVariableFields?join("', '")}']));
            tree.addQuery(targetQuery, new RQL.Query('sort', ['${searchVariableSortFields?join("', '")}']));

            let studyTargetQuery = new RQL.Query(TARGETS.STUDY);
            tree.addQuery(null, studyTargetQuery);
            tree.addQuery(studyTargetQuery, new RQL.Query('in', ['Mica_study.className', this.studyClassName]));
          } else if (tab && tab === 'studies' && studySetId) {
            resultEventName = 'studies-results';
            resultType = 'studies';

            let studyTargetQuery = new RQL.Query(TARGETS.STUDY);
            tree.addQuery(null, studyTargetQuery);

            tree.addQuery(studyTargetQuery, new RQL.Query('and', [new RQL.Query('in', ['Mica_study.className', this.studyClassName]), new RQL.Query('in', ['Mica_study.sets', studySetId])]));

            tree.addQuery(studyTargetQuery, new RQL.Query('limit', [this.from, this.size]));
            tree.addQuery(studyTargetQuery, new RQL.Query('fields', ['${searchStudyFields?join("', '")}']));
            tree.addQuery(studyTargetQuery, new RQL.Query('sort', ['${searchStudySortFields?join("', '")}']));
          } else if (tab && tab === 'networks' && networkSetId) {
            resultEventName = 'networks-results';
            resultType = 'networks';

            let networkTargetQuery = new RQL.Query(TARGETS.NETWORK);
            tree.addQuery(null, networkTargetQuery);

            tree.addQuery(networkTargetQuery, new RQL.Query('in', ['Mica_network.sets', networkSetId]));

            tree.addQuery(networkTargetQuery, new RQL.Query('limit', [this.from, this.size]));
            tree.addQuery(networkTargetQuery, new RQL.Query('fields', ['${searchNetworkFields?join("', '")}']));
            tree.addQuery(networkTargetQuery, new RQL.Query('sort', ['${searchNetworkSortFields?join("', '")}']));

            let studyTargetQuery = new RQL.Query(TARGETS.STUDY);
            tree.addQuery(null, studyTargetQuery);
            tree.addQuery(studyTargetQuery, new RQL.Query('in', ['Mica_study.className', this.studyClassName]));
          }

          tree.addQuery(null, new RQL.Query('locale', ['${.lang}']));

          let url = '/ws/' + resultType + '/_rql?query=' + tree.serialize();

          axios
            .get(MicaService.normalizeUrl(url))
            .then(function(response) {
              $('#loadingSet').hide();

              EventBus.$emit(resultEventName, {
                studyTypeSelection,
                type: resultType,
                response: response.data,
                from: this.from,
                size: this.size
              });
            });
        }
      },
      beforeMount() {
        EventBus.register("variables-results", this.onResult.bind(this));
        EventBus.register("studies-results", this.onResult.bind(this));
        EventBus.register("networks-results", this.onResult.bind(this));
      },
      mounted() {
        this.pagination = new OBiBaPagination('obiba-pagination-top', true, this.onPagination),
        this.pageSizeSelector = new OBiBaPageSizeSelector('obiba-page-size-selector-top', DEFAULT_PAGE_SIZES, DEFAULT_PAGE_SIZE, this.onPageSizeChanged);

        EventBus.register('variables-selections-updated', this.onSelectionChanged.bind(this));
        EventBus.register('studies-selections-updated', this.onSelectionChanged.bind(this));
        EventBus.register('networks-selections-updated', this.onSelectionChanged.bind(this));

        let url = '/ws/taxonomies/_filter?target=' + TARGETS.VARIABLE;
        let studyTaxonomyurl = '/ws/taxonomies/_filter?target=' + TARGETS.STUDY;
        let networkTaxonomyUrl = '/ws/taxonomies/_filter?target=' + TARGETS.NETWORK;

        axios.all([axios.get(MicaService.normalizeUrl(url)), axios.get(MicaService.normalizeUrl(studyTaxonomyurl)), axios.get(MicaService.normalizeUrl(networkTaxonomyUrl))]).
          then(axios.spread((...responses) => {
            let taxonomies = [];

            responses.forEach((response) => {
              for (let taxo of response.data) {
                taxonomies[taxo.name] = taxo;
              }
            });

            taxonomyTitleFinder.initialize(taxonomies);

            Vue.filter("taxonomy-title", (input) => {
              const [taxonomy, vocabulary, term] = input.split(/\./);
              return  taxonomyTitleFinder.title(taxonomy, vocabulary, term) || input;
            });

            let setType = this.currentWindowLocationSearch()['type'];
            let totalCount = 0;

            if (!setType || setType === 'variables') {
              totalCount = totalCounts.variablesCount;
            } else if (setType && setType === 'studies') {
              totalCount = totalCounts.studiesCount;
            } else if (setType && setType === 'networks') {
              totalCount = totalCounts.networksCount;
            }

            this.doQuery(setType);
          }));
      },
      beforeUnmount() {
        EventBus.unregister("variables-results", this.onResult.bind(this));
        EventBus.unregister("studies-results", this.onResult.bind(this));
        EventBus.unregister("networks-results", this.onResult.bind(this));
      }
    });
  });
</script>

<#include "special-char-codec.ftl">
