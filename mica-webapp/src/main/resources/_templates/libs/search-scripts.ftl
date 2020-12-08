<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<!-- MicaConfig in JSON Format -->
<script>
  const Mica = {
    config: ${configJson!"{}"},
    locale: "${.lang}",
    defaultLocale: "${defaultLang}"
  };

  Mica.tr = {
    "all": "<@message "all"/>",
    "variables": "<@message "variables"/>",
    "variable": "<@message "variable"/>",
    "datasets": "<@message "datasets"/>",
    "studies": "<@message "studies"/>",
    "networks": "<@message "networks"/>",
    "network": "<@message "network"/>",
    "name": "<@message "name"/>",
    "label": "<@message "label"/>",
    "annotations": "<@message "annotations"/>",
    "study": "<@message "study"/>",
    "dataset": "<@message "dataset"/>",
    "population": "<@message "population"/>",
    "data-collection-event": "<@message "data-collection-event"/>",
    "dce": "<@message "data-collection-event"/>",
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
    "coverage-buckets-study": "<@message "coverage-buckets-study"/>",
    "coverage-buckets-dce": "<@message "coverage-buckets-dce"/>",
    "coverage-buckets-dataset": "<@message "coverage-buckets-dataset"/>",
    "no-coverage-available": "<@message "no-coverage-available"/>",
    "coverage-end-date-ongoing": "<@message "coverage-end-date-ongoing"/>",
    "missing-variable-query": "<@message "missing-variable-query"/>",
    "no-graphics-result":  "<@message "no-graphics-result"/>",
    "taxonomy": "<@message "taxonomy"/>",
    "select-all": "<@message "select-all"/>",
    "clear-selection": "<@message "clear-selection"/>",
    "search.filter": "<@message "search-filter"/>",
    "search.filter-help": "<@message "search-filter-help"/>",
    "search.in": "<@message "search-in"/>",
    "search.out": "<@message "search-out"/>",
    "search.none": "<@message "search-none"/>",
    "search.any": "<@message "search-any"/>",
    "search.from": "<@message "search-from"/>",
    "search.to": "<@message "search-to"/>",
    "search.and": "<@message "search-and"/>",
    "search.or": "<@message "search-or"/>",
    "query-update": "<@message "query-update"/>",
    "criterion.created": "<@message "criterion-created"/>",
    "criterion.updated": "<@message "criterion-updated"/>",
    "geographical-distribution-chart-title": "<@message "geographical-distribution-chart-title"/>",
    "geographical-distribution-chart-text": "<@message "geographical-distribution-chart-text"/>",
    "study-design-chart-title": "<@message "study-design-chart-title"/>",
    "study-design-chart-text": "<@message "study-design-chart-text"/>",
    "number-participants-chart-title": "<@message "number-participants-chart-title"/>",
    "number-participants-chart-text": "<@message "number-participants-chart-text"/>",
    "bio-samples-chart-title": "<@message "bio-samples-chart-title"/>",
    "bio-samples-chart-text": "<@message "bio-samples-chart-text"/>",
    "study-start-year-chart-title": "<@message "study-start-year-chart-title"/>",
    "study-start-year-chart-text": "<@message "study-start-year-chart-text"/>",
    "to": "<@message "to"/>",
    "more": "<@message "search.facet.more"/>",
    "less": "<@message "search.facet.less"/>",
    "no-variable-added": "<@message "sets.cart.no-variable-added"/>",
    "no-variable-added-set": "<@message "sets.set.no-variable-added"/>",
    "variables-added-to-cart": "<@message "variables-added-to-cart"/>",
    "variables-added-to-set": "<@message "variables-added-to-set"/>",
    "collapse": "<@message "collapse"/>",
    "value": "<@message "value"/>",
    "frequency": "<@message "frequency"/>"
  };

  Mica.trArgs = (msgKey, msgArgs) => {
    let template = Mica.tr[msgKey] || msgKey;
    (msgArgs || []).forEach((arg, index) => template = template.replace('{'+index+'}', arg));
    return template;
  };

  Mica.maxNumberOfSets = ${maxNumberOfSets};

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
  }

  Mica.icons = {
    variable: '${variableIcon}',
    dataset: '${datasetIcon}',
    study: '${studyIcon}',
    network: '${networkIcon}'
  };

  Mica.charts = {
    backgroundColor: '${barChartBackgroundColor}',
    borderColor: '${barChartBorderColor}',
    backgroundColors: ['${colors?join("', '")}'],
    chartIds: ['${searchCharts?join("', '")}']
  };

  Mica.display = {
    variableColumns: ['${searchVariableColumns?join("', '")}'],
    datasetColumns: ['${searchDatasetColumns?join("', '")}'],
    studyColumns: ['${searchStudyColumns?join("', '")}'],
    networkColumns: ['${searchNetworkColumns?join("', '")}'],
    searchCriteriaMenus: ['${searchCriteriaMenus?join("', '")}']
  };

  fetch(contextPath + '/assets/topojson/${mapName}.json').then(r => r.json())
          .then(data => Mica.map = {
            name: '${mapName}',
            topo: data
          });
</script>

<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/libs/node_modules/chartjs-chart-geo/build/Chart.Geo.min.js"></script>

<!-- Mica Search and dependencies -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/libs/node_modules/vue-mica-search/dist/VueMicaSearch.umd.js"></script>
<script src="${assetsPath}/js/mica-tables.js"></script>
<script src="${assetsPath}/js/mica-query.js"></script>
<script src="${assetsPath}/js/mica-search.js"></script>
