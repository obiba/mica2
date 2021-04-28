<#include "entities-list-scripts.ftl">
<script>
  $(function () {
    $("#${title}").DataTable(dataTablesDefaultOpts);

    const sortOptionsTranslations = {
      'name': "<@message "global.name"/>",
      'acronym': "<@message "acronym"/>",
      'lastModifiedDate': "<@message "last-modified"/>",
      <#if title == "individual-studies">
      'numberOfParticipants-participant-number': "<@message "numberOfParticipants.label"/>"
      </#if>
    };

    if (document.querySelector("#cards")) {
      ObibaStudiesApp.build("#cards", "${title}", "${.lang}", sortOptionsTranslations);
    }
  });
</script>
