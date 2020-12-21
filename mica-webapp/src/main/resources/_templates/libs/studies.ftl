<!-- Studies page macros -->

<!-- Studies in table model templates -->
<#macro studyTableHeadModel>
  <tr>
    <th><@message "acronym"/></th>
    <th><@message "name"/></th>
    <th><@message "description"/></th>
    <#if showTypeColumn>
      <th><@message "type"/></th>
    </#if>
  </tr>
</#macro>

<#macro studyTableRowModel study>
  <tr>
    <td><a href="${contextPath}/study/${study.id}">${localize(study.acronym)}</a></td>
    <td><small>${localize(study.name)}</small></td>
    <td class="marked"><template><small>${localize(study.objectives)?trim?truncate_w(100, "...")}</small></template></td>
    <#if showTypeColumn>
      <td>
        <#if study.class.simpleName == "HarmonizationStudy">
          <@message "harmonization"/>
        <#else>
          <@message "individual"/>
        </#if>
      </td>
    </#if>
  </tr>
</#macro>

<!-- Studies in lines model template -->
<#macro studyLineModel study>
  <div class="col-lg-3 col-sm-12">
    <#if study.logo??>
      <img class="img-fluid" style="max-height: 150px" alt="${localize(study.acronym)} logo" src="${contextPath}/ws/study/${study.id}/file/${study.logo.id}/_download"/>
    <#else >
      <div class="text-black-50 text-center mt-5">
        <i class="${studyIcon} fa-3x"></i>
      </div>
    </#if>
  </div>
  <div class="col-lg-9 col-sm-12">
    <h2 class="lead"><b>${localize(study.acronym)}</b></h2>
    <p class="text-muted text-sm">${localize(study.name)}</p>
    <div class="marked">
      <template>${localize(study.objectives)?trim?truncate_w(200, "...")}</template>
    </div>
    <div class="mt-2">
      <a href="${contextPath}/study/${study.id}" class="btn btn-sm btn-outline-info">
        <@message "global.read-more"/>
      </a>
    </div>
  </div>
</#macro>

<!-- Studies in cards model template -->
<#macro studyCardModel study>
  <div class="card bg-light w-100">
    <div class="card-header text-dark border-bottom-0">
      <h2 class="lead"><b>${localize(study.acronym)}</b></h2>
    </div>
    <div class="card-body pt-0">
      <div class="row">
        <div class="col-7">
          <p class="text-muted text-sm">${localize(study.name)}</p>
        </div>
        <div class="col-5 text-center">
          <#if study.logo??>
            <img class="img-fluid" style="max-height: 200px" alt="${localize(study.acronym)} logo" src="${contextPath}/ws/study/${study.id}/file/${study.logo.id}/_download"/>
          <#else >
            <p class="text-black-50 text-center mr-5 ml-5 pr-5">
              <i class="${studyIcon} fa-3x"></i>
            </p>
          </#if>
        </div>
      </div>
    </div>
    <div class="card-footer">
      <div class="text-right">
        <a href="${contextPath}/study/${study.id}" class="btn btn-sm btn-outline-info">
          <i class="fas fa-eye"></i> <@message "global.read-more"/>
        </a>
      </div>
    </div>
  </div>
</#macro>
