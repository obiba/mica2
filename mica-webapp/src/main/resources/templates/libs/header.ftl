<#macro header title titlePrefix="" subtitle="" breadcrumb=[]>
  <div class="content-header bg-info mb-4">
    <div class="container">
      <div class="row mb-2">
        <div class="col-sm-6">
          <h1 class="m-0">
            <#if titlePrefix?? && titlePrefix?length != 0>
              <span class="text-white-50"><@message titlePrefix/> /</span>
            </#if>
            <#if title?? && title?length != 0>
              <@message title/>
            </#if>
          </h1>
          <small>${subtitle}</small>
        </div><!-- /.col -->
        <div class="col-sm-6">
          <#if breadcrumb?? && breadcrumb?size != 0>
            <ol class="breadcrumb float-sm-right">
              <#list breadcrumb as item>
                <#if item?size == 2>
                  <li class="breadcrumb-item"><a class="text-white-50" href="${item[0]}">
                    <#if item[1]?? && item[1]?length != 0>
                      <@message item[1]/>
                    </#if>
                  </a></li>
                <#else>
                  <li class="breadcrumb-item active text-light">
                    <#if item[0]?? && item[0]?length != 0>
                      <@message item[0]/>
                    </#if>
                  </li>
                </#if>
              </#list>
            </ol>
          </#if>
        </div><!-- /.col -->
      </div><!-- /.row -->
    </div><!-- /.container-fluid -->
  </div>
</#macro>
