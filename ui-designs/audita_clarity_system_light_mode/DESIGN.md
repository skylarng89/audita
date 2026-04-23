# Design System Specification: The Sovereign Architect

## 1. Overview & Creative North Star
In the world of ITIL and ITSM Change Management, the stakes are high. Every interaction within this system must project an aura of **"The Sovereign Architect"**—a persona that is authoritative, calm, and hyper-organized. 

We are moving away from the "standard SaaS dashboard" that relies on heavy borders and cluttered grids. Instead, this design system utilizes **Editorial Minimalism**. We use exaggerated white space, intentional asymmetry, and tonal depth to guide the user’s eye. The goal is to transform complex technical data into a curated experience where the most critical information—the "Change Risk"—is immediately apparent through visual weight rather than visual noise.

## 2. Colors & Tonal Depth
This system leverages a sophisticated Material-based palette to create a sense of environmental stability. 

### The Palette
- **Primary Focus:** `primary (#00236F)` and `primary_container (#1E3A8A)`. These colors represent the "Command" state.
- **Surface Hierarchy:** The foundation is built on `surface (#F7F9FB)`. 
- **Semantics:** Use `error (#BA1A1A)` for failed changes and `tertiary (#4B1C00)` as a unique, high-end accent for amber/warning states, moving away from generic bright oranges.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section off the UI. We define boundaries through tonal shifts. A section should be differentiated by placing a `surface_container_low` block against a `surface` background. If you feel the need for a line, you haven't used your spacing scale correctly.

### The Glass & Gradient Rule
To provide "visual soul," use the **Signature Glow**:
- **CTAs:** Main buttons should feature a subtle linear gradient from `primary` to `primary_container` (150-degree angle).
- **Floating Overlays:** Use `surface_container_lowest` with a 80% opacity and a `24px` backdrop-blur to create a "frosted glass" effect for modals and collapsible sidebars. This keeps the user grounded in their current context.

## 3. Typography: The Editorial Voice
We utilize **Inter** not just for its legibility, but for its architectural neutrality. 

- **Display Scales:** Use `display-lg (3.5rem)` and `display-md (2.75rem)` for high-level metrics (e.g., "99.2% Success Rate"). These should have tight letter-spacing (-0.02em) to look premium.
- **The Technical Layer:** Use `label-md` and `label-sm` for metadata and ITIL tags. These should be set in All-Caps with +0.05em tracking to differentiate "data" from "narrative."
- **Visual Hierarchy:** Use `title-lg` for card headings. Never bold these—use Medium weight (500) to maintain a clean, high-end feel.

## 4. Elevation & Depth: Tonal Layering
Depth in this design system is physical, not digital. We "stack" surfaces to create hierarchy.

- **The Layering Principle:** 
    1. Base: `surface`
    2. Large Sections: `surface_container_low`
    3. Content Cards: `surface_container_lowest` (White)
    This creates a soft, natural lift that eliminates the need for borders.
- **Ambient Shadows:** Shadows are reserved for floating elements (Toasts, Modals). Use a hyper-diffused shadow: `0px 20px 40px rgba(0, 35, 111, 0.06)`. Note the tint—we use a fraction of the `primary` color in our shadows, never pure black/grey.
- **The "Ghost Border" Fallback:** If accessibility requirements (WCAG 2.1 AA) demand a container edge in a low-contrast environment, use a `1px` stroke of `outline_variant` at **20% opacity**. It should be felt, not seen.

## 5. Components & Primitives

### Buttons
- **Primary:** Rounded `lg (1rem)`. Background: `primary`. Text: `on_primary`. No border.
- **Secondary:** Background: `secondary_container`. Text: `on_secondary_container`.
- **States:** On `:hover`, shift background color to `primary_container`. On `:focus`, apply a 3px outer ring of `surface_tint` with a 2px white gap.

### Cards & Lists
- **Rule:** Forbid divider lines between list items. Use `body-md` height and `12px` vertical padding to create separation.
- **Nesting:** Place `surface_container_highest` chips inside `surface_container_lowest` cards to denote status.

### Navigation (The Sovereign Sidebar)
- **Style:** Use `surface_container_low`. 
- **Active State:** An active menu item should not use a "box." Instead, use a vertical "pill" indicator (4px wide) of `primary` on the left and a subtle shift of the text color to `on_primary_fixed_variant`.

### Input Fields
- **Default:** `surface_container_lowest` with a subtle `outline_variant` (20% opacity) "Ghost Border."
- **Focus:** Transition the border to `primary` and add a subtle `primary_fixed` inner glow.

### Specialized Component: The Change Timeline
- Use a vertical line of `outline_variant` (10% opacity) with `primary` nodes. Overlap the nodes slightly over the card edges to break the grid and create an "interconnected" look.

## 6. Do’s and Don’ts

### Do
- **Do** use `8px (DEFAULT)` to `12px (lg)` rounded corners (Level 2) for all containers to soften the technical nature of ITIL.
- **Do** lean into asymmetry. For example, a header can be left-aligned while the primary action is floating in a `surface_container_lowest` card on the right.
- **Do** use `tertiary_container` for "In Progress" states to provide a sophisticated earthy contrast to the deep blue.
- **Do** utilize **Spacious (Level 3)** internal spacing to ensure the editorial minimalism is maintained.

### Don't
- **Don't** use 100% black text. Always use `on_surface` or `on_surface_variant` to keep the UI feeling "expensive."
- **Don't** use standard "Success Green" if it clashes with the indigo palette; use the muted tones provided in the `on_secondary_container` for non-critical success states.
- **Don't** cram data. If a table feels full, increase the vertical spacing. Space is a luxury; use it.