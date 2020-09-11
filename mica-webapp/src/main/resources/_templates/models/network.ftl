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
    <#if study.model.methods?? && study.model.methods.design??>
      <#assign code="study_taxonomy.vocabulary.methods-design.term." + study.model.methods.design + ".title"/>
      <@message code/>
    </#if>
  </td>
  <td>
    <#if study.model.numberOfParticipants.participant.number??>
      ${study.model.numberOfParticipants.participant.number}
    </#if>
  </td>
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

<!-- Files -->
<#macro networkFilesBrowser network>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "files"/></h3>
    </div>
    <div class="card-body">

      <div id="files-app">
        <@filesBrowser/>
      </div>

    </div>
  </div>
</#macro>

<!-- Variables classifications -->
<#macro variablesClassifications network studyAcronyms>
  <img id="loadingClassifications" src="${assetsPath}/images/loading.gif">
  <div id="classificationsContainer" style="display: none;" class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "variables-classifications"/></h3>
    </div>
    <div class="card-body">
      <div>
        <div class="mb-4">
          <select id="select-bucket" class="form-control select2">
            <option value="_all" selected><@message "all-studies"/></option>
          </select>
        </div>
        <div id="chartsContainer"></div>
      </div>
      <div id="noVariablesClassifications" style="display: none">
        <span class="text-muted"><@message "no-variables-classifications"/></span>
      </div>
    </div>
  </div>
</#macro>
