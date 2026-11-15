<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.global.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<script>
  $(function () {
    <#if showProjectFiles>
      makeFilesVue('#project-files-app', {
        type: 'project',
        id: '${project.id?js_string}',
        basePath: '',
        path: '/',
        folder: {},
        tr: {
          "item": "${messageJs("item")}",
          "items": "${messageJs("items")}",
          "download": "${messageJs("download")}",
          "name": "${messageJs("name")}",
          "description": "${messageJs("description")}",
          "size": "${messageJs("size")}",
          "actions": "${messageJs("actions")}"
        },
        locale: '${.lang}',
        contextPath: '${contextPath}'
      });
    </#if>
  });
</script>
</#outputformat>
