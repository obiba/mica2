<script>
  UserService.signin("form", "otp", function (response, banned) {
    if (response.status === 401 && response.headers["www-authenticate"] === "X-Obiba-TOTP") {
      $("#otp").val("");
      $("#signInCard").hide();
      $("#2faCard").show();
      if (response.data?.image) {
        $("#2faImage").show();
        $("#qr-img").attr("src", response.data.image);
      } else {
        $("#2faImage").hide();
      }
    } else {
      $("#otp").val("");
      $("#signInCard").show();
      $("#2faCard").hide();
      $("#2faImage").hide();
      $("#qr-img").attr("src", "");
      const alertId = banned ? "#alertBanned" : "#alertFailure";
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
    $("#2faImage").hide();
    $("#qr-img").attr("src", "");
  };

  $('#otp').keypress((e) => {
    if (e.which === 13) {
      validateOtp();
    }
  });
</script>
