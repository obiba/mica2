<!-- Data access (private) comments page macros -->

<#macro commentsTimeline isPrivate="false">
  <div class="timeline">

      <#list comments as comment>

          <#assign currentDay = comment.createdDate.toString(dateFormat)/>
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
          <#if comment.createdBy == "administrator" || authors[comment.createdBy].groups?? && (authors[comment.createdBy].groups?seq_contains("mica-administrator") || authors[comment.createdBy].groups?seq_contains("mica-data-access-officer"))>
            <i class="fas fa-user-shield bg-warning"></i>
          <#else>
            <i class="fas fa-user bg-blue"></i>
          </#if>

          <div class="timeline-item">
            <#if !comment?has_next && (user.username == comment.createdBy || isAdministrator)>
              <a class="time text-danger" onclick="micajs.dataAccess.deleteComment('${dar.id}', '${comment.id}', ${isPrivate})"><i class="fa fa-trash"></i></a>
            </#if>
            <span class="time"><i class="fas fa-clock"></i> <span
                      class="moment-datetime">${comment.createdDate.toString(datetimeFormat)}</span></span>
            <h3 class="timeline-header">${authors[comment.createdBy].fullName}</h3>

            <div class="timeline-body marked">
                ${comment.message}
            </div>
          </div>
        </div>
        <!-- END timeline item -->
      </#list>

    <div>
      <i class="fas fa-clock bg-gray"></i>
        <#if !comments?? || comments?size == 0>
          <span class="timeline-block"><@message "no-comments"/></span>
        </#if>
    </div>
  </div>
</#macro>
