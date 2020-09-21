<!-- Script for the cart and the documents list pages -->
<script>
  $(function () {
    const dataTableOpts = {
      "paging": true,
      lengthMenu: [10, 20, 50, 100],
      pageLength: 20,
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
        micajs.variable.set.searchDocuments('${set.id}', data.start, data.length, function(response) {
          $('#loadingSet').hide();
          if (response.variableResultDto && response.variableResultDto['obiba.mica.DatasetVariableResultDto.result']) {
            const result = response.variableResultDto;
            let rows = [];
            const summaries = result['obiba.mica.DatasetVariableResultDto.result'].summaries;
            for (const i in summaries) {
              let row = [];
              const summary = summaries[i];
              row.push('<a href="../variable/' + summary.id + '">' + summary.name + '</a>');
              row.push(summary.variableLabel ? LocalizedValues.forLang(summary.variableLabel, '${.lang}') : '');
              <#if config.studyDatasetEnabled && config.harmonizationDatasetEnabled>
                row.push(summary.variableType);
              </#if>
              <#if !config.singleStudyEnabled>
                row.push('<a href="../study/' + summary.studyId + '">' + LocalizedValues.forLang(summary.studyAcronym, '${.lang}') + '</a>');
              </#if>
              row.push('<a href="../dataset/' + summary.datasetId + '">' + LocalizedValues.forLang(summary.datasetAcronym, '${.lang}') + '</a>');
              rows.push(row);
            }
            callback({
              data: rows,
              recordsTotal: result.totalHits,
              recordsFiltered: result.totalHits
            });
          }
        });
      },
      "fixedHeader": true,
      dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>",
      "info": true
    };
    $("#setTable").DataTable(dataTableOpts);
  });
</script>
