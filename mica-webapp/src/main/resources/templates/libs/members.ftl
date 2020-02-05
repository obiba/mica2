<#macro membermodal id member>
  <div class="modal fade" id="modal-${id}">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><#if member.person.firstName??>${member.person.firstName} </#if>${member.person.lastName}</h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <dl class="row">
            <#if member.person.title??>
              <dt class="col-sm-4">Title</dt>
              <dd class="col-sm-8">${member.person.title}</dd>
            </#if>
            <dt class="col-sm-4">Name</dt>
            <dd class="col-sm-8">${member.person.firstName!" "} ${member.person.lastName}</dd>

            <#if member.person.email??>
              <dt class="col-sm-4">Email</dt>
              <dd class="col-sm-8">${member.person.email}</dd>
            </#if>

            <#if member.person.phone??>
              <dt class="col-sm-4">Phone</dt>
              <dd class="col-sm-8">${member.person.phone}</dd>
            </#if>

            <#if member.person.institution??>
              <#if member.person.institution.name??>
                <dt class="col-sm-4">Institution</dt>
                <dd class="col-sm-8">${member.person.institution.name[.lang]}</dd>
              </#if>
              <#if member.person.institution.department??>
                <dt class="col-sm-4">Department</dt>
                <dd class="col-sm-8">${member.person.institution.department[.lang]}</dd>
              </#if>
              <#if member.person.institution.address?? && (member.person.institution.address.street?? || member.person.institution.address.city??)>
                <dt class="col-sm-4">Address</dt>
                <dd class="col-sm-8">
                  <#if member.person.institution.address.street??>
                    ${member.person.institution.address.street[.lang]!""},
                  </#if>
                  <#if member.person.institution.address.city??>
                    ${member.person.institution.address.city[.lang]!""}
                  </#if>
                  <#if member.person.institution.address.countryIso??>
                    <@message member.person.institution.address.countryIso/>
                  </#if>
                </dd>
              </#if>
            </#if>
          </dl>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>

<#macro memberlist members role>
  <ul>
    <#assign i=0>
    <#list members as member>
      <li>
        <#assign i++>
        <a href="#" data-toggle="modal" data-target="#modal-${role}-${i}">
          ${member.person.title!""} ${member.person.firstName!""} ${member.person.lastName}
        </a>
        <#if member.person.institution?? && member.person.institution.name??>
          <div><small>${member.person.institution.name[.lang]!""}</small></div>
        </#if>
        <@membermodal id="${role}-${i}" member=member/>
      </li>
    </#list>
  </ul>
</#macro>
