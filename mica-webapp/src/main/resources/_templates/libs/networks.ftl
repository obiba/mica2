<!-- Networks page macros -->

<!-- Networks in table model templates -->
<#macro networkTableHeadModel>
  <tr>
    <th><@message "acronym"/></th>
    <th><@message "name"/></th>
    <th><@message "description"/></th>
  </tr>
</#macro>

<#macro networkTableRowModel network>
  <tr>
    <td><a href="${contextPath}/network/${network.id}">${localize(network.acronym)}</a></td>
    <td><small>${localize(network.name)}</small></td>
    <td class="marked"><template><small>${localize(network.description)?trim?truncate_w(100, "...")}</small></template></td>
  </tr>
</#macro>

<!-- Networks in lines model template -->
<#macro networkLineModel network>
  <div class="col-lg-3 col-sm-12">
    <#if network.logo??>
      <img class="img-fluid" style="max-height: 150px" alt="${localize(network.acronym)} logo" src="${contextPath}/ws/network/${network.id}/file/${network.logo.id}/_download"/>
    <#else >
      <div class="text-black-50 text-center mt-5">
        <i class="${networkIcon} fa-3x"></i>
      </div>
    </#if>
  </div>
  <div class="col-lg-9 col-sm-12">
    <h2 class="lead"><b>${localize(network.acronym)}</b></h2>
    <p class="text-muted text-sm">${localize(network.name)}</p>
    <div class="marked">
      <template>${localize(network.description)?trim?truncate_w(200, "...")}</template>
    </div>
    <div class="mt-2">
      <a href="${contextPath}/network/${network.id}" class="btn btn-sm btn-outline-info">
        <@message "global.read-more"/>
      </a>
    </div>
  </div>
</#macro>

<!-- Networks in cards model template -->
<#macro networkCardModel network>
  <div class="card bg-light w-100">
    <div class="card-header text-dark border-bottom-0">
      <h2 class="lead"><b>${localize(network.acronym)}</b></h2>
    </div>
    <div class="card-body pt-0">
      <div class="row">
        <div class="col-7">
          <p class="text-muted text-sm">${localize(network.name)}</p>
        </div>
        <div class="col-5 text-center">
          <#if network.logo??>
            <img class="img-fluid" style="max-height: 200px" alt="${localize(network.acronym)} logo" src="${contextPath}/ws/network/${network.id}/file/${network.logo.id}/_download"/>
          <#else >
            <p class="text-black-50 text-center mr-5 ml-5 pr-5">
              <i class="${networkIcon} fa-3x"></i>
            </p>
          </#if>
        </div>
      </div>
    </div>
    <div class="card-footer">
      <div class="text-right">
        <a href="${contextPath}/network/${network.id}" class="btn btn-sm btn-outline-info">
          <i class="fas fa-eye"></i> <@message "global.read-more"/>
        </a>
      </div>
    </div>
  </div>
</#macro>
