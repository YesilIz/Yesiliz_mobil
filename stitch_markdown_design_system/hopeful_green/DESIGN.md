---
name: Hopeful Green
colors:
  surface: '#f2fcf4'
  surface-dim: '#d2dcd5'
  surface-bright: '#f2fcf4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#ecf6ee'
  surface-container: '#e6f0e8'
  surface-container-high: '#e1ebe3'
  surface-container-highest: '#dbe5dd'
  on-surface: '#151d19'
  on-surface-variant: '#3d4a3c'
  inverse-surface: '#29322d'
  inverse-on-surface: '#e9f3eb'
  outline: '#6d7b6b'
  outline-variant: '#bccbb8'
  surface-tint: '#006e27'
  primary: '#006e27'
  on-primary: '#ffffff'
  primary-container: '#00ae42'
  on-primary-container: '#00380f'
  inverse-primary: '#54e16d'
  secondary: '#366853'
  on-secondary: '#ffffff'
  secondary-container: '#b6ebd1'
  on-secondary-container: '#3a6c57'
  tertiary: '#326a36'
  on-tertiary: '#ffffff'
  tertiary-container: '#69a46a'
  on-tertiary-container: '#00380e'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#72fe86'
  primary-fixed-dim: '#54e16d'
  on-primary-fixed: '#002107'
  on-primary-fixed-variant: '#00531b'
  secondary-fixed: '#b9eed3'
  secondary-fixed-dim: '#9dd2b8'
  on-secondary-fixed: '#002115'
  on-secondary-fixed-variant: '#1c4f3c'
  tertiary-fixed: '#b3f2b0'
  tertiary-fixed-dim: '#98d596'
  on-tertiary-fixed: '#002106'
  on-tertiary-fixed-variant: '#185121'
  background: '#f2fcf4'
  on-background: '#151d19'
  surface-variant: '#dbe5dd'
  surface-white: '#FFFFFF'
  surface-muted: '#c4dec9'
  text-secondary: '#5A6B60'
  divider: '#E3EDE6'
  risk-low: '#00AE42'
  risk-mid: '#70ab70'
  risk-high: '#124734'
typography:
  display-hero:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-h1:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-h2:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  caption:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  headline-h1-mobile:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  screen-margin: 20px
  card-padding: 20px
  gutter: 16px
  stack-sm: 4px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

The design system is anchored in a philosophy of **Optimistic Friction**—a supportive and non-judgmental approach to digital wellbeing. It is designed to act as a gentle companion rather than a clinical monitor, evoking feelings of growth, hope, and resilience.

The visual style is a blend of **Minimalism** and **Organic Design**. It utilizes generous white space, flat vector illustrations, and soft, rounded geometry to create a "calm" interface. By strictly avoiding alarming colors like red or harsh visual cues (such as exclamation marks), the design system ensures that even "high-risk" states are presented as opportunities for growth and support, symbolized through the density and health of a digital tree metaphor.

## Colors

The "Positive Palette" is the core of this design system. It is a monochromatic-adjacent green scale that replaces traditional error-state logic with varying saturations of green.

- **Primary Green (#00AE42):** Used for actions, progress, and "Low Risk" states. It represents vitality.
- **Primary Dark (#124734):** Used for primary typography and "High Risk" states. It represents depth and stability.
- **Muted Green (#70ab70):** Used for "Mid Risk" states and secondary illustrations.
- **Background (#edf7ef):** A soft, tinted white used for main screen backgrounds to reduce eye strain.

**Strict Constraint:** Red, orange, or traditional "warning" colors are strictly prohibited. Severity is communicated through the transition from vibrant primary green to deep, dark green.

## Typography

This design system uses **Inter** to achieve a clean, systematic, and highly readable interface. The type hierarchy is intentionally sparse to maintain the "airy" feel of the brand.

- **Display Scale:** Reserved for hero statistics, timers, and the central digital wellbeing score.
- **Headlines:** Use Semi-bold weights to provide clear structure without appearing aggressive.
- **Body & Captions:** Use a Regular weight with generous line height (1.5x) to ensure the copy feels approachable and easy to digest.
- **Tone of Voice:** Copy should always be supportive, using "we" instead of "you," and phrasing suggestions as invitations rather than commands.

## Layout & Spacing

The layout philosophy relies on a **Fluid Grid** based on an 8px rhythm. Generous white space is essential to prevent the user from feeling overwhelmed.

- **Margins:** A consistent 20px margin is applied to the left and right of all screens.
- **Rhythm:** Use 16px (stack-md) for vertical spacing between standard elements and 32px (stack-lg) to separate major sections.
- **Safe Areas:** Ensure interactive elements are padded away from screen edges, maintaining a "breathable" center-focused content flow.
- **Dashboards:** Use low-density layouts. Information should be grouped into clear, isolated cards rather than dense lists.

## Elevation & Depth

Visual hierarchy is achieved through **Tonal Layering** and very subtle shadows. The goal is a flat, "paper-like" depth that feels tactile but not heavy.

- **Surface Strategy:** The main background is `#edf7ef`. Cards use `#FFFFFF` to "pop" forward visually.
- **Shadows:** Use a single, soft elevation style for cards: `Y: 2, Blur: 12, Color: #124734 (at 6% opacity)`. This provides a gentle lift without creating dark, "dirty" edges.
- **Interactive Depth:** On press, cards should not sink; instead, provide a subtle scale-down (98%) or a slight color shift to `#c4dec9` to indicate interactivity.
- **Overlays:** Bottom sheets should use a 28px top corner radius and a dimming backdrop to focus the user's attention.

## Shapes

The shape language is organic and friendly. Avoid sharp corners entirely to maintain the supportive brand voice.

- **Cards:** Use a 20px radius. This large radius communicates a modern, app-like friendliness.
- **Buttons:** 16px radius.
- **Chips & Labels:** 12px radius.
- **Progress Elements:** Always pill-shaped (full radius) to evoke the soft curves of nature (stems, leaves).
- **Icons:** Use thin outlines with rounded stroke caps and joins to match the component geometry.

## Components

### Buttons
- **Primary:** 56px height, Background: Primary Green (#00AE42), Text: White, 16px radius.
- **Secondary:** 56px height, Background: Transparent, Border: 1.5px Primary Green, Text: Primary Green, 16px radius.

### Cards & Task Cards
- **Container:** White background, 20px radius, soft 6% shadow.
- **Task Indicator:** A vertical 4px bar or a circular dot on the left side of the card using the Risk Level colors to indicate priority/severity.

### Progress Bars
- **Track:** #c4dec9 (Support Green).
- **Fill:** #00AE42 (Primary Green). 
- **Shape:** Pill-shaped (Full radius). No segments; use a smooth, continuous fill to represent organic growth.

### Chips & Badges
- **Style:** Background: #edf7ef, Text: #124734 (Primary Dark), 12px radius. Use for categories or metadata.

### Navigation
- **Bottom Navigation:** 4 tabs. Icons should be flat vector style. Active state uses Primary Green for the icon and a small dot indicator below; inactive state uses Text Secondary (#5A6B60).

### Inputs
- **Field:** 1.5px border in #E3EDE6, 12px radius. Focus state shifts border to Primary Green. Helper text should always be framed as "supportive hints."