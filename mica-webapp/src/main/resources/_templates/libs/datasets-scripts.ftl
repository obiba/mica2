<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<#include "entities-list-scripts.ftl">
<script>
  $(function () {
    $("#${title}").DataTable(dataTablesDefaultOpts);

    const sortOptionsTranslations = {
      'name': "${messageJs("global.name")}",
      'acronym': "${messageJs("acronym")}",
      'studyTable.studyId,studyTable.populationWeight,studyTable.dataCollectionEventWeight,acronym': "${messageJs("global.chronological")}"
    };

    if (document.querySelector("#cards")) {
      ObibaDatasetsApp.build("#cards", "${title}", "${.lang}", sortOptionsTranslations);
    }
  });
</script>
</#outputformat>
