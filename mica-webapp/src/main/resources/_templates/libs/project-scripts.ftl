<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.global.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<script>
  $(function () {
    <#if showProjectFiles>
      makeFilesVue('#project-files-app', {
        type: 'project',
        id: '${project.id}',
        basePath: '',
        path: '/',
        folder: {},
        tr: {
          "item": "<@message "item"/>",
          "items": "<@message "items"/>",
          "download": "<@message "download"/>",
          "name": "<@message "name"/>",
          "description": "<@message "description"/>",
          "size": "<@message "size"/>",
          "actions": "<@message "actions"/>"
        },
        locale: '${.lang}',
        contextPath: '${contextPath}'
      });
    </#if>
  });
</script>
