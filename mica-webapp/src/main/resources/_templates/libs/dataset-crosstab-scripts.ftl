<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

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
      "all": "<@message "all"/>"
    }
  };
</script>

<!-- Mica crosstabs and dependencies -->
<script src="${assetsPath}/libs/node_modules/select2/dist/js/select2.full.js"></script>
<script src="${assetsPath}/libs/node_modules/select2/dist/js/i18n/${.lang}.js"></script>
<script src="${assetsPath}/js/mica-dataset-crosstab.js"></script>
