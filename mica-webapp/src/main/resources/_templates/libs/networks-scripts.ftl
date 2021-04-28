<#include "entities-list-scripts.ftl">
<!-- page script -->
<script>
  $(function () {
    $("#networks").DataTable(dataTablesDefaultOpts);

    const sortOptionsTranslations = {
      'name': "<@message "global.name"/>",
      'acronym': "<@message "acronym"/>",
      'numberOfStudies': "<@message "network.number-of-studies"/>"
    };

    if (document.querySelector("#cards")) {
      ObibaNetworksApp.build("#cards", "${.lang}", sortOptionsTranslations);
    }
  });
</script>
