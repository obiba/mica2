<!-- Network page macros -->

<!-- Network model template -->
<#macro networkModel network>
</#macro>

<!-- Individual study list: headers -->
<#macro individualStudyTHs>
  <th><@message "study-design"/></th>
  <th><@message "participants"/></th>
  <th><@message "countries"/></th>
</#macro>
<!-- Individual study list: data -->
<#macro individualStudyTDs study>
  <td>
    <#assign code="study_taxonomy.vocabulary.methods-design.term." + study.model.methods.design + ".title"/>
    <@message code/>
  </td>
  <td>${study.model.numberOfParticipants.participant.number}</td>
  <td>
    <#if study.populations?? && study.populations?size != 0>
      <#assign countries = []/>
      <#list study.populations as population>
        <#if population.model.selectionCriteria?? && population.model.selectionCriteria.countriesIso?? && population.model.selectionCriteria.countriesIso?size != 0>
          <#list population.model.selectionCriteria.countriesIso as country>
            <#if countries?seq_contains(country)>
            <#else>
              <#if countries?size != 0>,</#if>
              <#assign countries = countries + [country]/>
              <@message country/>
            </#if>
          </#list>
        </#if>
      </#list>
    </#if>
  </td>
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
