<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<#include "entities-list-scripts.ftl">
<script>
  $(function () {
    $("#${title}").DataTable(dataTablesDefaultOpts);

    const sortOptionsTranslations = {
      'name': "<@message "global.name"/>",
      'acronym': "<@message "acronym"/>",
      'studyTable.studyId,studyTable.populationWeight,studyTable.dataCollectionEventWeight,acronym': "<@message "global.chronological"/>"
    };

    if (document.querySelector("#cards")) {
      ObibaDatasetsApp.build("#cards", "${title}", "${.lang}", sortOptionsTranslations);
    }
  });
</script>
</#outputformat>
