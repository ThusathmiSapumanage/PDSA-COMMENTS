# ALGOCORE - PDSA Coursework

### Description
ALGOCORE is a high-fidelity frontend interface developed for the BSc (Hons) Computing - 25.2 PDSA Coursework. It provides a standardized, responsive execution environment for testing complex algorithms.

### Purpose
The objective of this repository is to unify our group's 5 algorithmic game solutions into a single, cohesive web application. This frontend ensures 100% design consistency, enforces strict input validation, and provides a professional presentation layer for the final submission.

### What This Includes
- **Global Theme Engine**: Dark mode/OLED styling (`#050505`) with custom typography (Syne & Outfit).
- **Core UI Toolkit**: Pre-built, strictly validated input components and alert systems.
- **Game Shell Architecture**: A universal layout template for rendering analytical data and game boards.
- **Main Dashboard**: A fully routed hub linking to all 5 required PDSA problems.

---

## Getting Started (How to Run Locally)

To view the frontend on your machine, follow these steps:

1. **Navigate to the frontend directory:**
   ```bash
   cd ALGOCORE/frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start the development server:**
   ```bash
   npm run dev
   ```

4. **View the Application:**
   Open your browser and navigate to [http://localhost:3000](http://localhost:3000)

---

## The Games Pipeline
We are responsible for integrating the following 5 problems into this UI:
1. **Minimum Cost Problem** (Task-to-Employee Assignment)
2. **Snake and Ladder** (Shortest Path / Dice Throw Optimization)
3. **Traffic Simulation** (Network Max-Flow)
4. **Knight's Tour** (Sequence Tracking on 8x8 / 16x16 Boards)
5. **Sixteen Queens Puzzle** (Sequential vs. Threaded Concurrency comparison)

---

## 📁 Team Instructions - Folder Structure

Please review the following directories before pushing code.

| Directory | Purpose | Usage Strictness |
| :--- | :--- | :--- |
| `frontend/app/` | The routing core. Global CSS and Layouts exist here. | **Do not modify `layout.tsx` or `globals.css`** without notifying the frontend lead. |
| `frontend/app/games/` | *(To be created)* Game-specific directories. | Each team member will create their own folder here (e.g., `.../games/snake/page.tsx`). |
| `frontend/components/forms/` | Core toolkit for inputs, dropdowns, and alerts. | **Mandatory.** Do not use default HTML `<input>`, `<select>`, or `window.alert()`. Use these components. |
| `frontend/components/layouts/` | Contains `GameLayoutShell.tsx`. | **Mandatory.** Wrap your game page logic inside this shell to maintain the standard UI layout. |
| `frontend/public/` | Static assets (Images, icons, animations). | Store your game-specific images here if needed. |
| `logic-java/` | Backend/Algorithm Logic. | Ensure all Java outputs can interface with the Next.js frontend via API or WASM. |

---

## How to Use the UI Toolkit (Strict Rules)

To ensure the system passes all data-integrity checks, you **must** use these custom components for your game controls. 

### 1. `AlgoInput` (For all text/number fields)
Automatically handles validation and real-time error states.
```tsx
import AlgoInput from "@/components/forms/AlgoInput";

// Inside your component:
<AlgoInput
    label="Phone Number"
    type="tel"
    nameType="phone" // Validates SL formats automatically
    value={stateValue}
    onChange={setStateValue}
    required={true}
/>
```

### 2. `AlgoDropdown` (For all Select fields)
Matches the global dark theme.
```tsx
import AlgoDropdown from "@/components/forms/AlgoDropdown";

<AlgoDropdown
    label="Algorithm Approach"
    options={[{ value: "algo1", label: "Algorithm 1" }, { value: "algo2", label: "Algorithm 2" }]}
    value={dropdownState}
    onChange={setDropdownState}
/>
```

### 3. `AlgoNotify` (For all success/error messages)
Replaces `window.alert` or `console.log` for user-facing feedback.
```tsx
import AlgoNotify from "@/components/forms/AlgoNotify";

<AlgoNotify
    show={isAlertVisible}
    type="success" // Allows 'success', 'error', 'warning'
    title="Execution Complete"
    message="Time taken: 12ms. Results sent to database."
    onClose={() => setIsAlertVisible(false)}
/>
```

### 4. `GameLayoutShell` (For your page layout)
Every game page must look identical. Feed your inputs and game board into this component.
```tsx
import GameLayoutShell from "@/components/layouts/GameLayoutShell";

export default function MyGamePage() {
    return (
        <GameLayoutShell
            title="Snake and Ladder"
            gameId="02"
            description="Shortest path evaluation on a dynamic board."
            controls={<> {/* Put AlgoInputs here */} </>}
            visualization={<> {/* Put your custom board/grid here */} </>}
        />
    );
}
```

---

## Git & Version Control (Strict Rules)

**DO NOT PUSH DIRECTLY TO THE `main` BRANCH.**

To keep the codebase clean and prevent merge conflicts, you must only push code to your specifically assigned game branch.

When you clone the repository or start working, switch to your dedicated branch:

- `mincost` - Minimum Cost Problem
- `snakeladder` - Snake and Ladder Game
- `traffic` - Traffic Simulation
- `knightstour` - Knight's Tour Problem
- `queenspuzzle` - Sixteen Queens Puzzle

**Example Workflow:**
```bash
# Switch to your branch before making changes
git checkout traffic

# Do your work...

# Add, commit, and push to YOUR branch
git add .
git commit -m "traffic: Added max-flow UI layout"
git push origin traffic
```

The frontend lead will handle merging these branches into the main application.
