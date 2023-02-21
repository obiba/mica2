<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-charts.js"></script>
<script src="${assetsPath}/libs/node_modules/plotly.js-dist-min/plotly.min.js"></script>

<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<script>
  const Mica = {};

  // cart
  <#if cartEnabled && variablesCartEnabled>
  const onVariablesCartAdd = function(id) {
    VariablesSetService.addQueryToCart('dataset(in(Mica_dataset.id,' + id + ')),variable(limit(0,10000),fields(variableType))', function(cart, oldCart) {
      VariablesSetService.showCount('#cart-count', cart, '${.lang}');
      if (cart.count === oldCart.count) {
        MicaService.toastInfo("<@message "sets.cart.no-variable-added"/>");
      } else {
        MicaService.toastSuccess("<@message "variables-added-to-cart"/>".replace('{0}', (cart.count - oldCart.count).toLocaleString('${.lang}')));
      }
    });
  };
  </#if>

  const renderVariablesClassifications = function() {
    $('#loadingClassifications').hide();
    const chartsElem = $('#chartsContainer');
    chartsElem.children().remove();
    if (Mica.variablesCoverage) {
      Mica.variablesCoverage.forEach(chartData => {
        chartsElem.append('<h5>' + chartData.title + '</h5>');
        chartsElem.append('<p>' + chartData.subtitle + '</p>');
        chartsElem.append('<div id="bar-graph" class="mb-4"></div>');

        const chartConfig = makeVariablesClassificationsChartSettings(chartData, {
          key: '${dataset.id}',
          label: "<@message "variables"/>",
          borderColor: '${barChartBorderColor}',
          backgroundColor: '${barChartBackgroundColor}',
          useColorsArray: ${useColorsArrayForClassificationsChart?c}
        });

        Plotly.newPlot("bar-graph", chartConfig.data, chartConfig.layout, {responsive: true});
      });
      $('#classificationsContainer').show();

      Plotly.relayout("bar-graph", {width: $('#classificationsContainer #bar-graph').width(), height: (2*1.42857)*12*Mica.variablesCoverage[0].vocabularies.length});
    } else {
      $('#noVariablesClassifications').show();
    }
  };

  $(function () {
    QueryService.getCounts('datasets', {query: "dataset(in(Mica_dataset.id,${dataset.id}))"}, function (stats) {
      $('#network-hits').text(numberFormatter.format(stats.networkResultDto.totalHits));
      $('#study-hits').text(numberFormatter.format(stats.studyResultDto.totalHits));
      $('#variable-hits').text(numberFormatter.format(stats.variableResultDto.totalHits));
      if (stats.variableResultDto.totalHits>0) {
        $('#cart-add').show();
      }
    });

    <#if type == "Harmonized">
      $('#harmonizedTable').show();
      const dataTableOpts = {
        drawCallback: function() {
          const pagination = $(this).closest('.dataTables_wrapper').find('.pagination-bar');
          if (pagination) {
            if (this.api().page.info().pages > 1) {
              pagination.removeClass('d-none');
            } else {
              pagination.addClass('d-none');
            }
          }
        },
        "paging": true,
        "pageLength": 25,
        "lengthChange": true,
        "searching": true,
        "ordering": false,
        "info": false,
        "autoWidth": true,
        "language": {
          "url": "${assetsPath}/i18n/datatables.${.lang}.json"
        },
        "processing": true,
        "serverSide": true,
        "ajax": function(data, callback) {
          const search = 'search' in data && data.search.value ? data.search.value : null;
          DatasetService.getHarmonizedVariables('${dataset.id}', search, data.start, data.length, function(response) {
            $('#loadingSummary').hide();
            if (response.variableHarmonizations) {
              let rows = [];
              for (const i in response.variableHarmonizations) {
                let row = [];
                const variableHarmonization = response.variableHarmonizations[i];
                const name = variableHarmonization.dataschemaVariableRef.name;
                row.push('<a href="${contextPath}/variable/${dataset.id}:' + name + ':Dataschema">' + name + '</a>');
                for (const j in variableHarmonization.harmonizedVariables) {
                  const harmonizedVariable = variableHarmonization.harmonizedVariables[j];
                  let iconClass = VariableService.getHarmoStatusClass(harmonizedVariable.status);
                  if (!harmonizedVariable.status || harmonizedVariable.status.length === 0) {
                    row.push('<i class="' + iconClass + '"></i>');
                  } else {
                    const url = harmonizedVariable.harmonizedVariableRef ? '../variable/' + harmonizedVariable.harmonizedVariableRef.id : '#';
                    row.push('<a href="' + url + '"><i class="' + iconClass + '"></i></a>');
                  }
                }
                rows.push(row);
              }
              callback({
                data: rows,
                recordsTotal: response.total,
                recordsFiltered: response.total
              });
            } else {
              callback({
                data: [],
                recordsTotal: 0,
                recordsFiltered: 0
              });
            }
          });
        },
        "fixedHeader": true,
        dom: "<'row'<'col-sm-12'f>><'row pagination-bar'<'col-sm-3'l><'col-sm-9'p>><'row'<'table-responsive col-sm-12'tr>><'row pagination-bar'<'col-sm-5'i><'col-sm-7'p>>",
        "info": true
      };
      $("#harmonizedTable").DataTable(dataTableOpts);

      /*
      $('#harmonizedTable').on( 'page.dt', function () {
        var info = table.page.info();
        console.dir(info);
      } );
      */
    </#if>

    <!-- Files -->
    const filesTr = {
      'item': "<@message "item"/>",
      'items': "<@message "items"/>",
      'download': "<@message "download"/>"
    };
    <#if showDatasetFiles>
      makeFilesVue('#files-app', {
        type: '${type?lower_case}-dataset',
        id: '${dataset.id}',
        basePath: '',
        path: '/',
        folder: {},
        tr: filesTr,
        locale: '${.lang}',
        contextPath: '${contextPath}'
      });
    </#if>
    <#if showStudyPopulationFiles>
      <#if study?? && population??>
        makeFilesVue('#study-${population.id}-files-app', {
          type: '<#if type == "Harmonized">harmonization<#else>individual</#if>-study',
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
      <#if studyTables?? && studyTables?size != 0>
        <#list studyTables as table>
          <#if table.population??>
            makeFilesVue('#study-${table.study.id}-${table.population.id}-files-app', {
              type: 'individual-study',
              id: '${table.study.id}',
              basePath: '/population/${table.population.id}',
              path: '/',
              folder: {},
              tr: filesTr,
              locale: '${.lang}',
              contextPath: '${contextPath}'
            }, function(file) {
              return !(file.type === 'FOLDER' && file.name === 'data-collection-event');
            });
          </#if>
        </#list>
      </#if>
    </#if>
    <#if showStudyDCEFiles>
      <#if study?? && population?? && dce??>
        makeFilesVue('#study-${population.id}-${dce.id}-files-app', {
          type: 'individual-study',
          id: '${study.id}',
          basePath: '/population/${population.id}/data-collection-event/${dce.id}',
          path: '/',
          folder: {},
          tr: filesTr,
          locale: '${.lang}',
          contextPath: '${contextPath}'
        });
      </#if>
      <#if studyTables?? && studyTables?size != 0>
        <#list studyTables as table>
          <#if table.population?? && table.dce??>
            makeFilesVue('#study-${table.study.id}-${table.population.id}-${table.dce.id}-files-app', {
              type: 'individual-study',
              id: '${table.study.id}',
              basePath: '/population/${table.population.id}/data-collection-event/${table.dce.id}',
              path: '/',
              folder: {},
              tr: filesTr,
              locale: '${.lang}',
              contextPath: '${contextPath}'
            });
          </#if>
        </#list>
      </#if>
    </#if>

    <#if datasetVariablesClassificationsTaxonomies?? && datasetVariablesClassificationsTaxonomies?size gt 0>
      const taxonomies = ['${datasetVariablesClassificationsTaxonomies?join("', '")}'];
      DatasetService.getVariablesCoverage('${dataset.id}', taxonomies, '${.lang}', function(data, vocabulariesColorsMapFunc) {
        if (data && data.charts) {
          Mica.variablesCoverage = data.charts.map(chart => prepareVariablesClassificationsData(chart, vocabulariesColorsMapFunc(['${colors?join("', '")}'])));
        }
        renderVariablesClassifications();
      }, function(response) {

      });
    </#if>
  });
</script>
