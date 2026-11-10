---
name: Academic Precision
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#444651'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#757682'
  outline-variant: '#c5c5d3'
  surface-tint: '#4059aa'
  primary: '#00236f'
  on-primary: '#ffffff'
  primary-container: '#1e3a8a'
  on-primary-container: '#90a8ff'
  inverse-primary: '#b6c4ff'
  secondary: '#006e2d'
  on-secondary: '#ffffff'
  secondary-container: '#7cf994'
  on-secondary-container: '#007230'
  tertiary: '#00285e'
  on-tertiary: '#ffffff'
  tertiary-container: '#003d88'
  on-tertiary-container: '#82abff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dce1ff'
  primary-fixed-dim: '#b6c4ff'
  on-primary-fixed: '#00164e'
  on-primary-fixed-variant: '#264191'
  secondary-fixed: '#7ffc97'
  secondary-fixed-dim: '#62df7d'
  on-secondary-fixed: '#002109'
  on-secondary-fixed-variant: '#005320'
  tertiary-fixed: '#d8e2ff'
  tertiary-fixed-dim: '#adc6ff'
  on-tertiary-fixed: '#001a42'
  on-tertiary-fixed-variant: '#004395'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system establishes a focused, reassuring, and streamlined atmosphere tailored to the dynamic pace of university life. Designed primarily for mobile interactions in lecture halls and campus corridors, the visual language minimizes cognitive overhead during time-critical attendance check-ins.

The aesthetic follows an institutional yet contemporary minimalist direction:
- **Clean surfaces**: Generous negative space balances information density, providing immediate clarity when navigating between schedules, camera viewports, and records.
- **Academic authority meets modern utility**: Classic collegiate navy anchors the visual identity, while energetic blue and emerald accents guide actionable decisions.
- **Frictionless focus**: Eliminates visual clutter so students and faculty can complete QR validation within seconds.

## Colors
The palette balances institutional trust with high-contrast accessibility in daylight campus settings:

- **Primary (`#1E3A8A`)**: University Navy serves as the structural foundation, applied to top navigation bars, key brand anchors, and prominent interactive triggers.
- **Secondary / Success (`#16A34A`)**: Emerald Green denotes verified status, successful QR check-in states, and on-time attendance records ("Presente").
- **Tertiary / Action Accent (`#3B82F6`)**: Bright Blue highlights active navigation items, camera guides, informational notices, and progressive indicators.
- **Warning & Error (`#D97706` / `#DC2626`)**: Amber handles late arrivals ("Retardo"), while deep crimson signals absences ("Falta") or scan failures.
- **Neutrals & Surfaces**:
  - `Canvas Background`: `#F8FAFC` creates a soft, glare-free backdrop.
  - `Card / Surface Background`: Pure `#FFFFFF` ensures crisp contrast against canvas tones.
  - `Secondary Surface`: `#F1F5F9` provides distinction for inset widgets, search inputs, and inactive tabs.
  - `Borders & Dividers`: `#E2E8F0` defines structured divisions without harsh outlines.
  - `Text Primary`: Deep Graphite (`#0F172A`) ensures WCAG AAA readability for critical labels and metrics.
  - `Text Secondary & Muted`: Slate (`#64748B`) provides effortless scanning for timestamps, course codes, and subheadings.

## Typography
Inter delivers crisp geometric legibility at small sizes on varied mobile displays. 

- **Display & Section Headers**: Tight negative letter-spacing (`-0.01em` to `-0.02em`) on headline styles keeps course titles, greeting lines, and summary numbers compact and punchy.
- **Body Text**: Generous vertical line heights prevent reading fatigue when viewing dense attendance logs and syllabus breakdowns.
- **Labels & Microcopy**: Form tags, attendance badges, and scanner instructions leverage uppercase tracking or semi-bold weights for rapid optical recognition.

## Layout & Spacing
The layout follows a mobile-first, 4-column fluid grid system engineered for thumb reachability:

- **Screen Margins**: Uniform `1.25rem` (20px) horizontal gutters anchor main content blocks, preventing thumb cramp while maximizing viewport efficiency.
- **Vertical Rhythm**: 
  - Standard spacing uses multiples of 8px (`0.5rem`, `1rem`, `1.5rem`, `2rem`), maintaining systematic visual balance.
  - Safe area insets accommodate iOS dynamic islands, notches, and home indicators.
- **Navigation Bounds**: Bottom bar adheres to a fixed height of 68px (excluding device safe areas) with dedicated tap targets of at least 48x48px.

## Elevation & Depth
Depth is maintained through soft, diffused ambient light rather than stark shadows:

- **Level 0 (Flat)**: Background canvas `#F8FAFC` and passive inline fields (`#F1F5F9`) with no shadow.
- **Level 1 (Card & List Item)**: Surface `#FFFFFF` enclosed with a subtle stroke `1px solid #E2E8F0` and an ambient glow: `0px 2px 8px -2px rgba(15, 23, 42, 0.05), 0px 1px 3px 0px rgba(15, 23, 42, 0.03)`.
- **Level 2 (Interactive Floating Elements)**: Floating QR trigger buttons and active search sheets utilize elevated elevation: `0px 8px 24px -4px rgba(30, 58, 138, 0.12), 0px 4px 12px -2px rgba(15, 23, 42, 0.04)`.
- **Level 3 (Modals & Scan Overlays)**: Confirmation bottom-sheets and dialog cards apply deep soft drop shadows: `0px 20px 32px -8px rgba(15, 23, 42, 0.16)`.

## Shapes
The shape language implements modern curvature (`rounded-2xl` / 16px radius) across cards, modal containers, and interactive components. 

- **Cards & Sheets**: Master panels and status containers utilize `1rem` (16px) corner rounding to maintain a friendly, approachable touchpoint.
- **Buttons & Inputs**: Inputs and primary actions adopt matching `0.875rem` to `1rem` radii, forming a coherent visual contour.
- **Badges & Micro-elements**: Status pills and active indicators use full pill rounding (`9999px`) to create distinction from content cards.

## Components

### Buttons
- **Primary Button**: Solid `#1E3A8A` background, `#FFFFFF` text, `rounded-2xl` corners, 48px minimal height. Active states trigger subtle micro-interaction compression (`transform: scale(0.98)`).
- **Secondary Button**: `#FFFFFF` background with `1px solid #E2E8F0`, `#0F172A` text, hover/active transition to `#F8FAFC`.
- **Floating Scan Trigger**: Centered bottom navigation action features a prominent circular/squircle form with primary blue background, dual ambient shadows, and an animated optical pulse during active classes.

### Attendance Status Badges
Status indicators leverage a two-tone model (tinted background with deep saturated text):
- **Presente**: Background `#DCFCE7`, Text `#15803D`, Border `1px solid #BBF7D0`.
- **Retardo**: Background `#FEF3C7`, Text `#B45309`, Border `1px solid #FDE68A`.
- **Falta**: Background `#FEE2E2`, Text `#B91C1C`, Border `1px solid #FECACA`.
Badges employ fully rounded pill geometry (`rounded-full`), `space-xs` vertical padding, and `space-sm` horizontal padding.

### Cards & Class Schedule Blocks
- Encased in `#FFFFFF` with `rounded-2xl` geometry and Level 1 elevation.
- Includes course code badge, time slot, instructor name, and classroom designation.
- Left-edge accent strips (3px solid `#1E3A8A` or `#16A34A`) indicate live versus completed sessions.

### Input Fields
- Background `#FFFFFF` or `#F8FAFC` paired with `1px solid #E2E8F0`.
- Focus state activates `border-color: #3B82F6` and a soft glow (`box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15)`).
- Clear action triggers and prefix icons in `#64748B`.

### Navigation Bar
- Fixed 3-tab layout: **Inicio**, **Escanear (QR)**, **Historial**.
- Frosted surface (`rgba(255, 255, 255, 0.9)` with backdrop blur) bordered by a top hairline `1px solid #E2E8F0`.
- Inactive tabs display `#64748B`; active tab shifts to `#1E3A8A` paired with an animated micro-dot indicator below the label.