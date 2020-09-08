<!-- Timeline -->
<script src="../bower_components/d3/d3.js"></script>
<script src="../bower_components/mica-study-timeline/dist/mica-study-timeline.js"></script>

<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-variable.js"></script>
<!-- Select2 -->
<script src="${adminLTEPath}/plugins/select2/js/select2.js"></script>
<script src="${adminLTEPath}/plugins/select2/js/i18n/${.lang}.js"></script>

<script>
  const Mica = { options: {} };
  <#if study.populations?? && study.populations?size != 0>
    <#list study.populationsSorted as population>
      <#if type == "Individual">
        <#if population.dataCollectionEvents?? && population.dataCollectionEvents?size != 0>
          <#list population.dataCollectionEventsSorted as dce>
            Mica.options['${study.id}:${population.id}:${dce.id}'] =
              <#if study.populations?size == 1>'${localize(dce.name)}'<#else>'${localize(population.name)} / ${localize(dce.name)}'</#if>;
          </#list>
        </#if>
      <#else>
        Mica.options['${study.id}:${population.id}:.'] = '${localize(population.name)}';
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
        chartsElem.append('<canvas class="mb-4"></canvas>');
        const chartCanvas = $('#chartsContainer canvas:last-child').get(0).getContext('2d');
        new Chart(chartCanvas, makeVariablesClassificationsChartSettings(chartData, {
          key: key,
          label: '<@message "variables"/>',
          borderColor: '${barChartBorderColor}',
          backgroundColor: '${barChartBackgroundColor}'
        }));
      });
      $('#classificationsContainer').show();
    } else {

    }
  };

  const initSelectBucket = function() {
    // scan for bucket ids
    const buckets = [];
    if (Mica.variablesCoverage) {
      Mica.variablesCoverage.forEach(chartData => {
        Object.keys(chartData.itemCounts).forEach(k => {
          if (k !== '_all' && !buckets.includes(k)) {
            buckets.push(k);
          }
        });
      });
    }
    const selectBucketElem = $('#select-bucket');
    selectBucketElem.select2({
      theme: 'bootstrap4'
    }).on('select2:select', function (e) {
      let data = e.params.data;
      //console.log(data);
      $('#classificationsContainer').hide();
      renderVariablesClassifications(data.id);
    });
    buckets.forEach(k => {
      let newOption = new Option(Mica.options[k], k, false, false);
      selectBucketElem.append(newOption);
    });
  };

  $(function () {
    let options = dataTablesDefaultOpts;
    options.columnDefs = [
      { "type": "num", "targets": 0, "visible": false }
    ];
    <#list study.populations as pop>
      $("#population-${pop.id}-dces").DataTable(options);
    </#list>

    micajs.stats('studies', { query: "study(in(Mica_study.id,${study.id}))" }, function(stats) {
      $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
      $('#dataset-hits').text(new Intl.NumberFormat().format(stats.datasetResultDto.totalHits));
      $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    });

    <#if timelineData??>
      let timelineData = ${timelineData};
      new $.MicaTimeline(new $.StudyDtoParser('${.lang}')).create('#timeline', timelineData).addLegend();
    </#if>


    <!-- Variables classifications -->
    <#if studyVariablesClassificationsTaxonomies?? && studyVariablesClassificationsTaxonomies?size gt 0>
      const taxonomies = ['${networkVariablesClassificationsTaxonomies?join("', '")}'];
      $('#classificationsContainer').hide();
      micajs.study.variablesCoverage('${study.id}', taxonomies, '${.lang}', function(data) {
        if (data && data.charts) {
          Mica.variablesCoverage = data.charts.map(chart => prepareVariablesClassificationsData(chart));
        }
        initSelectBucket();
        renderVariablesClassifications('_all');
      }, function(response) {

      });
    </#if>

  });
</script>
