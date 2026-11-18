<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<script>
  $(function () {
    const errorMessages = {
      "server.error.bad-request": "${messageJs("server.error.bad-request")}",
      "server.error.bad-captcha": "${messageJs("server.error.bad-captcha")}",
    };
    const requiredFields = [
      { name: "name", title: "${messageJs("contact-name")}" },
      { name: "email", title: "${messageJs("contact-email")}" },
      { name: "subject", title: "${messageJs("contact-subject")}" },
      { name: "message", title: "${messageJs("contact-message")}" }
      <#if reCaptchaKey?has_content>
      , { name: "g-recaptcha-response", title: "${messageJs("captcha")}" }
      </#if>
    ];
    UserService.contact("#form", requiredFields, function() {
      let form = document.querySelector("#form");
      if (form) {
        window.location.href = window.location.origin + "${contextPath}/contact-success"
      }
    }, function (messageItems) {
      var alertId = "#alertFailure";
      var msg = "";
      if (Array.isArray(messageItems)) {
        msg = "${messageJs("contact-fields-required")}: " + messageItems.map(item => item.title).join(", ");
        messageItems.map(item => item.name).forEach(itemName => {
          $('#contact-' + itemName).addClass('is-invalid');
        });
      } else if (messageItems.title && errorMessages[messageItems.title]) {
        msg = errorMessages[messageItems.title];
        $('#contact-' + messageItems.name).addClass('is-invalid');
      } else {
        msg = errorMessages[messageItems] || errorMessages["server.error.bad-request"];
      }
      $(alertId).html('<small>' + msg + '</small>').removeClass("d-none");
      setTimeout(function() {
        $(alertId).addClass("d-none");
      }, 5000);
    });
  });
</script>
</#outputformat>
