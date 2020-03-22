<!-- MicaConfig in JSON Format -->
<script>
  const Mica = { config: ${configJson!"{}"} , locale: "${.lang}" };
  Mica.tr = {
    "all": '<@message "all"/>',
    "variables": '<@message "variables"/>',
    "datasets": '<@message "datasets"/>',
    "studies": '<@message "studies"/>',
    "networks": '<@message "networks"/>',
    "name": '<@message "name"/>',
    "label": '<@message "label"/>',
    "annotations": '<@message "annotations"/>',
    "study": '<@message "study"/>',
    "dataset": '<@message "dataset"/>',
    "acronym": '<@message "acronym"/>',
    "type": '<@message "type"/>',
    "study-design": '<@message "study-design"/>',
    "data-sources-available": '<@message "data-sources-available"/>',
    "participants": '<@message "participants"/>',
    "individual": '<@message "individual"/>',
    "harmonization": '<@message "harmonization"/>',
    "collected": '<@message "collected"/>',
    "harmonized": '<@message "harmonized"/>',
    "dataschema": '<@message "Dataschema"/>',
    "no-variable-found": '<@message "no-variable-found"/>',
    "no-dataset-found": '<@message "no-dataset-found"/>',
    "no-study-found": '<@message "no-study-found"/>',
    "no-network-found": '<@message "no-network-found"/>',
    "coverage-buckets-study": '<@message "coverage-buckets-study"/>',
    "coverage-buckets-dce": '<@message "coverage-buckets-dce"/>',
    "coverage-buckets-dataset": '<@message "coverage-buckets-dataset"/>',
    "taxonomy": '<@message "taxonomy"/>',
    "select-all": '<@message "select-all"/>',
    "clear-selection": '<@message "clear-selection"/>',
    "search.filter": '<@message "search.filter"/>',
    "search.in": '<@message "search.in"/>',
    "search.out": '<@message "search.out"/>',
    "search.none": '<@message "search.none"/>',
    "search.any": '<@message "search.any"/>'
  };
  Mica.icons = {
    variable: '${variableIcon}',
    dataset: '${datasetIcon}',
    study: '${studyIcon}',
    network: '${networkIcon}'
  };
</script>

<!-- Mica Search and dependencies -->
<script src="../assets/libs/node_modules/vue/dist/vue.js"></script>
<script src="../assets/libs/node_modules/rql/dist/rql.js"></script>
<script src="../assets/libs/node_modules/vue-obiba-search-result/dist/VueObibaSearchResult.umd.js"></script>
<script src="../assets/libs/mica-query.js"></script>
<script src="../assets/js/mica-search.js"></script>
