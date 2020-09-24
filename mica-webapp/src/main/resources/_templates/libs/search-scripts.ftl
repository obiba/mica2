<!-- MicaConfig in JSON Format -->
<script>
  const Mica = { config: ${configJson!"{}"}, locale: "${.lang}" };
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
    "acronym": "<@message "acronym"/>",
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
    "variables-added-to-cart": "<@message "variables-added-to-cart"/>",
    "collapse": "<@message "collapse"/>",
    "value": "<@message "value"/>",
    "frequency": "<@message "frequency"/>"
  };

  Mica.trArgs = (msgKey, msgArgs) => {
    let template = Mica.tr[msgKey] || msgKey;
    (msgArgs || []).forEach((arg, index) => template = template.replace('{'+index+'}', arg));
    return template;
  };

  Mica.icons = {
    variable: '${variableIcon}',
    dataset: '${datasetIcon}',
    study: '${studyIcon}',
    network: '${networkIcon}'
  };

  Mica.charts = {
    backgroundColor: '${barChartBackgroundColor}',
    borderColor: '${barChartBorderColor}',
    backgroundColors: ['${colors?join("', '")}']
  }
</script>

<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-charts.js"></script>

<!-- Mica Search and dependencies -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/libs/node_modules/vue-obiba-search-result/dist/VueObibaSearchResult.umd.js"></script>
<script src="${assetsPath}/js/mica-query.js"></script>
<script src="${assetsPath}/js/mica-search.js"></script>
