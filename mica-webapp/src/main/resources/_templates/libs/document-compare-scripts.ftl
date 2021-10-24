<script>
  const onRemoveRow = function(field) {
    $('#' + field).remove();
  };

  const makeCompareTable = function() {
    const dataTableOpts = {
      dom: "<'row'<'col-sm-6'B><'col-sm-6'f>><'row'<'table-responsive col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>",
      buttons: [
        'csv', 'print'
      ],
      ...dataTablesDefaultOpts};
    dataTableOpts.paging = false;
    dataTableOpts.ordering = false;
    $("#compare-${type}").DataTable(dataTableOpts);
  }

  $(function () {
    makeCompareTable();
  });
</script>
