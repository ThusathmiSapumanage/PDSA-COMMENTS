# ALGOCORE Design Guide

> Universal design reference for all 5 game modules. Follow this guide to keep UI consistent across branches.

---

## Brand

- **Name:** ALGOCORE
- **Tagline:** Advanced Algorithmic Game Suite
- **Logo:** `/public/ALGOCORE-Logo.png`
- **Style:** Dark, minimal, technical. Think sci-fi terminal meets modern dashboard.

---

## Color Palette

### Core Colors

| Token              | Hex         | Usage                                      |
|--------------------|-------------|---------------------------------------------|
| `--background`     | `#050505`   | Page background (OLED black)                |
| `--foreground`     | `#FFFFFF`   | Primary text                                |
| `--surface`        | `#0A0A0A`   | Cards, panels, containers                   |
| `--surface-alt`    | `#111111`   | Hover states on surfaces, secondary panels  |

### Accent Colors

| Token        | Tailwind Class     | Hex         | Usage                                       |
|--------------|---------------------|-------------|----------------------------------------------|
| Primary      | `blue-500`          | `#3B82F6`   | Links, active states, focus rings, CTAs      |
| Primary Dark | `blue-600`          | `#2563EB`   | Logo accent, gradient endpoints, tags        |
| Success      | `green-500`         | `#22C55E`   | Correct answers, results panel headers       |
| Error        | `red-500`           | `#EF4444`   | Validation errors, wrong answers, alerts     |
| Warning      | `amber-500`         | `#F59E0B`   | Warnings, time-critical indicators           |

### Text Opacity Scale

| Class           | Opacity | Usage                                      |
|-----------------|---------|---------------------------------------------|
| `text-white`    | 100%    | Headings, primary content                   |
| `text-white/60` | 60%     | Secondary text, icons                       |
| `text-white/50` | 50%     | Descriptions, body copy                     |
| `text-white/40` | 40%     | Labels, meta text, breadcrumbs              |
| `text-white/30` | 30%     | Game IDs, serial numbers                    |
| `text-white/20` | 20%     | Decorative text, watermarks                 |
| `text-white/10` | 10%     | Dividers (vertical bars), placeholders      |
| `text-white/5`  | 5%      | Giant background typography                 |

### Border Opacity Scale

| Class              | Usage                                |
|--------------------|---------------------------------------|
| `border-white/10`  | Default borders, dividers             |
| `border-white/5`   | Subtle card borders, panel outlines   |

---

## Typography

### Font Stack

| Role      | Font    | CSS Variable      | Tailwind Class   | Usage               |
|-----------|---------|---------------------|-------------------|----------------------|
| Display   | Syne    | `--font-syne`       | `font-display`    | Headings (h1-h6)    |
| Body      | Outfit  | `--font-outfit`     | `font-sans`       | Body text, UI        |
| Mono      | System  | —                   | `font-mono`       | Labels, tags, IDs    |

### Type Scale

| Element           | Classes                                                              |
|--------------------|----------------------------------------------------------------------|
| Page Title (h1)    | `text-3xl md:text-5xl font-black tracking-tighter uppercase`        |
| Section Title (h2) | `text-5xl md:text-7xl font-black tracking-tighter uppercase`        |
| Card Title (h3)    | `text-xl font-bold uppercase tracking-tight`                        |
| Subsection Label   | `text-xs font-mono tracking-[0.3em] uppercase text-blue-500`        |
| Meta Label         | `text-[10px] font-mono tracking-[0.3em] uppercase text-white/40`    |
| Tag / Badge        | `text-[10px] font-bold tracking-[0.2em] uppercase`                  |
| Body Text          | `text-sm md:text-base leading-relaxed`                              |
| Small Print        | `text-[10px] font-mono tracking-widest uppercase`                   |

---

## Spacing System

All spacing uses Tailwind's default 4px base.

### Page-Level

| Property              | Value               | Notes                          |
|------------------------|---------------------|--------------------------------|
| Page horizontal pad    | `px-6 lg:px-12`     | Responsive                     |
| Page top padding       | `pt-24`             | Clears fixed header            |
| Page bottom padding    | `pb-12`             |                                |
| Max content width      | `max-w-[1800px]`    | Homepage sections              |
| Max content width (nav)| `max-w-7xl`         | Header nav                     |

### Section-Level

| Property               | Value          | Notes                          |
|-------------------------|----------------|--------------------------------|
| Section vertical pad    | `py-40`        | Between major sections         |
| Section header margin   | `mb-24`        | Below section headers          |
| Border bottom padding   | `pb-10`        | Section header divider         |

### Card / Panel-Level

| Property               | Value          | Notes                          |
|-------------------------|----------------|--------------------------------|
| Card padding            | `p-8`          | Standard internal padding      |
| Card border radius      | `rounded-xl`   | 12px                          |
| Card border             | `border border-white/5` | Subtle outline          |
| Card background         | `bg-[#0a0a0a]` | Surface color                 |
| Gap between items       | `gap-8`        | Standard spacing              |

### Game Cards (Homepage)

| Property               | Value                            |
|-------------------------|-----------------------------------|
| Grid                    | `grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6` |
| Card image aspect ratio | `aspect-[3/4]`                   |
| Card image border radius| `rounded-lg`                     |
| Title margin top        | `mt-3`                           |
| Card padding            | `p-0` (edge-to-edge image)       |

### Form Elements

| Property               | Value          | Notes                          |
|-------------------------|----------------|--------------------------------|
| Input bottom margin     | `mb-8`         | Between form fields            |
| Input vertical padding  | `py-4`         | Inside input                   |
| Label margin bottom     | `mb-2`         | Label to input gap             |
| Button padding          | `px-8 py-4`    | Standard CTA buttons           |

---

## Components Reference

### Shared Components (already built — use these, don't rebuild)

| Component           | Path                                       | Purpose                         |
|---------------------|---------------------------------------------|---------------------------------|
| `Header`            | `components/Header.tsx`                    | Fixed top nav with scroll blur  |
| `Footer`            | `components/Footer.tsx`                    | Site footer with branding       |
| `GameLayoutShell`   | `components/layouts/GameLayoutShell.tsx`    | 30/70 split game page layout    |
| `AlgoInput`         | `components/forms/AlgoInput.tsx`           | Validated text input            |
| `AlgoDropdown`      | `components/forms/AlgoDropdown.tsx`        | Animated select dropdown        |
| `AlgoNotify`        | `components/forms/AlgoNotify.tsx`          | Toast notification system       |

### GameLayoutShell Usage

Every game page **must** use `GameLayoutShell`. It provides:
- Breadcrumb navigation back to home
- 30% controls sidebar (left) + 70% visualization panel (right)
- Fullscreen toggle on visualization
- Results panel (optional)

```tsx
import GameLayoutShell from "@/components/layouts/GameLayoutShell";

export default function YourGamePage() {
  return (
    <GameLayoutShell
      title="Your Game Name"
      gameId="01"
      description="Short description of the game."
      controls={<>{/* Your form inputs here */}</>}
      visualization={<>{/* Your game visualization here */}</>}
      results={<>{/* Optional results panel */}</>}
    />
  );
}
```

### Form Components Usage

**AlgoInput** — for text, number, name, email, phone inputs:
```tsx
<AlgoInput
  label="Player Name"
  type="text"
  nameType="name"         // "name" | "phone" | "email" | "none"
  value={name}
  onChange={setName}
  required
/>
```

**AlgoDropdown** — for option selection:
```tsx
<AlgoDropdown
  label="Algorithm"
  options={["BFS", "Dijkstra"]}
  value={selected}
  onChange={setSelected}
  placeholder="Select algorithm..."
/>
```

**AlgoNotify** — for result feedback:
```tsx
<AlgoNotify
  show={showResult}
  type="success"          // "success" | "error" | "warning"
  title="Correct!"
  message="You found the right answer."
  onClose={() => setShowResult(false)}
  duration={5000}         // auto-dismiss in ms
/>
```

---

## Patterns & Effects

### Noise Texture Overlay
Applied globally via `<div className="bg-noise" />` in layout. Do NOT add it per-page.

### Card Gradient Line
Top accent line on panels:
```tsx
<div className="absolute top-0 left-0 w-full h-[2px] bg-gradient-to-r from-blue-600 to-transparent opacity-50" />
```

### Grid Background (Visualization Area)
Soft grid inside visualization panels:
```tsx
<div className="absolute inset-0 opacity-[0.02]"
  style={{
    backgroundImage: 'linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px)',
    backgroundSize: '40px 40px'
  }}
/>
```

### Hover Transitions
- Color transitions: `transition-colors duration-300`
- All transitions: `transition-all duration-500`
- Opacity reveal: `opacity-0 group-hover:opacity-100 transition-opacity`

### Scrollbar
Custom dark scrollbar is defined globally. No per-component scrollbar styling needed.

---

## Game-Specific UI Guidelines

### Win / Lose / Draw States

Each game must show feedback when a player answers. Use `AlgoNotify` with:

| State  | Type      | Suggested Title     |
|--------|-----------|----------------------|
| Win    | `success` | "Correct!"          |
| Lose   | `error`   | "Wrong Answer"      |
| Draw   | `warning` | "Already Found"     |

### Player Name Input
Every game must collect the player's name before or after answering. Use `AlgoInput` with `nameType="name"`.

### Algorithm Timing Display
All games record algorithm execution time. Display in the results panel:
```tsx
<div className="flex justify-between items-center py-3 border-b border-white/5">
  <span className="text-[10px] font-mono uppercase tracking-widest text-white/40">Algorithm 1</span>
  <span className="text-sm font-bold text-blue-500">{time1}ms</span>
</div>
```

### Visualization Area
Each game has unique visualization needs. Keep these consistent:
- **Chess boards** (Queens, Knight's Tour): Use a grid with alternating `bg-white/5` and `bg-white/10` cells
- **Graph networks** (Traffic): Use SVG or Canvas with nodes as circles and edges as lines
- **Board games** (Snake & Ladder): Grid with numbered cells, colored snakes/ladders
- **Matrix** (Min Cost): Scrollable table with highlighted optimal assignments

---

## Icons

No external icon library is used. All icons are inline SVGs. Common icons in the project:

### User / Profile
```tsx
<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
  <circle cx="12" cy="7" r="4" />
</svg>
```

### Fullscreen Toggle
```tsx
// Expand
<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
  <path d="M8 3H5a2 2 0 0 0-2 2v3"/>
  <path d="M21 8V5a2 2 0 0 0-2-2h-3"/>
  <path d="M3 16v3a2 2 0 0 0 2 2h3"/>
  <path d="M16 21h3a2 2 0 0 0 2-2v-3"/>
</svg>

// Collapse
<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
  <path d="M8 3v3a2 2 0 0 1-2 2H3"/>
  <path d="M21 8h-3a2 2 0 0 1-2-2V3"/>
  <path d="M3 16h3a2 2 0 0 1 2 2v3"/>
  <path d="M16 21v-3a2 2 0 0 1 2-2h3"/>
</svg>
```

### Back Arrow
```tsx
<span>&#8592;</span> // ←
```

When you need new icons, use inline SVGs from [Lucide](https://lucide.dev) (same style as existing icons — 24x24 viewBox, stroke-based, 2px stroke width).

---

## File & Folder Conventions

### Frontend Structure
```
frontend/
  app/
    games/
      your-game-name/     # kebab-case folder
        page.tsx           # Main game page (uses GameLayoutShell)
  components/
    forms/                 # Shared form components (DO NOT duplicate)
    layouts/               # Layout shells
    [GameName].tsx          # Game-specific components if needed
```

### Backend Structure
```
games-backend/
  src/main/java/com/pdsa/games/
    yourgame/              # lowercase package name
      YourGameController.java
      YourGameService.java
      YourGameModel.java
      YourGameRepository.java
```

### Branch Naming
Each game has its own branch: `mincost`, `snakeladder`, `traffic`, `knightstour`, `queenspuzzle`

---

## Do's and Don'ts

### Do
- Use the shared components (`AlgoInput`, `AlgoDropdown`, `AlgoNotify`, `GameLayoutShell`)
- Keep the dark theme consistent — no light backgrounds
- Use `font-mono` for labels, IDs, and technical text
- Use `font-display` (Syne) for headings only
- Use `uppercase tracking-[0.2em]` or `tracking-[0.3em]` for labels
- Record algorithm execution times in the database
- Collect player name before saving results
- Show win/lose/draw feedback using `AlgoNotify`

### Don't
- Don't add new color schemes or fonts
- Don't use external CSS frameworks (no Bootstrap, no Material UI)
- Don't add the noise texture overlay per-page (it's global)
- Don't create new layout shells — use `GameLayoutShell`
- Don't hardcode API URLs — use environment variables or relative paths
- Don't skip validation on user inputs
