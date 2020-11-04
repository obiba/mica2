<!-- Additional columns for registered users -->
<#macro userProfileTHs>

</#macro>
<#macro userProfileTDs profile>

</#macro>

<!-- User profile -->
<#macro userProfile profile>
  <dl class="row">
    <dt class="col-sm-4"><@message "full-name"/></dt>
    <dd class="col-sm-8">${profile.fullName}</dd>
    <dt class="col-sm-4"><@message "username"/></dt>
    <dd class="col-sm-8">${profile.username}</dd>
    <#if showProfileGroups && profile.groups??>
      <dt class="col-sm-4"><@message "groups"/></dt>
      <dd class="col-sm-8">
        <#list profile.groups as group>
          <span class="badge badge-info">${group}</span>
        </#list>
      </dd>
    </#if>
    <#if showProfileRole && profile.roles??>
      <dt class="col-sm-4"><@message "roles"/></dt>
      <dd class="col-sm-8">
        <#list profile.roles as role>
          <span class="badge badge-primary">${role}</span>
        </#list>
      </dd>
    </#if>
    <#if profile.attributes??>
      <#list profile.attributes?keys as key>
        <#if key != "realm" || isAdministrator>
          <dt class="col-sm-4">
            <@message key/>
          </dt>
          <dd class="col-sm-8">
            <#if key == "createdDate" || key == "lastLogin">
              <span class="moment-datetime">${profile.attributes[key].toString(datetimeFormat)}</span>
            <#elseif key == "email">
              <a href="mailto:${profile.attributes[key]}">${profile.attributes[key]}</a>
            <#elseif key == "locale">
              <@message profile.attributes[key]/>
            <#elseif profile.attributes[key] == "true">
              <i class="fas fa-check"></i>
            <#elseif key == "realm">
              <code>${profile.attributes[key]}</code>
            <#else>
              ${profile.attributes[key]}
            </#if>
          </dd>
        </#if>
      </#list>
    </#if>
  </dl>
</#macro>

<!-- User profile modal -->
<#macro userProfileDialog profile>
  <div class="modal fade" id="modal-${profile.username?replace(".", "-")}">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title">${profile.fullName}</h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <@userProfile profile=profile/>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" data-dismiss="modal"><@message "close"/></button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>
