<!-- MicaConfig in JSON Format -->
<script>
  const Mica = { config: ${configJson!"{}"} };
  if (Mica.config.translations) {
    Mica.config.translations.forEach(function(tr) {
      if (tr.value) {
        tr.value = JSON.parse(tr.value);
      }
    });
  }
  console.dir(Mica.config);
</script>

<!-- Mica Search and dependencies -->
<script src="../assets/libs/node_modules/vue/dist/vue.js"></script>
<script src="../assets/libs/node_modules/rql/dist/rql.js"></script>
<script src="../assets/libs/node_modules/vue-obiba-search-result/dist/VueObibaSearchResult.umd.js"></script>
<script src="../assets/libs/mica-query.js"></script>
<script src="../assets/js/mica-search.js"></script>
