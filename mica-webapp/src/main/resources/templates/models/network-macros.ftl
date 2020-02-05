<!-- Network page macros -->

<!-- Individual study list: headers -->
<#macro individualStudyTHs>
  <th>Study Design</th>
  <th>Participants</th>
  <th>Countries</th>
</#macro>
<!-- Individual study list: data -->
<#macro individualStudyTDs study>
  <td>
    <#assign code="study_taxonomy.vocabulary.methods-design.term." + study.model.methods.design + ".title"/>
    <@message code/>
  </td>
  <td>${study.model.numberOfParticipants.participant.number}</td>
  <td></td>
</#macro>

<!-- Harmonization study list: headers -->
<#macro harmonizationStudyTHs>
</#macro>
<!-- Harmonization study list: data -->
<#macro harmonizationStudyTDs study>
</#macro>

<!-- Network list: headers -->
<#macro networkTHs>
</#macro>
<!-- Network list: data -->
<#macro networkTDs network>
</#macro>
