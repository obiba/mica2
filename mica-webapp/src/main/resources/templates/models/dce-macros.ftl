<!-- DCE macros -->

<!-- DCE model -->
<#macro dceModel dce>
</#macro>

<!-- DCE modal dialog -->
<#macro dceDialog id dce>
  <div class="modal fade" id="modal-${id}">
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title">${dce.name[.lang]!""}</h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <#if dce.description??>
            <div>${dce.description[.lang]!""}</div>
          </#if>
          <@dceModel dce=dce/>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>
