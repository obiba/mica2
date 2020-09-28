<!-- Project page macros -->

<!-- Project model template -->
<#macro projectModel project>
  <#if project.model??>
    <small>
      <#if project.model.institution??>${project.model.institution} - </#if>
      ${project.model.name}
    </small> -
    <span class="badge badge-info moment-date">${project.model.startDate}</span>
    <small><i class="fas fa-arrow-right"></i></small>
    <span class="badge badge-info moment-date">${project.model.endDate}</span>
  </#if>
</#macro>
