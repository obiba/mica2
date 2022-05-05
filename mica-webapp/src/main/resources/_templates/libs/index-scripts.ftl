<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<script>
  $(function () {
    <#if config.openAccess || user??>
      MetricsService.getStats((response) => {
        $('#network-hits').text(numberFormatter.format(response.Network));
        $('#study-hits').text(numberFormatter.format(response.Study));
        $('#initiative-hits').text(numberFormatter.format(response.HarmonizationStudy));
        $('#dataset-hits').text(numberFormatter.format(response.StudyDataset));
        $('#protocols-hits').text(numberFormatter.format(response.HarmonizationDataset));
        $('#variable-hits').text(numberFormatter.format(response.DatasetVariable));
      });

    </#if>
  });
</script>
