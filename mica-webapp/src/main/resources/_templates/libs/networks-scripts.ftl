<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<#include "entities-list-scripts.ftl">
<!-- page script -->
<script>
  $(function () {
    $("#networks").DataTable(dataTablesDefaultOpts);

    const sortOptionsTranslations = {
      'name': "${messageJs("global.name")}",
      'acronym': "${messageJs("acronym")}",
      'numberOfStudies': "${messageJs("network.number-of-studies")}"
    };

    if (document.querySelector("#cards")) {
      ObibaNetworksApp.build("#cards", "${.lang}", sortOptionsTranslations);
    }
  });
</script>
</#outputformat>
