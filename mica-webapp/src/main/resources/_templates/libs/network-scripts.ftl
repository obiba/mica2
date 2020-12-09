<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-charts.js"></script>
<!-- Select2 -->
<script src="${adminLTEPath}/plugins/select2/js/select2.js"></script>
<script src="${adminLTEPath}/plugins/select2/js/i18n/${.lang}.js"></script>

<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<script>
  const Mica = { options: {} };
  <#if studyAcronyms??>
    <#list studyAcronyms as key, value>
    Mica.options['${key}'] = '${localize(value)}';
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
          label: "<@message "variables"/>",
          borderColor: '${barChartBorderColor}',
          backgroundColor: '${barChartBackgroundColor}'
        }));
      });
      $('#classificationsContainer').show();
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
    }
  };

  const toMembershipsAnchors = function(urlPrefix, membershipsArray) {
    const roleTr = {'contact': "<@message "contact.label.contact"/>", 'investigator': "<@message "contact.label.investigator"/>"};

    const memberships = (membershipsArray || []).reduce((accumulator, membership) => {
      const item = accumulator[membership.parentId] || {};
      accumulator[membership.parentId] = item;

      if (!Array.isArray(item.roles)) {
        item.roles = [];
      }

      item.roles.push(roleTr[membership.role]);
      item.acronym = LocalizedValues.forLang(membership.parentAcronym, '${.lang}');

      return accumulator;
    }, {});

    return (Object.keys(memberships) || []).map(key => {
      let url = MicaService.normalizeUrl(urlPrefix + key);
      let acronym = memberships[key].acronym;
      let roles = '<span class="ml-1">(' + (memberships[key].roles || []).join(", ") + ')</span>';

      return '<a href="' + url + '">' + acronym + '</a>' + roles;
    });
  };

  $(function () {
    $("#networks").DataTable(dataTablesDefaultOpts);
    $("#individual-studies").DataTable(dataTablesDefaultOpts);
    $("#harmonization-studies").DataTable(dataTablesDefaultOpts);
    QueryService.getCounts('networks', { query: "network(in(Mica_network.id,${network.id}))" }, function(stats) {
      $('#study-hits').text(numberFormatter.format(stats.studyResultDto.totalHits));
      $('#dataset-hits').text(numberFormatter.format(stats.datasetResultDto.totalHits));
      $('#variable-hits').text(numberFormatter.format(stats.variableResultDto.totalHits));
    });

    <!-- Affiliated Members -->
    <#if affiliatedMembersQuery??>
      NetworkService.getAffiliatedMembers('${affiliatedMembersQuery}', function(data) {
        if (data && data.total > 0) {
          const dataset = data.persons.map(person => {
            const item = [];
            item.push(person.firstName + ' ' + person.lastName);
            item.push(person.email || '');

            const studyMemberships = toMembershipsAnchors('/study/', person.studyMemberships);
            const networkMemberships = toMembershipsAnchors('/network', person.networkMemberships);

            item.push(studyMemberships.length > 0 ? studyMemberships.join(', ') : '');
            item.push(networkMemberships.length > 0 ? networkMemberships.join(', ') : '');

            return item;
          });

          $('#affiliatedMembersTable').dataTable({
            columns: [{ title: "<@message "full-name"/>" }, { title: "<@message "email"/>" }, { title: "<@message "studies"/>" }, { title: "<@message "networks"/>" }],
            data: dataset
          });

          $('#affiliatedMembersModal').modal('handleUpdate');
        }
      }, function(response) {

      });
    </#if>

    <!-- Files -->
    <#if showNetworkFiles>
      makeFilesVue('#files-app', {
        type: 'network',
        id: '${network.id}',
        basePath: '',
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

    <#if networkVariablesClassificationsTaxonomies?? && networkVariablesClassificationsTaxonomies?size gt 0 && studyAcronyms?? && studyAcronyms?size gt 0>
      const taxonomies = ['${networkVariablesClassificationsTaxonomies?join("', '")}'];
      $('#classificationsContainer').hide();
      NetworkService.getVariablesCoverage('${network.id}', taxonomies, '${.lang}', function(data) {
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
