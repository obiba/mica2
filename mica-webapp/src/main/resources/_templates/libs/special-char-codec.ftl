<script>
  const encodeMappings = {
    <#if specialCharCodec?? && specialCharCodec?has_content>
      <#list specialCharCodec?keys as key>
        "${key?js_string}": "${specialCharCodec[key]?js_string}"<#if key_has_next>,</#if>
      </#list>
    </#if>
  };
  const decodeMappings = Object.fromEntries(Object.entries(encodeMappings).map(([key, value]) => [value, key]));
  <#noparse>
  Mica.encodeRqlSafe = (str) => {
    if (typeof str !== "string") return str;
    const pattern = new RegExp(`[${Object.keys(encodeMappings).map(char => '\\\\' + char).join('')}]`,'g');
    return str.replace(pattern, match => encodeMappings[match]);
  }
  </#noparse>

  Mica.decodeRqlSafe = (str) => {
    if (typeof str !== "string") return str;
    let decoded = str;
    for (const [encoded, original] of Object.entries(decodeMappings)) {
      decoded = decoded.split(encoded).join(original);
    }
    return decoded;
  }
</script>
