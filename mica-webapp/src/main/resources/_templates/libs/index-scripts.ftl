<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<script>
  $(function () {
    <#if config.openAccess || user??>
      QueryService.getCounts('studies', {}, function (stats) {
        $('#network-hits').text(new Intl.NumberFormat('${.lang}').format(stats.networkResultDto.totalHits));
        $('#study-hits').text(new Intl.NumberFormat('${.lang}').format(stats.studyResultDto.totalHits));
        $('#dataset-hits').text(new Intl.NumberFormat('${.lang}').format(stats.datasetResultDto.totalHits));
        $('#variable-hits').text(new Intl.NumberFormat('${.lang}').format(stats.variableResultDto.totalHits));
      });
    </#if>
  });
</script>
