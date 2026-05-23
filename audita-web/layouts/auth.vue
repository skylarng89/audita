<template>
  <div
    class="min-h-screen flex items-center justify-center bg-[#f7f9fb] dark:bg-slate-950 p-4 relative overflow-hidden"
  >
    <!-- Decorative blurs matching designs -->
    <div
      class="absolute top-[-10%] right-[-5%] w-[40vw] h-[40vw] bg-primary/5 rounded-full blur-[120px] pointer-events-none"
    ></div>
    <div
      class="absolute bottom-[-10%] left-[-5%] w-[30vw] h-[30vw] bg-secondary/10 rounded-full blur-[100px] pointer-events-none"
    ></div>

    <!-- Split-panel card matching the sign-in design -->
    <div
      class="w-full max-w-[900px] flex rounded-2xl shadow-[0px_24px_48px_rgba(0,35,111,0.10)] overflow-hidden bg-white dark:bg-slate-800 relative z-10"
    >
      <!-- Left: Navy hero panel -->
      <div
        class="hidden md:flex flex-col justify-between w-[45%] bg-hero-gradient p-10 text-white relative overflow-hidden"
      >
        <!-- Subtle texture overlay -->
        <div class="absolute inset-0 opacity-10">
          <div
            class="absolute inset-0"
            style="
              background-image: url(&quot;data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.4'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E&quot;);
            "
          ></div>
        </div>

        <div class="relative z-10 flex items-center gap-3">
          <img
            src="/brand/audita-icon-dark.svg"
            alt="Audita"
            class="w-10 h-10 rounded-xl"
          />
          <span class="font-bold text-lg tracking-tight">Audita</span>
        </div>

        <div class="relative z-10">
          <slot name="hero">
            <h1
              class="text-3xl font-semibold leading-tight mb-4 tracking-tight"
            >
              The Sovereign<br />Architect of<br />Infrastructure.
            </h1>
            <p class="text-white/70 text-sm leading-relaxed">
              Enter the command center. Oversee risks, manage change, and
              maintain absolute structural integrity across your enterprise.
            </p>
          </slot>
        </div>

        <div
          class="relative z-10 flex items-center gap-3 text-white/50 text-xs uppercase tracking-widest"
        >
          <div class="w-8 h-px bg-white/30" />
          <slot name="hero-footer">Portal Access</slot>
        </div>
      </div>

      <!-- Right: Form panel -->
      <div
        class="flex-1 flex flex-col justify-center px-10 py-12 md:px-14 bg-white dark:bg-slate-800"
      >
        <slot />
      </div>
    </div>

    <!-- Help button -->
    <button
      class="fixed bottom-6 right-6 w-9 h-9 rounded-full bg-white dark:bg-slate-800 shadow-card-md border border-outline-variant text-muted text-sm font-semibold hover:bg-surface-container-low transition-colors"
      aria-label="Need assistance?"
    >
      ?
    </button>
  </div>
</template>
