# Design System Specification: The Luminescent Void

## 1. Overview & Creative North Star
**Creative North Star: The Midnight Architect**
This design system moves away from the "flat web" era into an environment of tactile depth and editorial precision. It is designed to feel like a high-end physical ledger resting under a soft spotlight in a darkened room. We achieve this through "The Midnight Architect" approach—using light not as a border, but as a medium to reveal form.

The system breaks traditional template constraints by favoring **intentional asymmetry** and **tonal layering**. We treat the screen as a 3D space where information doesn't just sit on a grid; it floats at various altitudes within a deep navy atmosphere.

---

## 2. Color & Atmospheric Surface Hierarchy
The palette is rooted in deep navy (#0B1326) and slate, prioritizing eye comfort and high-fidelity "prestige" aesthetics.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to define sections. Layout boundaries must be defined solely through background color shifts.
*   **Background (#0B1326):** The base canvas.
*   **Surface Container Low:** Used for secondary content areas or sidebars.
*   **Surface Container High:** Used for interactive cards or primary content blocks.

### Surface Hierarchy & Nesting
Treat the UI as stacked sheets of fine glass. To create depth:
1.  **Base:** `surface`
2.  **Sectioning:** `surface_container_low`
3.  **Component:** `surface_container_highest`
This "nesting" creates natural separation without visual clutter.

### The "Glass & Gradient" Rule
To avoid a "flat" dark mode, use Glassmorphism for floating elements (Modals, Popovers, Navigation bars):
*   **Effect:** Apply `surface_bright` at 60% opacity with a `24px` backdrop-blur.
*   **Soul Gradients:** Main CTAs or Hero backgrounds should use a subtle linear gradient from `primary` (#ADC6FF) to `primary_container` (#003D88) at a 135-degree angle.

---

## 3. Typography: Editorial Authority
We pair the geometric character of **Manrope** for high-level messaging with the Swiss precision of **Inter** for data and utility.

*   **Display & Headline (Manrope):** Use these to create an "Editorial" feel. Don't be afraid of the `display-lg` (3.5rem) size for hero statements—the goal is to command attention.
*   **Body & Title (Inter):** These are the workhorses. Use `body-md` (0.875rem) for primary reading. Ensure tracking (letter-spacing) is set to -0.01em for a premium, "tight" look.
*   **Label (Inter):** Reserved for metadata and micro-copy. Always use `label-md` or `label-sm` in `on_surface_variant` to maintain a clear hierarchy.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are too "heavy" for this system. We use light and air to define space.

*   **The Layering Principle:** Depth is achieved by "stacking." A `surface-container-lowest` card placed on a `surface-container-low` section creates a recessed, "carved-out" look.
*   **Ambient Shadows:** If a floating effect is required (e.g., a dropdown), use an extra-diffused shadow: `box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4)`. The shadow must feel like an absence of light, not a gray smudge.
*   **The "Ghost Border" Fallback:** For accessibility in complex data views, use the `outline_variant` token at **15% opacity**. This creates a "suggestion" of a boundary that disappears into the background.

---

## 5. Components

### Buttons
*   **Primary:** A subtle gradient from `primary` (#ADC6FF) to `primary_container`. Text color is `on_primary`. Shape: `md` (Subtle roundedness).
*   **Secondary:** No background. Use a "Ghost Border" (15% opacity `outline`) with `primary` colored text.
*   **Tertiary:** Purely typographic. Use `label-md` in `primary` color.

### Cards & Lists
*   **Rule:** Forbid the use of divider lines.
*   **Implementation:** Separate list items using spacious vertical white space (Spacing Level 3) or by alternating background tones between `surface_container` and `surface_container_low`.
*   **Interaction:** On hover, a card should shift from `surface_container_high` to `surface_bright` to simulate "lifting" toward the light.

### Input Fields
*   **State:** The default state uses `surface_container_highest`. 
*   **Focus:** Instead of a thick border, use a 2px outer "glow" using the `primary` color at 30% opacity. This maintains the "Luminescent" theme.

### Signature Component: The "Clarity Breadcrumb"
Instead of standard breadcrumbs, use `label-sm` typography with `surface_variant` backgrounds for each step, creating a "pill" appearance that sits flush against the top of the container.

---

## 6. Do’s and Don’ts

### Do:
*   **Do** use extreme white space (Spacious/Level 3). Let the deep navy backgrounds "breathe."
*   **Do** use `tertiary` (#FFB691) for tiny pops of "humanity"—such as a notification dot or a "new" tag.
*   **Do** ensure all text meets WCAG AA contrast ratios against their respective `surface-container` tiers.

### Don’t:
*   **Don’t** use pure black (#000000). It kills the "navy/slate" depth and creates harsh contrast.
*   **Don’t** use standard 1px separators. If you feel you need a line, use a 4px gap of background color instead.
*   **Don’t** use high-saturation accent colors. The `primary` (#ADC6FF) is tuned for dark mode; keep all other accents similarly "dusty" or muted.