<!-- Project page macros -->

<!-- Project model template for the list -->
<#macro projectModelSummary project>
  <#if project.model??>
    <span class="badge badge text-bg-info moment-date">${project.model.startDate}</span>
    <small><i class="fa-solid fa-arrow-right"></i></small>
    <span class="badge badge text-bg-info moment-date">${project.model.endDate}</span>
    - <small>
    <#if project.model.institution??>${project.model.institution} - </#if>
    ${project.model.name}
  </small>
  </#if>
</#macro>

<!-- Project model template -->
<#macro projectModel project>
  <#if project.model??>
    <div class="row">
      <div class="col-sm-6">
        <div class="card">
          <div class="card-header">
            <h5 class="card-title">
              <@message "calendar"/>
            </h5>
          </div>
          <!-- /.card-header -->
          <div class="card-body">
            <dl class="row">
              <dt class="col-sm-4"><@message "start-date"/></dt>
              <dd class="col-sm-8 moment-date">${project.model.startDate}</dd>
              <dt class="col-sm-4"><@message "end-date"/></dt>
              <dd class="col-sm-8 moment-date">${project.model.endDate}</dd>
            </dl>
          </div>
        </div>
      </div>
      <div class="col-sm-6">
        <div class="card">
          <div class="card-header">
            <h5 class="card-title">
              <@message "research-project.default.contact.title"/>
            </h5>
          </div>
          <!-- /.card-header -->
          <div class="card-body">
            <dl class="row">
              <dt class="col-sm-4"><@message "research-project.default.contact.name"/></dt>
              <dd class="col-sm-8">${project.model.name}</dd>
              <#if project.model.institution??>
                <dt class="col-sm-4"><@message "research-project.default.contact.institution"/></dt>
                <dd class="col-sm-8">
                  <pre class="p-0">${project.model.institution}</pre>
                </dd>
              </#if>
            </dl>
          </div>
        </div>
      </div>
    </div>
  </#if>
</#macro>
