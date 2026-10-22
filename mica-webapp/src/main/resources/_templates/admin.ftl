<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <!-- Context path setting -->
  <title>${config.name!"Agate"}</title>
  <meta charset=utf-8>
  <meta name=description content="OBiBa identity provider">
  <meta name=format-detection content="telephone=no">
  <meta name=msapplication-tap-highlight content=no>
  <meta name=viewport content="user-scalable=no,initial-scale=1,maximum-scale=1,minimum-scale=1,width=device-width">
  <link rel=icon href="favicon.ico" sizes=any>
  <link rel=icon href="./admin/favicon.svg" type="image/svg+xml">
  <link rel=apple-touch-icon href="./admin/apple-touch-icon.png">
  <script type="module" crossorigin src="./admin/assets/${entryPointJS}"></script>
  <link rel="stylesheet" href="./admin/assets/${entryPointCSS}">
</head>
<body>
<div id=q-app></div>
</body>
</html>
