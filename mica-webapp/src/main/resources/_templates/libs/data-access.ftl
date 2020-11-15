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

          <#if user.username == dar.applicant>
            <span><@message "start-date-applicant-text"/></span>
          <#else>
            <p><@message "start-date-dao-text"/></p>
            <div>
              <button type="button" class="btn btn-primary btn-sm" data-toggle="modal" data-target="#modal-start-date">
                <i class="fas fa-clock"></i> <@message "start-date"/>
              </button>
            </div>
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
              <#if user.username == dar.applicant>
                <span><@message "intermediate-date-applicant-text"/></span>
              <#else>
                <span><@message "intermediate-date-dao-text"/></span>
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

          <#if user.username == dar.applicant>
            <span><@message "end-date-applicant-text"/></span>
          <#else>
            <span><@message "end-date-dao-text"/></span>
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
