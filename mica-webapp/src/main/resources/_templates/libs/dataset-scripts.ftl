<script>
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
          "url": "/assets/i18n/datatables.${.lang}.json"
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
  });
</script>
