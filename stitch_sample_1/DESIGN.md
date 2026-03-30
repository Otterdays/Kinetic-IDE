```markdown
# Design System Specification: The Kinetic Syntax

## 1. Overview & Creative North Star
**Creative North Star: "The Neon Architect"**
This design system moves away from the static, gray-box environments of traditional IDEs toward a high-fidelity, immersive workspace. It treats code not as text on a page, but as a living architectural structure. By utilizing intentional asymmetry, deep tonal layering, and "active" light sources, we create a professional environment that feels both ultra-precise and evolutionarily intelligent. 

The "template" look is rejected in favor of a **Modular Monolith** approach: dense data-heavy panels are balanced by expansive, blurred glass overlays for AI interactions, ensuring the tablet interface feels like a specialized piece of hardware rather than a web app.

---

## 2. Colors & Surface Logic
The palette is rooted in the deep void of `#060e20` (Background), utilizing high-frequency accents to denote machine intelligence and logical operations.

### The "No-Line" Rule
Traditional 1px borders are strictly prohibited for sectioning. Structural boundaries must be defined exclusively through background shifts. 
- Use `surface_container_low` for the primary editor area.
- Use `surface_container_high` for sidebars.
- Transitioning between these tokens provides a "machined" look that feels more premium than a stroke.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of light-absorbing materials:
1.  **Base Layer:** `surface` (#060e20) – The infinite canvas.
2.  **Primary Workspace:** `surface_container` (#0f1930) – The main IDE floor.
3.  **Utility Panels:** `surface_container_high` (#141f38) – File explorers and inspectors.
4.  **Floating Logic:** `surface_container_highest` (#192540) – Modals and pop-overs.

### The "Glass & Gradient" Rule
To elevate AI-driven components (like autocomplete suggestions or chat), use **Glassmorphism**. Apply a backdrop-blur (minimum 12px) to `surface_variant` at 60% opacity. 
- **Signature Texture:** Primary CTAs should utilize a linear gradient from `primary` (#81ecff) to `primary_dim` (#00d4ec) at a 135-degree angle to simulate glowing light-tubes.

---

## 3. Typography: Precision & Utility
The system utilizes a dual-engine typographic approach to separate "System Logic" from "Human Interface."

- **The Monospace Engine (JetBrains Mono):** Reserved strictly for code and metadata. It represents the "truth" of the machine.
- **The UI Engine (Inter / Space Grotesk):** 
    - **Display & Headlines:** Use `Space Grotesk`. Its geometric quirks mirror the high-tech aesthetic.
    - **Body & Labels:** Use `Inter`. It provides maximum legibility on high-density tablet displays.

### Typographic Hierarchy
- **Display LG (3.5rem):** Used for empty state titles or major AI insights.
- **Title SM (1rem):** The standard for panel headers. Use `font-weight: 600` and `letter-spacing: 0.05rem` to create an authoritative, "instrument panel" feel.
- **Label SM (0.6875rem):** Used for micro-data (line numbers, file sizes). Always in `on_surface_variant` to prevent visual noise.

---

## 4. Elevation & Depth
Depth in this system is a result of **Tonal Layering**, not shadows.

- **The Layering Principle:** Place a `surface_container_lowest` (#000000) card inside a `surface_container_low` (#091328) container to create a "recessed" effect—perfect for code execution terminals.
- **Ambient Shadows:** For floating AI elements, use a "Cyan-Glow" shadow: `0px 20px 40px rgba(129, 236, 255, 0.08)`. This mimics the light of the screen reflecting off the user’s hand.
- **The "Ghost Border" Fallback:** If a separator is required for accessibility, use the `outline_variant` token at 15% opacity. Never use 100% opacity lines; they break the immersion of the dark environment.

---

## 5. Components

### AI Action Buttons
- **Primary:** Gradient fill (`primary` to `primary_dim`). Text is `on_primary_fixed` (Deep Teal).
- **Secondary:** Transparent background with a `Ghost Border` and `primary` text.
- **Touch Target:** Minimum 44x44dp. Even if the visual button is small, the hit area must be expansive for tablet ergonomics.

### The "AI Aura" Chip
Used for AI-suggested tags or functions.
- **Style:** Background `tertiary_container`, text `on_tertiary_container`, with a 2px outer glow of `tertiary_dim`. 
- **Shape:** `Roundedness: full`.

### Code Editor Surface
- **Separation:** No dividers between the line-number gutter and the code. Use a shift from `surface_container_low` (gutter) to `surface_container` (editor).
- **Syntax Highlighting:** 
    - Keywords: `secondary` (#c3f400)
    - Functions: `primary` (#81ecff)
    - Strings: `tertiary` (#ac89ff)
    - Comments: `outline` (#6d758c)

### Input Fields (Command Palette)
- **State:** Active inputs should not use a border; they should use a `primary` outer glow and a background shift to `surface_bright`.
- **Text:** User input in `on_surface`, placeholder in `on_surface_variant`.

---

## 6. Do’s and Don’ts

### Do:
- **Use Asymmetric Spacing:** Give the code editor 2.25rem (`spacing.10`) of breathing room on the left, but keep sidebars dense at 0.75rem (`spacing.3.5`).
- **Embrace the Glow:** Use subtle blurs behind AI elements to suggest they are "computing."
- **Focus on Density:** Ensure professional users can see 40+ lines of code at once.

### Don’t:
- **Don't use pure white:** All "white" text should be `on_surface` (#dee5ff), which is a very light blue-tinted slate. This reduces eye strain in dark mode.
- **Don't use standard dividers:** If you find yourself reaching for a `<hr>` tag, use a `0.4rem` (`spacing.2`) vertical gap instead.
- **Don't use heavy shadows:** They look muddy in deep charcoal palettes. Use tonal stepping (Surface Low -> Surface High) instead.

### Director's Final Note:
*This system is about the tension between the dark, quiet space of the editor and the vibrant, electric energy of the AI. Treat the AI as a guest in the user's workspace—bright, helpful, and sophisticated, but never cluttering the structural integrity of the code itself.*