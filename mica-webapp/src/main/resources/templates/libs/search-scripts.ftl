<!-- MicaConfig in JSON Format -->
<script>
  const Mica = { config: ${configJson!"{}"} };
  Mica.tr = {
    "variables": '<@message "variables"/>',
    "datasets": '<@message "datasets"/>',
    "studies": '<@message "studies"/>',
    "networks": '<@message "networks"/>',
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
