<script>
  UserService.signin("form", "otp", function (response, banned) {
    if (response.status === 401 && response.headers["www-authenticate"] === "X-Obiba-TOTP") {
      $("#otp").val("");
      $("#signInCard").hide();
      $("#2faCard").show();
    } else {
      $("#otp").val("");
      $("#signInCard").show();
      $("#2faCard").hide();
      var alertId = banned ? "#alertBanned" : "#alertFailure";
      $(alertId).removeClass("d-none");
      setTimeout(function () {
        $(alertId).addClass("d-none");
      }, 5000);
    }
  });

  const validateOtp = () => {
    $("#form").submit();
  };

  const cancelOtp = () => {
    $("#otp").val("");
    $("#signInCard").show();
    $("#2faCard").hide();
  };

  $('#otp').keypress((e) => {
    if (e.which === 13) {
      validateOtp();
    }
  });
</script>
