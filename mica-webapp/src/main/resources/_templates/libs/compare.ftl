<!-- Compare page macros -->

<!-- Studies compare model template -->
<#macro studiesCompareModel studies>
  <tr id="name">
    <td class="font-weight-bold">
        <@message "name"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('name')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>${localize(study.name)}</td>
    </#list>
  </tr>
  <tr id="objectives">
    <td class="font-weight-bold">
        <@message "study.objectives"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('objectives')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <small class="marked truncate truncate-300">
          <template>${localize(study.objectives)}</template></small>
      </td>
    </#list>
  </tr>
  <tr id="start-year">
    <td class="font-weight-bold">
        <@message "study.start-year"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('start-year')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.startYear??>
            ${study.model.startYear?c}
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="end-year">
    <td class="font-weight-bold">
        <@message "study.end-year"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('end-year')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.endYear??>
            ${study.model.endYear?c}
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="funding">
    <td class="font-weight-bold">
        <@message "funding"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('funding')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>${localize(study.funding)}</td>
    </#list>
  </tr>
  <tr id="suppl-info">
    <td class="font-weight-bold">
      <@message "suppl-info"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('suppl-info')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td><small class="marked truncate truncate-100"><template>${localize(study.model.info)}</template></small></td>
    </#list>
  </tr>
  <tr id="website">
    <td class="font-weight-bold">
        <@message "website"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('website')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.website??>
          <a href="${study.model.website}" target="_blank">${study.model.website}</a>
        </#if>
      </td>
    </#list>
  </tr>
</#macro>

<!-- Networks compare model template -->
<#macro networksCompareModel networks>
  <tr id="name">
    <td class="font-weight-bold">
        <@message "name"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('name')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list networks as network>
      <td>${localize(network.name)}</td>
    </#list>
  </tr>
  <tr id="description">
    <td class="font-weight-bold">
        <@message "network.description"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('description')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list networks as network>
      <td>
        <small class="marked truncate truncate-300">
          <template>${localize(network.description)}</template></small>
      </td>
    </#list>
  </tr>
  <tr id="website">
    <td class="font-weight-bold">
        <@message "website"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('website')"><i class="fa fa-times"></i></a></small>
    </td>
    <#list networks as network>
      <td>
        <#if network.model.website??>
          <a class="d-print-none" href="${network.model.website}" target="_blank">${network.model.website}</a>
        </#if>
      </td>
    </#list>
  </tr>
</#macro>
