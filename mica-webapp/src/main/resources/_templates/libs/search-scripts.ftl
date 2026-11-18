<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<!-- MicaConfig in JSON Format -->
<script>
  const Mica = {
    config: ${configJson!"{}"},
    locale: "${.lang}",
    defaultLocale: "${defaultLang}"
  };

  Mica.useOnlyOpalTaxonomiesForCoverage = ${useOnlyOpalTaxonomiesForCoverage?c};

  Mica.tr = {
    "all": "${messageJs("all")}",
    "variables": "${messageJs("variables")}",
    "variable": "${messageJs("variable")}",
    "datasets": "${messageJs("datasets")}",
    "studies": "${messageJs("studies")}",
    "networks": "${messageJs("networks")}",
    "network": "${messageJs("network")}",
    "initiatives": "${messageJs("initiatives")}",
    "initiative": "${messageJs("initiative")}",
    "protocol": "${messageJs("protocol")}",
    "protocols": "${messageJs("protocols")}",
    "data-access-request": "${messageJs("data-access-request")}",
    "name": "${messageJs("name")}",
    "label": "${messageJs("label")}",
    "annotations": "${messageJs("annotations")}",
    "study": "${messageJs("study")}",
    "dataset": "${messageJs("dataset")}",
    "population": "${messageJs("population")}",
    "data-collection-event": "${messageJs("data-collection-event")}",
    "dce": "${messageJs("search.study.dce-name")}",
    "acronym": "${messageJs("acronym")}",
    "valueType": "${messageJs("value-type")}",
    "text-type": "${messageJs("text-type")}",
    "integer-type": "${messageJs("integer-type")}",
    "decimal-type": "${messageJs("decimal-type")}",
    "boolean-type": "${messageJs("boolean-type")}",
    "binary-type": "${messageJs("binary-type")}",
    "date-type": "${messageJs("date-type")}",
    "datetime-type": "${messageJs("datetime-type")}",
    "point-type": "${messageJs("point-type")}",
    "linestring-type": "${messageJs("linestring-type")}",
    "polygon-type": "${messageJs("polygon-type")}",
    "locale-type": "${messageJs("locale-type")}",
    "type": "${messageJs("type")}",
    "study-design": "${messageJs("study-design")}",
    "data-sources-available": "${messageJs("data-sources-available")}",
    "participants": "${messageJs("participants")}",
    "individual": "${messageJs("individual")}",
    "harmonization": "${messageJs("harmonization")}",
    "collected": "${messageJs("collected")}",
    "harmonized": "${messageJs("harmonized")}",
    "dataschema": "${messageJs("Dataschema")}",
    "no-variable-found": "${messageJs("no-variable-found")}",
    "no-dataset-found": "${messageJs("no-dataset-found")}",
    "no-study-found": "${messageJs("no-study-found")}",
    "no-network-found": "${messageJs("no-network-found")}",
    "coverage-buckets-study": "${messageJs("coverage-buckets-study")}",
    "coverage-buckets-dce": "${messageJs("coverage-buckets-dce")}",
    "coverage-buckets-dataset": "${messageJs("coverage-buckets-dataset")}",
    "coverage-buckets-harmonization": "${messageJs("coverage-buckets-harmonization")}",
    "no-coverage-available": "${messageJs("no-coverage-available")}",
    "download-error": "${messageJs("download-error")}",
    "coverage-end-date-ongoing": "${messageJs("coverage-end-date-ongoing")}",
    "missing-variable-query": "${messageJs("missing-variable-query")}",
    "no-graphics-result":  "${messageJs("no-graphics-result")}",
    "taxonomy": "${messageJs("taxonomy")}",
    "select-all": "${messageJs("select-all")}",
    "clear-selection": "${messageJs("clear-selection")}",
    "search.filter": "${messageJs("search-filter")}",
    "search.filter-help": "${messageJs("search-filter-help")}",
    "search.in": "${messageJs("search-in")}",
    "search.out": "${messageJs("search-out")}",
    "search.none": "${messageJs("search-none")}",
    "search.any": "${messageJs("search-any")}",
    "search.from": "${messageJs("search-from")}",
    "search.to": "${messageJs("search-to")}",
    "search.and": "${messageJs("search-and")}",
    "search.or": "${messageJs("search-or")}",
    "query-update": "${messageJs("query-update")}",
    "criterion.created": "${messageJs("criterion-created")}",
    "criterion.updated": "${messageJs("criterion-updated")}",
    "geographical-distribution-chart-title": "${messageJs("geographical-distribution-chart-title")}",
    "geographical-distribution-chart-text": "${messageJs("geographical-distribution-chart-text")}",
    "study-design-chart-title": "${messageJs("study-design-chart-title")}",
    "study-design-chart-text": "${messageJs("study-design-chart-text")}",
    "number-participants-chart-title": "${messageJs("number-participants-chart-title")}",
    "number-participants-chart-text": "${messageJs("number-participants-chart-text")}",
    "bio-samples-chart-title": "${messageJs("bio-samples-chart-title")}",
    "bio-samples-chart-text": "${messageJs("bio-samples-chart-text")}",
    "study-start-year-chart-title": "${messageJs("study-start-year-chart-title")}",
    "study-start-year-chart-text": "${messageJs("study-start-year-chart-text")}",
    "to": "${messageJs("to")}",
    "more": "${messageJs("search.facet.more")}",
    "less": "${messageJs("search.facet.less")}",
    "no-variable-added": "${messageJs("sets.cart.no-variable-added")}",
    "variables-added-to-cart": "${messageJs("variables-added-to-cart")}",
    "no-variable-added-set": "${messageJs("sets.set.no-variable-added")}",
    "variables-added-to-set": "${messageJs("variables-added-to-set")}",
    "no-study-added": "${messageJs("sets.cart.no-study-added")}",
    "studies-added-to-cart": "${messageJs("studies-added-to-cart")}",
    "no-network-added": "${messageJs("sets.cart.no-network-added")}",
    "networks-added-to-cart": "${messageJs("networks-added-to-cart")}",
    "collapse": "${messageJs("collapse")}",
    "value": "${messageJs("value")}",
    "frequency": "${messageJs("frequency")}",
    "graphics.total": "${messageJs("graphics.total")}",
    "search.coverage-dce-cols.study": "${messageJs("search.coverage-dce-cols.study")}",
    "search.coverage-dce-cols.population": "${messageJs("search.coverage-dce-cols.population")}",
    "search.coverage-dce-cols.dce": "${messageJs("search.coverage-dce-cols.dce")}",
    "search.coverage-dce-cols.harmonization": "${messageJs("search.coverage-dce-cols.harmonization")}",
    "search.coverage-buckets.dataset": "${messageJs("search.coverage-buckets.dataset")}",
    "search.coverage-buckets.study": "${messageJs("search.coverage-buckets.study")}"
  };

  Mica.trArgs = (msgKey, msgArgs) => {
    let template = Mica.tr[msgKey] || msgKey;
    (msgArgs || []).forEach((arg, index) => template = template.replace('{'+index+'}', arg));
    return template;
  };

  Mica.maxNumberOfSets = ${maxNumberOfSets?c};

  <#if rc.requestUri?starts_with("harmonization")>
    Mica.defaultSearchMode = "HarmonizationStudy"
  <#else>
    Mica.defaultSearchMode = "${defaultSearchMode}" || "Study";
  </#if>

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
    dataschema: '${dataschemaIcon}',
    dataset: '${datasetIcon}',
    harmoDataset: '${harmoDatasetIcon}',
    study: '${studyIcon}',
    initiative: '${initiativeIcon}',
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

  fetch(contextPath + '/assets/topojson/${mapName}.json').then(r => r.json())
          .then(data => Mica.map = {
            name: '${mapName}',
            topo: data
          });
</script>

<!-- ChartJS -->
<script src="${assetsPath}/libs/node_modules/plotly.js-dist-min/plotly.min.js"></script>

<!-- Mica Search and dependencies -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.global.js"></script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/js/vue-mica-search/libs/criterion.js"></script>
<script src="${assetsPath}/js/vue-mica-search/libs/result-parsers.js"></script>
<script src="${assetsPath}/js/vue-mica-search/builder.js"></script>
<script src="${assetsPath}/js/vue-mica-search/panel.js"></script>
<script src="${assetsPath}/js/vue-mica-search/result.js"></script>
<script src="${assetsPath}/js/mica-tables.js"></script>
<script src="${assetsPath}/js/mica-query.js"></script>
<script src="${assetsPath}/js/mica-search.js"></script>

<#include "special-char-codec.ftl">
</#outputformat>
