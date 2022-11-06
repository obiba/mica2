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
            <td data-sort="${collaborator.lastModifiedDate.toString()}" class="moment-datetime">${collaborator.lastModifiedDate.toString()}</td>
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

<#macro dataAccessInfoBox>
  <#assign status = dar.status.toString()/>
  <#if preliminary?? && preliminary.status.toString() == "REJECTED">
    <#assign status = "REJECTED"/>
  </#if>

  <#if status == "OPENED">
    <#assign boxIcon = "fa fa-pen"/>
    <#assign boxProgress = "10"/>
    <#assign boxText = "data-access-progress-opened"/>
  <#elseif status == "APPROVED">
    <#assign boxIcon = "fa fa-check"/>
    <#assign boxProgress = "100"/>
    <#assign boxText = "data-access-progress-approved"/>
  <#elseif status == "REJECTED">
    <#assign boxIcon = "fa fa-ban"/>
    <#assign boxProgress = "100"/>
    <#assign boxText = "data-access-progress-rejected"/>
  <#elseif status == "SUBMITTED">
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "30"/>
    <#assign boxText = "data-access-progress-submitted"/>
  <#elseif status == "REVIEWED">
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "50"/>
    <#assign boxText = "data-access-progress-reviewed"/>
  <#elseif status == "CONDITIONALLY_APPROVED">
    <#assign boxIcon = "fa fa-pen"/>
    <#assign boxProgress = "80"/>
    <#assign boxText = "data-access-progress-conditionally-approved"/>
  <#else>
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "50"/>
    <#assign boxText = ""/>
  </#if>

  <div class="info-box bg-${statusColor(status)}">
    <span class="info-box-icon"><i class="${boxIcon}"></i></span>

    <div class="info-box-content">
      <span class="info-box-text"><@message "status"/></span>
      <span class="info-box-number"><@message status/></span>

      <div class="progress">
        <div class="progress-bar" style="width: ${boxProgress}%"></div>
      </div>
      <span class="progress-description">
        <small><@message boxText/></small>
      </span>
    </div>
    <!-- /.info-box-content -->
  </div>
  <!-- /.info-box -->
</#macro>

<#macro dataAccessLastFeasibilityInfoBox>
  <#if lastFeasibility.status.toString() == "OPENED">
    <#assign boxIcon = "fa fa-pen"/>
    <#assign boxProgress = "10"/>
    <#assign boxText = "data-access-feasibility-progress-opened"/>
  <#elseif lastFeasibility.status.toString() == "APPROVED">
    <#assign boxIcon = "fa fa-check"/>
    <#assign boxProgress = "100"/>
    <#assign boxText = "data-access-feasibility-progress-approved"/>
  <#elseif lastFeasibility.status.toString() == "REJECTED">
    <#assign boxIcon = "fa fa-ban"/>
    <#assign boxProgress = "100"/>
    <#assign boxText = "data-access-feasibility-progress-rejected"/>
  <#elseif lastFeasibility.status.toString() == "SUBMITTED">
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "30"/>
    <#assign boxText = "data-access-feasibility-progress-submitted"/>
  <#else>
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "50"/>
    <#assign boxText = ""/>
  </#if>

  <div class="info-box bg-${statusColor(lastFeasibility.status.toString())}">
    <span class="info-box-icon"><i class="${boxIcon}"></i></span>

    <div class="info-box-content">
      <span class="info-box-text"><@message "last-feasibility-status"/> - <a class="text-white" href="${contextPath}/data-access-feasibility-form/${lastFeasibility.id}">${lastFeasibility.id}</a></span>
      <span class="info-box-number"><@message lastFeasibility.status.toString()/></span>

      <div class="progress">
        <div class="progress-bar" style="width: ${boxProgress}%"></div>
      </div>
      <span class="progress-description">
        <small><@message boxText/></small>
      </span>
    </div>
    <!-- /.info-box-content -->
  </div>
  <!-- /.info-box -->
</#macro>

<#macro dataAccessLastAmendmentInfoBox>
  <#if lastAmendment.status.toString() == "OPENED">
    <#assign boxIcon = "fa fa-pen"/>
    <#assign boxProgress = "10"/>
    <#assign boxText = "data-access-amendment-progress-opened"/>
  <#elseif lastAmendment.status.toString() == "APPROVED">
    <#assign boxIcon = "fa fa-check"/>
    <#assign boxProgress = "100"/>
    <#assign boxText = "data-access-amendment-progress-approved"/>
  <#elseif lastAmendment.status.toString() == "REJECTED">
    <#assign boxIcon = "fa fa-ban"/>
    <#assign boxProgress = "100"/>
    <#assign boxText = "data-access-amendment-progress-rejected"/>
  <#elseif lastAmendment.status.toString() == "SUBMITTED">
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "30"/>
    <#assign boxText = "data-access-amendment-progress-submitted"/>
  <#elseif lastAmendment.status.toString() == "REVIEWED">
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "50"/>
    <#assign boxText = "data-access-amendment-progress-reviewed"/>
  <#elseif lastAmendment.status.toString() == "CONDITIONALLY_APPROVED">
    <#assign boxIcon = "fa fa-pen"/>
    <#assign boxProgress = "80"/>
    <#assign boxText = "data-access-amendment-progress-conditionally-approved"/>
  <#else>
    <#assign boxIcon = "far fa-clock"/>
    <#assign boxProgress = "50"/>
    <#assign boxText = ""/>
  </#if>

  <div class="info-box bg-${statusColor(lastAmendment.status.toString())}">
    <span class="info-box-icon"><i class="${boxIcon}"></i></span>

    <div class="info-box-content">
      <span class="info-box-text"><@message "last-amendment-status"/> - <a class="text-white" href="${contextPath}/data-access-amendment-form/${lastAmendment.id}">${lastAmendment.id}</a></span>
      <span class="info-box-number"><@message lastAmendment.status.toString()/></span>

      <div class="progress">
        <div class="progress-bar" style="width: ${boxProgress}%"></div>
      </div>
      <span class="progress-description">
        <small><@message boxText/></small>
      </span>
    </div>
    <!-- /.info-box-content -->
  </div>
  <!-- /.info-box -->
</#macro>

<#macro dataAccessNotes>
  <#if dar.archived>
    <h4><@message "archived-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "archived-applicant-text"/></p>
      <#else>
        <p><@message "archived-dao-text"/></p>
      </#if>

  <#elseif dar.status == "CONDITIONALLY_APPROVED">

    <h4><@message "conditionally-approved-title"/></h4>
    <#if user.username == dar.applicant>
      <p><@message "conditionally-approved-applicant-text"/></p>
      <div>
        <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
          <i class="fas fa-pen"></i> <@message "application-form"/>
        </a>
      </div>
    <#else>
      <p><@message "conditionally-approved-dao-text"/></p>
    </#if>

  <#elseif dar.status == "SUBMITTED">

    <h4><@message "submitted-title"/></h4>
    <#if user.username == dar.applicant>
      <p><@message "submitted-applicant-text"/></p>
    <#else>
      <p><@message "submitted-dao-text"/></p>
      <div>
        <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
          <i class="fas fa-pen"></i> <@message "application-form"/>
        </a>
      </div>
    </#if>

  <#elseif dar.status == "REVIEWED">

    <h4><@message "reviewed-title"/></h4>
    <#if user.username == dar.applicant>
      <p><@message "reviewed-applicant-text"/></p>
    <#else>
      <p><@message "reviewed-dao-text"/></p>
      <div>
        <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
          <i class="fas fa-pen"></i> <@message "application-form"/>
        </a>
      </div>
    </#if>

  <#elseif dar.status == "APPROVED">
    <#if accessConfig.agreementEnabled>
      <h4><@message "agreements-title"/></h4>
      <p><@message "agreements-text"/></p>
        <@dataAccessAgreementsNote/>
    </#if>

    <#if dataAccessReportTimelineEnabled && reportTimeline.endDate??>
      <h4><@message "report-timeline-title"/></h4>
      <p><@message "report-timeline-text"/></p>
      <@dataAccessTimeline dar=dar reportTimeline=reportTimeline/>
    <#else>
      <h4><@message "approved-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "approved-applicant-text"/></p>
      <#else>
        <p><@message "approved-dao-text"/></p>
        <div>
          <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
            <i class="fas fa-eye"></i> <@message "application-form"/>
          </a>
        </div>
      </#if>
    </#if>

  <#elseif dar.status == "REJECTED">

    <h4><@message "rejected-title"/></h4>
    <#if user.username == dar.applicant>
      <p><@message "rejected-applicant-text"/></p>
    <#else>
      <p><@message "rejected-dao-text"/></p>
      <div>
        <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
          <i class="fas fa-eye"></i> <@message "application-form"/>
        </a>
      </div>
    </#if>

  <#elseif preliminary?? && preliminary.status == "CONDITIONALLY_APPROVED">

    <h4><@message "conditionally-approved-preliminary-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "conditionally-approved-preliminary-applicant-text"/></p>
        <div>
          <a href="${contextPath}/data-access-preliminary-form/${dar.id}" class="btn btn-primary" >
            <i class="fas fa-pen"></i> <@message "application-form"/>
          </a>
        </div>
      <#else>
        <p><@message "conditionally-approved-preliminary-dao-text"/></p>
      </#if>

  <#elseif preliminary?? && preliminary.status == "SUBMITTED">

    <h4><@message "submitted-preliminary-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "submitted-preliminary-applicant-text"/></p>
      <#else>
        <p><@message "submitted-preliminary-dao-text"/></p>
        <div>
          <a href="${contextPath}/data-access-preliminary-form/${dar.id}" class="btn btn-primary" >
            <i class="fas fa-pen"></i> <@message "preliminary-application-form"/>
          </a>
        </div>
      </#if>

  <#elseif preliminary?? && preliminary.status == "REVIEWED">

    <h4><@message "reviewed-preliminary-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "reviewed-preliminary-applicant-text"/></p>
      <#else>
        <p><@message "reviewed-preliminary-dao-text"/></p>
        <div>
          <a href="${contextPath}/data-access-preliminary-form/${dar.id}" class="btn btn-primary" >
            <i class="fas fa-pen"></i> <@message "preliminary-application-form"/>
          </a>
        </div>
      </#if>

  <#elseif preliminary?? && preliminary.status == "REJECTED">

    <h4><@message "rejected-preliminary-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "rejected-preliminary-applicant-text"/></p>
      <#else>
        <p><@message "rejected-preliminary-dao-text"/></p>
        <div>
          <a href="${contextPath}/data-access-preliminary-form/${preliminary.id}" class="btn btn-primary" >
            <i class="fas fa-eye"></i> <@message "preliminary-application-form"/>
          </a>
        </div>
      </#if>

  <#elseif preliminary?? && preliminary.status == "OPENED">

    <h4><@message "opened-preliminary-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "opened-preliminary-applicant-text"/></p>
        <div>
          <a href="${contextPath}/data-access-preliminary-form/${dar.id}" class="btn btn-primary" >
            <i class="fas fa-pen"></i> <@message "preliminary-application-form"/>
          </a>
        </div>
      <#else>
        <p><@message "opened-dao-text"/></p>
      </#if>

  <#elseif dar.status == "OPENED">

    <h4><@message "opened-title"/></h4>
      <#if user.username == dar.applicant>
        <p><@message "opened-applicant-text"/></p>
        <div>
          <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
            <i class="fas fa-pen"></i> <@message "application-form"/>
          </a>
        </div>
      <#else>
        <p><@message "opened-dao-text"/></p>
      </#if>


  </#if>
</#macro>
