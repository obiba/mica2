<!-- Member macros -->

<!-- Member model -->
<#macro memberModel member>
  <dl class="row">
    <#if member.person.title??>
      <dt class="col-sm-4"><@message "contact.title"/></dt>
      <dd class="col-sm-8">${member.person.title}</dd>
    </#if>
    <dt class="col-sm-4"><@message "contact.name"/></dt>
    <dd class="col-sm-8">${member.person.firstName!" "} ${member.person.lastName}</dd>

      <#if member.person.email??>
        <dt class="col-sm-4"><@message "contact.email"/></dt>
        <dd class="col-sm-8">${member.person.email}</dd>
      </#if>

      <#if member.person.phone??>
        <dt class="col-sm-4"><@message "contact.phone"/></dt>
        <dd class="col-sm-8">${member.person.phone}</dd>
      </#if>

      <#if member.person.institution??>
        <#if member.person.institution.name??>
          <dt class="col-sm-4"><@message "contact.institution"/></dt>
          <dd class="col-sm-8">${localize(member.person.institution.name)}</dd>
        </#if>
        <#if member.person.institution.department??>
          <dt class="col-sm-4"><@message "contact.department"/></dt>
          <dd class="col-sm-8">${localize(member.person.institution.department)}</dd>
        </#if>
        <#if member.person.institution.address?? && (member.person.institution.address.street?? || member.person.institution.address.city??)>
          <dt class="col-sm-4"><@message "address.label"/></dt>
          <dd class="col-sm-8">
            <#if member.person.institution.address.street??>
              ${localize(member.person.institution.address.street)}
            </#if>
            <#if member.person.institution.address.city??>
              ${localize(member.person.institution.address.city)}
            </#if>
            <#if member.person.institution.address.countryIso??>
              <@message member.person.institution.address.countryIso/>
            </#if>
          </dd>
        </#if>
      </#if>
  </dl>
</#macro>

<!-- Member modal dialog -->
<#macro memberDialog id member>
  <div class="modal fade" id="modal-${id}">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><#if member.person.firstName??>${member.person.firstName} </#if>${member.person.lastName}</h4>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
            
          </button>
        </div>
        <div class="modal-body">
          <@memberModel member=member/>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" data-bs-dismiss="modal"><@message "close"/></button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>

<!-- Member list by role -->
<#macro memberList members role>
  <ul class="pl-3">
    <#assign i=0>
    <#list members as member>
      <li>
        <#assign i++>
        <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${role}-${i}">
          ${member.person.title!""} ${member.person.firstName!""} ${member.person.lastName}
        </a>
        <#if member.person.institution?? && member.person.institution.name??>
          <div><small>${localize(member.person.institution.name)}</small></div>
        </#if>
        <@memberDialog id="${role}-${i}" member=member/>
      </li>
    </#list>
  </ul>
</#macro>
