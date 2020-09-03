
<script>
  const Mica = {
    contextPath: "${contextPath}",
    type: "${type}".toLowerCase(),
    dataset: "${dataset.id}",
    var1: ${variable1!"undefined"},
    var2: ${variable2!"undefined"},
    locale: "${.lang}",
    tr: {
      "chi-squared-test": "<@message "chi-squared-test"/>",
      "n-total": "<@message "n-total"/>",
      "min": "<@message "min"/>",
      "max": "<@message "max"/>",
      "mean": "<@message "mean"/>",
      "stdDev": "<@message "stdDev"/>",
      "all": "<@message "all"/>",
    }
  };
</script>

<!-- Mica crosstabs and dependencies -->
<script src="${adminLTEPath}/plugins/select2/js/select2.js"></script>
<script src="${adminLTEPath}/plugins/select2/js/i18n/${.lang}.js"></script>
<script src="${assetsPath}/js/mica-dataset-crosstab.js"></script>
