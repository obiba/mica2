<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<script>
  $(function () {
    <#if config.openAccess || user??>
      QueryService.getCounts('studies', {}, function (stats) {
        $('#network-hits').text(numberFormatter.format(stats.networkResultDto.totalHits));
        $('#study-hits').text(numberFormatter.format(stats.studyResultDto.totalHits));
        $('#dataset-hits').text(numberFormatter.format(stats.datasetResultDto.totalHits));
        $('#variable-hits').text(numberFormatter.format(stats.variableResultDto.totalHits));
      });
    </#if>
  });
</script>
