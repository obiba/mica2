<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-charts.js"></script>

<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<script>
  const Mica = {};

  const renderVariablesClassifications = function() {
    $('#loadingClassifications').hide();
    const chartsElem = $('#chartsContainer');
    chartsElem.children().remove();
    if (Mica.variablesCoverage) {
      Mica.variablesCoverage.forEach(chartData => {
        chartsElem.append('<h5>' + chartData.title + '</h5>');
        chartsElem.append('<p>' + chartData.subtitle + '</p>');
        chartsElem.append('<canvas class="mb-4"></canvas>');
        const chartCanvas = $('#chartsContainer canvas:last-child').get(0).getContext('2d');
        new Chart(chartCanvas, makeVariablesClassificationsChartSettings(chartData, {
          key: '${dataset.id}',
          label: '<@message "variables"/>',
          borderColor: '${barChartBorderColor}',
          backgroundColor: '${barChartBackgroundColor}'
        }));
      });
      $('#classificationsContainer').show();
    } else {
      $('#noVariablesClassifications').show();
    }
  };

  $(function () {
    micajs.stats('datasets', {query: "dataset(in(Mica_dataset.id,${dataset.id}))"}, function (stats) {
      $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
      $('#study-hits').text(new Intl.NumberFormat().format(stats.studyResultDto.totalHits));
      $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    });

    <#if type == "Harmonized">
      $('#harmonizedTable').show();
      const dataTableOpts = {
        "paging": true,
        "pageLength": 25,
        "lengthChange": true,
        "searching": false,
        "ordering": false,
        "info": false,
        "autoWidth": true,
        "language": {
          "url": "${assetsPath}/i18n/datatables.${.lang}.json"
        },
        "processing": true,
        "serverSide": true,
        "ajax": function(data, callback) {
          micajs.dataset.harmonizedVariables('${dataset.id}', data.start, data.length, function(response) {
            $('#loadingSummary').hide();
            if (response.variableHarmonizations) {
              let rows = [];
              for (const i in response.variableHarmonizations) {
                let row = [];
                const variableHarmonization = response.variableHarmonizations[i];
                const name = variableHarmonization.dataschemaVariableRef.name;
                row.push('<a href="../variable/${dataset.id}:' + name + ':Dataschema">' + name + '</a>');
                for (const j in variableHarmonization.harmonizedVariables) {
                  const harmonizedVariable = variableHarmonization.harmonizedVariables[j];
                  let iconClass = micajs.harmo.statusClass(harmonizedVariable.status);
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
            }
          });
        },
        "fixedHeader": true,
        dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>",
        "info": true
      };
      var table = $("#harmonizedTable").DataTable(dataTableOpts);

      /*
      $('#harmonizedTable').on( 'page.dt', function () {
        var info = table.page.info();
        console.dir(info);
      } );
      */
    </#if>

    <!-- Files -->
    <#if showDatasetFiles>
      makeFilesVue('#files-app', {
        type: '${type?lower_case}-dataset',
        id: '${dataset.id}',
        basePath: '',
        path: '/',
        folder: {},
        tr: {
          'item': '<@message "item"/>',
          'items': '<@message "items"/>',
          'download': '<@message "download"/>'
        },
        locale: '${.lang}'
      });
    </#if>
    <#if showStudyDCEFiles>
      <#if study?? && population?? && dce??>
        makeFilesVue('#study-${population.id}-${dce.id}-files-app', {
          type: 'individual-study',
          id: '${study.id}',
          basePath: '/population/${population.id}/data-collection-event/${dce.id}',
          path: '/',
          folder: {},
          tr: {
            'item': '<@message "item"/>',
            'items': '<@message "items"/>',
            'download': '<@message "download"/>'
          },
          locale: '${.lang}'
        });
      </#if>
      <#if studyTables?? && studyTables?size != 0>
        <#list studyTables as table>
          makeFilesVue('#study-${table.study.id}-${table.population.id}-${table.dce.id}-files-app', {
            type: 'individual-study',
            id: '${table.study.id}',
            basePath: '/population/${table.population.id}/data-collection-event/${table.dce.id}',
            path: '/',
            folder: {},
            tr: {
              'item': '<@message "item"/>',
              'items': '<@message "items"/>',
              'download': '<@message "download"/>'
            },
            locale: '${.lang}'
          });
        </#list>
      </#if>
    </#if>

    <#if datasetVariablesClassificationsTaxonomies?? && datasetVariablesClassificationsTaxonomies?size gt 0>
      const taxonomies = ['${datasetVariablesClassificationsTaxonomies?join("', '")}'];
      micajs.dataset.variablesCoverage('${dataset.id}', taxonomies, '${.lang}', function(data) {
        if (data && data.charts) {
          Mica.variablesCoverage = data.charts.map(chart => prepareVariablesClassificationsData(chart));
        }
        renderVariablesClassifications();
      }, function(response) {

      });
    </#if>
  });
</script>
