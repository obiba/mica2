<!-- Project page macros -->

<!-- Project model template -->
<#macro projectModel project>
  <small>${project.model.institution} - ${project.model.name}</small> -
  <span class="badge badge-info moment-date">${project.model.startDate}</span> <small><i class="fas fa-arrow-right"></i></small> <span class="badge badge-info moment-date">${project.model.endDate}</span>
</#macro>
