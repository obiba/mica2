<template>
  <div class="fullscreen bg-dark text-white text-center q-pa-md flex flex-center">
    <div>
      <div style="font-size: 30vh">404</div>

      <div class="text-h2" style="opacity: 0.4">{{ $t('error.not_found') }}</div>

      <q-btn
        class="q-mt-xl"
        color="white"
        text-color="black"
        unelevated
        :label="$t('nav.go_home')"
        no-caps
        :to="'/'"
      />
    </div>
  </div>
</template>
