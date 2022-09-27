<!-- Data access (private) comments page macros -->

<#macro commentsTimeline isPrivate="false">
  <div class="timeline">

    <#list items as item>

      <#assign currentDay = item.date.toString(dateFormat)/>
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
            <i class="fas fa-user-shield bg-warning"></i>
          <#else>
            <i class="fas fa-user bg-blue"></i>
          </#if>

          <div class="timeline-item">
            <#if item.canRemove>
              <a class="time text-danger" onclick="DataAccessService.deleteComment('${dar.id}', '${comment.id}', ${isPrivate})"><i class="fa fa-trash"></i></a>
            </#if>
            <span class="time"><i class="fas fa-clock"></i> <span
                      class="moment-datetime">${item.date.toString(datetimeFormat)}</span></span>
            <h3 class="timeline-header bg-light">${authors[item.author].fullName}</h3>

            <div class="timeline-body marked">
                ${comment.message}
            </div>
          </div>
        <#elseif item.isEventItem()>
          <#assign event = item.event/>
          <i class="fas fa-bolt bg-${statusColor(event.status.toString())}"></i>

          <div class="timeline-item">
            <span class="time"><i class="fas fa-clock"></i> <span
                class="moment-datetime">${item.date.toString(datetimeFormat)}</span></span>
            <h3 class="timeline-header bg-light">${authors[item.author].fullName}</h3>

            <div class="timeline-body">
              <#if event.amendment>
                <a href="${contextPath}/data-access-amendment-form/${event.form.id}"><i class="fas fa-file-import"></i> ${event.form.id}</a>
              <#elseif event.feasibility>
                <a href="${contextPath}/data-access-feasibility-form/${event.form.id}"><i class="far fa-question-circle"></i> ${event.form.id}</a>
              <#elseif event.agreement>
                <a href="${contextPath}/data-access-agreement-form/${event.form.id}"><i class="fa fa-gavel"></i> ${event.form.id}</a>
              <#else>
                <a href="${contextPath}/data-access-form/${event.form.id}"><i class="fas fa-book"></i> ${event.form.id}</a>
              </#if>
              <i class="fas fa-arrow-right ml-2 mr-2"></i>
              <@message event.status.toString()/>
            </div>
          </div>
        </#if>

      </div>
      <!-- END timeline item -->
    </#list>

    <div>
      <i class="fas fa-clock bg-gray"></i>
        <#if !items?? || items?size == 0>
          <span class="timeline-block"><@message "no-comments"/></span>
        </#if>
    </div>
  </div>
</#macro>
