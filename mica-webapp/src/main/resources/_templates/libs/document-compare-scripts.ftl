<script>
  const onRemoveRow = function(field) {
    $('#' + field).remove();
  };

  const makeCompareTable = function(id) {
    const dataTableOpts = {
      dom: "<'row'<'col-sm-6'B><'col-sm-6'f>><'row'<'table-responsive col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>",
      buttons: [
        'csv', 'print'
      ],
      ...dataTablesDefaultOpts};
    dataTableOpts.paging = false;
    dataTableOpts.ordering = false;
    $("#" + id).DataTable(dataTableOpts);
  }

  let harmonizationStudiesCompareTableInitialized = false;

  $(function () {
    if ("${type}" === "studies") {
      <#if individualStudies?size gt 0>
        makeCompareTable("compare-individual-studies");
        $("#harmonization-studies-tab").click(function() {
          if (!harmonizationStudiesCompareTableInitialized) {
            makeCompareTable("compare-harmonization-studies");
            harmonizationStudiesCompareTableInitialized = true;
          }
        });
      <#else>
        makeCompareTable("compare-harmonization-studies");
      </#if>
    } else
      makeCompareTable("compare-${type}");
  });
</script>
