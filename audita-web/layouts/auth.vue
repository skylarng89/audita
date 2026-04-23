<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-slate-950 p-4">
    <!-- Split-panel card matching the sign-in design -->
    <div class="w-full max-w-4xl flex rounded-2xl shadow-2xl overflow-hidden bg-white dark:bg-surface-dark">

      <!-- Left: Navy hero panel -->
      <div class="hidden md:flex flex-col justify-between w-[45%] bg-hero-gradient p-10 text-white">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center font-bold text-sm">A</div>
          <span class="font-bold text-lg tracking-tight">Audita</span>
        </div>

        <div>
          <slot name="hero">
            <h1 class="text-3xl font-bold leading-tight mb-4">
              The Sovereign Architect of Infrastructure.
            </h1>
            <p class="text-white/70 text-sm leading-relaxed">
              Enter the command center. Oversee risks, manage change, and maintain
              absolute structural integrity across your enterprise.
            </p>
          </slot>
        </div>

        <div class="flex items-center gap-2 text-white/50 text-xs uppercase tracking-widest">
          <div class="w-6 h-px bg-white/30" />
          <slot name="hero-footer">Super Admin Access</slot>
        </div>
      </div>

      <!-- Right: Form panel -->
      <div class="flex-1 flex flex-col justify-center p-8 md:p-12">
        <slot />
      </div>
    </div>

    <!-- Help button -->
    <button
      class="fixed bottom-6 right-6 w-9 h-9 rounded-full bg-white dark:bg-slate-800 shadow
             border border-border dark:border-border-dark text-muted text-sm font-semibold
             hover:bg-gray-50"
      aria-label="Need assistance?"
    >?</button>
  </div>
</template>
