<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<#include "entities-list-scripts.ftl">
<script>
  $(function () {
    $("#${title}").DataTable(dataTablesDefaultOpts);

    const sortOptionsTranslations = {
      'name': "${messageJs("global.name")}",
      'acronym': "${messageJs("acronym")}",
      'lastModifiedDate': "${messageJs("last-modified")}",
      <#if title == "individual-studies">
      'numberOfParticipants-participant-number': "${messageJs("numberOfParticipants.label")}"
      </#if>
    };

    if (document.querySelector("#cards")) {
      ObibaStudiesApp.build("#cards", "${title}", "${.lang}", sortOptionsTranslations);
    }
  });
</script>
</#outputformat>
