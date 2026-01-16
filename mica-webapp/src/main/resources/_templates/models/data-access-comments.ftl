<!-- Data access (private) comments page macros -->

<#macro commentsTimeline isPrivate="false">
  <div class="timeline">

    <#list items as item>
      <#if !item.isEventItem() || showDataAccessEventsInComments?seq_contains(item.event.type)>

        <#assign currentDay = item.date.toString()/>
        <#if !day?? || day != currentDay>
            <#assign day = currentDay/>
          <!-- timeline time label -->
          <div class="time-label">
            <span class="bg-red moment-date">${day}</span>
          </div>
          <!-- /.timeline-label -->
        </#if>

        <!-- timeline item -->
        <div>
          <#if item.isCommentItem()>
            <#assign comment = item.comment/>
            <#if item.author == "administrator" || authors[item.author].groups?? && (authors[item.author].groups?seq_contains("mica-administrator") || authors[item.author].groups?seq_contains("mica-data-access-officer"))>
              <div class="timeline-icon bg-warning">
                <i class="fa-solid fa-user-shield"></i>
              </div>
            <#else>
              <div class="timeline-icon bg-blue">
                <i class="fa-solid fa-user"></i>
              </div>
            </#if>

            <div class="timeline-item timeline-comment-item">
              <#if item.canRemove>
                <a class="time text-danger" onclick="DataAccessService.deleteComment('${dar.id}', '${comment.id}', ${isPrivate})"><i class="fa fa-trash"></i></a>
              </#if>
              <span class="time"><i class="fa-solid fa-clock"></i> <span
                        class="moment-datetime">${item.date.toString()}</span></span>
              <h3 class="timeline-header bg-light">${authors[item.author].fullName}</h3>

              <div class="timeline-body marked">
                  ${comment.message}
              </div>
            </div>
          <#elseif item.isEventItem()>
            <#assign event = item.event/>
            <div class="timeline-icon bg-${statusColor(event.status.toString())}">
              <i class="fa-solid fa-bolt"></i>
            </div>

            <div class="timeline-item timeline-event-item">
              <span class="time"><i class="fa-solid fa-clock"></i> <span
                  class="moment-datetime">${item.date.toString()}</span></span>
              <h3 class="timeline-header bg-light">${authors[item.author].fullName}</h3>

              <div class="timeline-body">
                <#if event.amendment>
                  <a href="${contextPath}/data-access-amendment-form/${event.form.id}"><i class="fa-solid fa-file-import"></i> ${event.form.id}</a>
                <#elseif event.feasibility>
                  <a href="${contextPath}/data-access-feasibility-form/${event.form.id}"><i class="fa-regular fa-question-circle"></i> ${event.form.id}</a>
                <#elseif event.preliminary>
                  <a href="${contextPath}/data-access-preliminary-form/${event.form.id}"><i class="fa-regular fa-play-circle"></i> ${event.form.id}</a>
                <#elseif event.agreement>
                  <a href="${contextPath}/data-access-agreement-form/${event.form.id}"><i class="fa fa-gavel"></i> ${event.form.id}</a>
                <#else>
                  <a href="${contextPath}/data-access-form/${event.form.id}"><i class="fa-solid fa-book"></i> ${event.form.id}</a>
                </#if>
                <i class="fa-solid fa-arrow-right ms-2 me-2"></i>
                <@message event.status.toString()/>
              </div>
            </div>
          </#if>

        </div>
        <!-- END timeline item -->

      </#if>

    </#list>

    <div>
      <div class="timeline-icon bg-gray">
        <i class="fa-solid fa-clock"></i>
      </div>
        <#if !items?? || items?size == 0>
          <span class="timeline-block"><@message "no-comments"/></span>
        </#if>
    </div>
  </div>
</#macro>
