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
          </dl>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>

<#macro memberlist members role>
  <ul class="list-unstyled">
      <#assign i=0>
      <#list members as member>
        <li>
            <#assign i++>
          <a href="#" data-toggle="modal" data-target="#modal-${role}-${i}">
              ${member.person.title!""} ${member.person.firstName!""} ${member.person.lastName}
          </a>
            <@membermodal id="${role}-${i}" member=member/>
        </li>
      </#list>
  </ul>
</#macro>
