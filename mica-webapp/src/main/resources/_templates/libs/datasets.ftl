<!-- Datasets page macros -->

<!-- Datasets in table model templates -->
<#macro datasetTableHeadModel>
  <tr>
    <th><@message "acronym"/></th>
    <th><@message "name"/></th>
    <th><@message "description"/></th>
    <#if showTypeColumn>
      <th><@message "type"/></th>
    </#if>
  </tr>
</#macro>

<#macro datasetTableRowModel dataset>
  <tr>
    <td><a href="${contextPath}/dataset/${dataset.id}">${localize(dataset.acronym)}</a></td>
    <td><small>${localize(dataset.name)}</small></td>
    <td class="marked"><template><small>${localize(dataset.description)?trim?truncate_w(100, "...")}</small></template></td>
    <#if showTypeColumn>
      <td>
        <#if dataset.class.simpleName == "HarmonizationDataset">
          <@message "harmonized"/>
        <#else>
          <@message "collected"/>
        </#if>
      </td>
    </#if>
  </tr>
</#macro>

<!-- Datasets in lines model template -->
<#macro datasetLineModel dataset>
  <div class="col-lg-3 col-sm-12">
    <#if dataset.class.simpleName == "HarmonizationDataset">
      <div class="text-black-50 text-center mt-5">
        <i class="${harmoDatasetIcon} fa-3x"></i>
      </div>
    <#else>
      <div class="text-black-50 text-center mt-4">
        <i class="${datasetIcon} fa-3x"></i>
      </div>
    </#if>
  </div>
  <div class="col-lg-9 col-sm-12">
    <h2 class="lead"><b>${localize(dataset.acronym)}</b></h2>
    <p class="text-muted text-sm">${localize(dataset.name)}</p>
    <div class="marked">
      <template>${localize(dataset.description)?trim?truncate_w(200, "...")}</template>
    </div>
    <div class="mt-2">
      <a href="${contextPath}/dataset/${dataset.id}" class="btn btn-sm btn-outline-info">
        <@message "global.read-more"/>
      </a>
    </div>
  </div>
</#macro>

<!-- Datasets in cards model template -->
<#macro datasetCardModel dataset>
  <div class="card bg-light w-100">
    <div class="card-header text-dark border-bottom-0">
      <h2 class="lead"><b>${localize(dataset.acronym)}</b></h2>
    </div>
    <div class="card-body pt-0">
      <div class="row">
        <div class="col-7">
          <p class="text-muted text-sm">${localize(dataset.name)}</p>
        </div>
        <div class="col-5 text-center">
          <p class="text-black-50 text-center mr-5 ml-5 pr-5">
            <#if dataset.class.simpleName == "HarmonizationDataset">
              <i class="${harmoDatasetIcon} fa-3x"></i>
            <#else>
              <i class="${datasetIcon} fa-3x"></i>
            </#if>
          </p>
        </div>
      </div>
    </div>
    <div class="card-footer">
      <div class="text-right">
        <a href="${contextPath}/dataset/${dataset.id}" class="btn btn-sm btn-outline-info">
          <i class="fas fa-eye"></i> <@message "global.read-more"/>
        </a>
      </div>
    </div>
  </div>
</#macro>
