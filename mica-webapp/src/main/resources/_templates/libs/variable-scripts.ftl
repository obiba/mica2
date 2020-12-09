<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-charts.js"></script>

<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<!-- Mica variable and dependencies -->
<script src="${adminLTEPath}/plugins/select2/js/select2.js"></script>
<script src="${adminLTEPath}/plugins/select2/js/i18n/${.lang}.js"></script>
<script src="${assetsPath}/js/mica-variable.js"></script>

<script>
  const Mica = {
    contextPath: "${contextPath}",
    currentLanguage: '${.lang}',
    variableId: '${variable.id}',
    categories: {},
    backgroundColors: ['${colors?join("', '")}'],
    barChartBorderColor: '${barChartBorderColor}',
    barChartBackgroundColor: '${barChartBackgroundColor}',
    dataTableOpts: {
      "paging": false,
      "lengthChange": false,
      "searching": false,
      "ordering": true,
      "order": [[1, "desc"]],
      "info": false,
      "autoWidth": true,
      "language": {
        "url": "${assetsPath}/i18n/datatables.${.lang}.json"
      }
    },
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

  // categories
  <#if variable.categories??>
  <#list variable.categories as cat>
  Mica.categories['${cat.name}'] = escapeQuotes("${localize(cat.attributes.label)}");
  </#list>
  </#if>

  // cart
  <#if cartEnabled>
    const onVariablesCartGet = function(cart) {
      VariablesSetService.contains(cart, '${variableCartId}', function() {
        $('#cart-remove').show();
      }, function () {
        $('#cart-add').show();
      });
    };
    const onVariablesCartAdd = function(id) {
      VariablesSetService.addToCart([id], function(cart, oldCart) {
        VariablesSetService.showCount('#cart-count', cart, '${.lang}');
        if (cart.count === oldCart.count) {
          MicaService.toastInfo("<@message "sets.cart.no-variable-added"/>");
        } else {
          MicaService.toastSuccess("<@message "variable-added-to-cart"/>");
        }
        $('#cart-add').hide();
        $('#cart-remove').show();
      });
    };
    const onVariablesCartRemove = function(id) {
      VariablesSetService.removeFromCart([id], function(cart, oldCart) {
        VariablesSetService.showCount('#cart-count', cart, '${.lang}');
        // TODO toast cart update
        if (cart.count === oldCart.count) {
          MicaService.toastInfo("<@message "sets.cart.no-variable-removed"/>");
        } else {
          MicaService.toastSuccess("<@message "variable-removed-from-cart"/>");
        }
        $('#cart-remove').hide();
        $('#cart-add').show();
      });
    };
  </#if>

  $(function () {

    <#if type == "Dataschema">
      makeHarmonizedVariablesTable();
    </#if>

    <!-- Files -->
    <#if showStudyDCEFiles && study?? && population?? && dce??>
      makeFilesVue('#study-${population.id}-${dce.id}-files-app', {
        type: 'individual-study',
        id: '${study.id}',
        basePath: '/population/${population.id}/data-collection-event/${dce.id}',
        path: '/',
        folder: {},
        tr: {
          "item": "<@message "item"/>",
          "items": "<@message "items"/>",
          "download": "<@message "download"/>"
        },
        locale: '${.lang}'
      });
    </#if>

    <#if user?? || !config.variableSummaryRequiresAuthentication>
      makeSummary();
    </#if>

  });
</script>
