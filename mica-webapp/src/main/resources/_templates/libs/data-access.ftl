<!-- Function to get the color corresponding to a status -->
<#function statusColor status>
  <#if status == "OPENED">
    <#local txtColor = "primary"/>
  <#elseif status == "APPROVED">
    <#local txtColor = "success"/>
  <#elseif status == "REJECTED">
    <#local txtColor = "danger"/>
  <#elseif status == "SUBMITTED">
    <#local txtColor = "info"/>
  <#elseif status == "REVIEWED">
    <#local txtColor = "info"/>
  <#elseif status == "CONDITIONALLY_APPROVED">
    <#local txtColor = "warning"/>
  <#else>
    <#local txtColor = "info"/>
  </#if>
  <#return txtColor/>
</#function>

<!-- Function to check if a date is in the past -->
<#function isPast date>
  <#return date?? && .now?datetime gt date?datetime>
</#function>

<!-- Data access timeline -->
<#macro dataAccessTimeline dar reportTimeline>
  <!-- Timeline -->
  <div class="timeline">
    <!-- timeline time label -->
    <div class="time-label">
      <span class="<#if isPast(reportTimeline.startDate)>bg-secondary<#else>bg-primary</#if>">${reportTimeline.startDate?date}</span>
    </div>
    <!-- /.timeline-label -->
    <!-- timeline item -->
    <div>
      <i class="fas fa-info <#if isPast(reportTimeline.startDate)>bg-secondary<#else>bg-blue</#if>"></i>
      <div class="timeline-item">
        <div class="timeline-body">

          <#if isDAO || isAdministrator>
            <p><@message "start-date-dao-text"/></p>
            <div>
              <button type="button" class="btn btn-primary btn-sm" data-toggle="modal" data-target="#modal-start-date" <#if dar.archived>disabled</#if>>
                <i class="fas fa-clock"></i> <@message "start-date"/>
              </button>
            </div>
          <#else>
            <p><@message "start-date-applicant-text"/></p>
            <a href="${contextPath}/data-access-comments/${dar.id}"><@message "send-message"/> <i
                class="fas fa-arrow-circle-right ml-1"></i></a>
          </#if>
        </div>
      </div>
    </div>
    <!-- END timeline item -->

    <#if reportTimeline.intermediateDates??>
      <#list reportTimeline.intermediateDates as date>
        <!-- timeline time label -->
        <div class="time-label">
          <span class="<#if isPast(date)>bg-secondary<#else>bg-info</#if>">${date?date}</span>
        </div>
        <!-- /.timeline-label -->

        <!-- timeline item -->
        <div>
          <i class="fas fa-file <#if isPast(date)>bg-secondary<#else>bg-blue</#if>"></i>
          <div class="timeline-item">
            <div class="timeline-body">
              <span class="badge badge-info">${date?counter}</span>
              <#if isDAO || isAdministrator>
                <span><@message "intermediate-date-dao-text"/></span>
              <#else>
                <span><@message "intermediate-date-applicant-text"/></span>
              </#if>
            </div>
          </div>
        </div>
        <!-- END timeline item -->
      </#list>
    </#if>

    <!-- timeline time label -->
    <div class="time-label">
      <span class="<#if isPast(reportTimeline.endDate)>bg-secondary<#else>bg-blue</#if>">${reportTimeline.endDate?date}</span>
    </div>
    <!-- /.timeline-label -->
    <!-- timeline item -->
    <div>
      <i class="fas fa-book <#if isPast(reportTimeline.endDate)>bg-secondary<#else>bg-blue</#if>"></i>
      <div class="timeline-item">
        <div class="timeline-body">

          <#if isDAO || isAdministrator>
            <span><@message "end-date-dao-text"/></span>
          <#else>
            <span><@message "end-date-applicant-text"/></span>
          </#if>
        </div>
      </div>
    </div>
    <!-- END timeline item -->

    <div>
      <i class="fas fa-circle bg-gray"></i>
    </div>
  </div>
  <!-- END Timeline -->
</#macro>

<#macro dataAccessAgreementsNote>
  <#list agreements as agreement>
    <#if agreement.applicant == user.username>
      <p>
        <a class="btn btn-outline-secondary" href="${contextPath}/data-access-agreement-form/${agreement.id}">
          <i class="fas fa-circle nav-icon text-${statusColor(agreement.status.toString())}"
             title="<@message agreement.status.toString()/>"></i>
            <@message "agreement-current-user"/>
        </a>
      </p>
    </#if>
  </#list>

  <div class="mb-3">
    <#if agreementsRejected?size gt 0>
      <div class="alert alert-danger">
        <p><i class="icon fas fa-ban"></i>
          <#if isDAO || isAdministrator>
            <@message "agreements-rejected-dao-text"/>
          <#else>
            <@message "agreements-rejected-text"/>
          </#if>
        </p>
        <p>
          <a href="${contextPath}/data-access-comments/${dar.id}"><@message "send-message"/> <i
              class="fas fa-arrow-circle-right ml-1"></i></a>
        </p>
      </div>
    <#elseif agreementsOpened?size gt 0>
      <div class="alert alert-info">
        <p><i class="icon fas fa-info"></i> <@message "agreements-opened-text"/></p>
      </div>
    <#elseif agreementsApproved?size == agreements?size>
      <div class="alert alert-success">
        <p><i class="icon fas fa-check"></i> <@message "agreements-approved-text"/></p>
      </div>
    </#if>
  </div>
</#macro>

<#macro dataAccessCollaborators>
  <#if collaborators?has_content>
    <div class="table-responsive">
      <table id="collaborators" class="table table-bordered table-striped">
        <thead>
        <tr>
          <th><@message "email"/></th>
          <th><@message "status"/></th>
          <th><@message "last-update"/></th>
          <#if accessConfig.agreementEnabled>
            <th><@message "agreement"/></th>
          </#if>
          <#if permissions?seq_contains("ADD_COLLABORATORS") || permissions?seq_contains("DELETE_COLLABORATORS")>
            <th></th>
          </#if>
        </tr>
        </thead>
        <tbody>
        <#list collaborators as collaborator>
          <tr>
            <td>
              <a href="mailto:${collaborator.email}">${collaborator.fullName}</a>
            </td>
            <td>
              <#if collaborator.invitationPending>
                  <@message "invited"/>
              <#else>
                  <@message "accepted"/>
              </#if>
            </td>
            <td data-sort="${collaborator.lastModifiedDate.toString(datetimeFormat)}" class="moment-datetime">${collaborator.lastModifiedDate.toString(datetimeFormat)}</td>
            <#if accessConfig.agreementEnabled>
              <td>
                  <#list agreements as agreement>
                      <#if collaborator.principal?? && agreement.applicant == collaborator.principal>
                        <a href="${contextPath}/data-access-agreement-form/${agreement.id}">
                          <i class="fas fa-circle nav-icon text-${statusColor(agreement.status.toString())}"
                             title="<@message agreement.status.toString()/>"></i>
                        </a>
                      </#if>
                  </#list>
              </td>
            </#if>
            <#if permissions?seq_contains("ADD_COLLABORATORS") || permissions?seq_contains("DELETE_COLLABORATORS")>
              <td>
                <div class="btn-group">
                  <button type="button" class="btn text-muted" data-toggle="dropdown" aria-expanded="false">
                    <i class="fa fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul class="dropdown-menu" style="">
                    <#if permissions?seq_contains("ADD_COLLABORATORS") && collaborator.invitationPending>
                      <li><a class="dropdown-item" href="#" onclick="DataAccessService.reinviteCollaborator('${dar.id}', '${collaborator.email}', '<@message "invitation-resent"/>')"><i class="fa fa-paper-plane mr-2"></i> <@message "invite"/></a></li>
                    </#if>
                    <#if permissions?seq_contains("DELETE_COLLABORATORS")>
                      <li><a class="dropdown-item" href="#" onclick="$('#collaborator-to-delete').text('${collaborator.email}')" data-toggle="modal" data-target="#modal-collaborator-delete"><i class="fa fa-trash mr-2"></i> <@message "remove"/></a></li>
                    </#if>
                  </ul>
                </div>
              </td>
            </#if>
          </tr>
        </#list>
        </tbody>
      </table>
    </div>
  <#else >
    <p><@message "no-collaborators"/></p>
  </#if>
</#macro>
