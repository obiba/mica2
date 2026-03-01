<!-- Timeline -->
<script src="${contextPath}/bower_components/d3/d3.js"></script>
<script src="${contextPath}/bower_components/mica-study-timeline/dist/mica-study-timeline.js"></script>

<!-- ChartJS -->
<script src="${assetsPath}/libs/node_modules/chart.js/dist/chart.umd.js"></script>
<script src="${assetsPath}/js/mica-charts.js"></script>
<script src="${assetsPath}/libs/node_modules/plotly.js-dist-min/plotly.min.js"></script>
<!-- Select2 -->
<script src="${assetsPath}/libs/node_modules/select2/dist/js/select2.full.js"></script>
<script src="${assetsPath}/libs/node_modules/select2/dist/js/i18n/${.lang}.js"></script>

<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.global.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<script>
  // cart
  <#if cartEnabled && studiesCartEnabled>
  const onStudiesCartGet = function(cart) {
    StudiesSetService.contains(cart, '${study.id}', function() {
      $('#cart-remove').show();
    }, function () {
      $('#cart-add').show();
    });
  };
  const onStudiesCartAdd = function(id) {
    StudiesSetService.addToCart([id], function(cart, oldCart) {
      StudiesSetService.showCount('#cart-count', cart, '${.lang}');
      if (cart.count === oldCart.count) {
        MicaService.toastInfo("<@message "sets.cart.no-study-added"/>");
      } else {
        MicaService.toastSuccess("<@message "study-added-to-cart"/>");
      }
      $('#cart-add').hide();
      $('#cart-remove').show();
    });
  };
  const onStudiesCartRemove = function(id) {
    StudiesSetService.removeFromCart([id], function(cart, oldCart) {
      StudiesSetService.showCount('#cart-count', cart, '${.lang}');
      // TODO toast cart update
      if (cart.count === oldCart.count) {
        MicaService.toastInfo("<@message "sets.cart.no-study-removed"/>");
      } else {
        MicaService.toastSuccess("<@message "study-removed-from-cart"/>");
      }
      $('#cart-remove').hide();
      $('#cart-add').show();
    });
  };
  </#if>

  const Mica = { options: {} };
  <#if study.populations?? && study.populations?size != 0>
    <#list study.populationsSorted as population>
      <#if type == "Individual">
        <#if population.dataCollectionEvents?? && population.dataCollectionEvents?size != 0>
          <#list population.dataCollectionEventsSorted as dce>
            Mica.options['${study.id}:${population.id}:${dce.id}'] =
                    escapeQuotes(<#if study.populations?size == 1>"${localize(dce.name)}"<#else>"${localize(population.name)} / ${localize(dce.name)}"</#if>);
          </#list>
        </#if>
      <#else>
        Mica.options['${study.id}:${population.id}:.'] = escapeQuotes("${localize(population.name)}");
      </#if>
    </#list>
  </#if>

  const renderVariablesClassifications = function(key) {
    $('#loadingClassifications').hide();
    const chartsElem = $('#chartsContainer');
    chartsElem.children().remove();
    if (Mica.variablesCoverage) {
      Mica.variablesCoverage.forEach(chartData => {
        chartsElem.append('<h5>' + chartData.title + '</h5>');
        chartsElem.append('<p>' + chartData.subtitle + '</p>');
        chartsElem.append('<div id="bar-graph-' + chartData.taxonomy + '" class="mb-4"></div>');

        const chartConfig = makeVariablesClassificationsChartSettings(chartData, {
          key: key,
          label: "<@message "variables"/>",
          borderColor: '${barChartBorderColor}',
          backgroundColor: '${barChartBackgroundColor}',
          useColorsArray: ${useColorsArrayForClassificationsChart?c}
        });

        Plotly.newPlot("bar-graph-" + chartData.taxonomy, chartConfig.data, chartConfig.layout, {responsive: true});
      });
      $('#classificationsContainer').show();

      Mica.variablesCoverage.forEach(chartData => {
        let contentLength = Math.max(Mica.variablesCoverage.filter(c => c.taxonomy === chartData.taxonomy)[0].vocabularies.length, 7);
        let contentWidth = $('#classificationsContainer #bar-graph-' + chartData.taxonomy).width();

        Plotly.relayout("bar-graph-" + chartData.taxonomy, {width: contentWidth, height: (2*1.42857)*12*contentLength});
      });
    } else {
      $('#noVariablesClassifications').show();
    }
  };

  const initSelectBucket = function() {
    // scan for bucket ids
    if (Mica.variablesCoverage) {
      const buckets = [];
      Mica.variablesCoverage.forEach(chartData => {
        Object.keys(chartData.itemCounts).forEach(k => {
          if (k !== '_all' && !buckets.includes(k)) {
            buckets.push(k);
          }
        });
      });
      const selectBucketElem = $('#select-bucket');
      selectBucketElem.on('change', function (e) {
        $('#classificationsContainer').hide();
        renderVariablesClassifications($(this).val());
      });
      buckets.forEach(k => {
        let newOption = new Option(Mica.options[k], k, false, false);
        selectBucketElem.append(newOption);
      });
    }
  };

  $(function () {
    let options = Object.assign({}, dataTablesDefaultOpts, {
      autoWidth: false,
      columnDefs: [
        { "type": "num", "targets": 0, "visible": false }
      ]
    });
    <#list study.populations as pop>
      $("#population-${pop.id}-dces").DataTable(options);
    </#list>

    QueryService.getCounts('studies', { query: "study(in(Mica_study.id,${study.id}))" }, function(stats) {
      $('#network-hits').text(numberFormatter.format(stats.networkResultDto.totalHits));
      $('#dataset-hits').text(numberFormatter.format(stats.datasetResultDto.totalHits));
      $('#variable-hits').text(numberFormatter.format(stats.variableResultDto.totalHits));
    });

    <#if timelineData??>
      let timelineData = ${timelineData};
      new $.MicaTimeline(new $.StudyDtoParser('${.lang}')).create('#timeline', timelineData).addLegend();
    </#if>

    <!-- Files -->
    const filesTr = {
      "item": "<@message "item"/>",
      "items": "<@message "items"/>",
      "download": "<@message "download"/>",
      "name": "<@message "name"/>",
      "description": "<@message "description"/>",
      "size": "<@message "size"/>",
      "actions": "<@message "actions"/>"
    };
    <#if showStudyFiles>
      makeFilesVue('#study-files-app', {
        type: '${type?lower_case}-study',
        id: '${study.id}',
        basePath: '',
        path: '/',
        folder: {},
        tr: filesTr,
        locale: '${.lang}',
        contextPath: '${contextPath}'
      }, function(file) {
        return !(file.type === 'FOLDER' && file.name === 'population');
      });
    </#if>

    <#if study.populations?? && study.populations?size != 0>
      <#list study.populations as population>
        <#if showStudyPopulationFiles>
          makeFilesVue('#study-${population.id}-files-app', {
            type: '${type?lower_case}-study',
            id: '${study.id}',
            basePath: '/population/${population.id}',
            path: '/',
            folder: {},
            tr: filesTr,
            locale: '${.lang}',
            contextPath: '${contextPath}'
          }, function(file) {
            return !(file.type === 'FOLDER' && file.name === 'data-collection-event');
          });
        </#if>
        <#if type == "Individual" && showStudyDCEFiles>
          <#if population.dataCollectionEvents?? && population.dataCollectionEvents?size != 0>
            <#list population.dataCollectionEventsSorted as dce>
              makeFilesVue('#study-${population.id}-${dce.id}-files-app', {
                type: '${type?lower_case}-study',
                id: '${study.id}',
                basePath: '/population/${population.id}/data-collection-event/${dce.id}',
                path: '/',
                folder: {},
                tr: filesTr,
                locale: '${.lang}',
                contextPath: '${contextPath}'
              });
            </#list>
          </#if>
        </#if>
      </#list>
    </#if>

    <!-- Variables classifications -->
    <#if studyVariablesClassificationsTaxonomies?? && studyVariablesClassificationsTaxonomies?size gt 0>
      const taxonomies = ['${studyVariablesClassificationsTaxonomies?join("', '")}'];
      $('#classificationsContainer').hide();
      StudyService.getVariablesCoverage('${study.id}', taxonomies, '${.lang}', function(data, vocabulariesColorsMapFunc) {
        if (data && data.charts) {
          Mica.variablesCoverage = data.charts.map(chart => prepareVariablesClassificationsData(chart, vocabulariesColorsMapFunc(['${colors?join("', '")}'])));
        }

        initSelectBucket();
        renderVariablesClassifications('_all');
      }, function(response) {

      });
    </#if>

    function ensurePopulationDceSelection() {
      const hash = new URL(window.location).hash;
      const regex = /population\/([^\/]*)|data-collection-event\/(.*)$/g;

      const matches = [...hash.matchAll(regex)]
      if (matches.length > 0) {
        const populationId = matches[0][1].split(/:/).pop();
        let dceId = '';
        if (matches.length > 1) {
          dceId = matches[1][2].split(/:/).pop();
        }

        const nav = $('nav.main-header');
        const navOffset = nav ? nav.outerHeight() : 0;

        $([document.documentElement, document.body]).animate({
          scrollTop: $("#populations").offset().top - navOffset
        }, 250);

        setTimeout(() => {
          const tabEl = document.querySelector('a[href="#population-'+populationId+'"]');
          if (tabEl) {
            const tab = new bootstrap.Tab(tabEl);
            tab.show();
          }
          if (dceId) {
            const modalEl = document.getElementById('modal-'+populationId+'-'+dceId);
            if (modalEl) {
              const modal = new bootstrap.Modal(modalEl);
              modal.show();
            }
          }
        }, 250);

      }
    }

    ensurePopulationDceSelection();
  });
</script>
