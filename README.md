# Skybound

## Quick Start
- **Prerequisites:** Java 17+ (JDK) and a POSIX-compatible shell.
- **Build & run:**
  ```bash
  mkdir -p out
  javac -d out src/*.java
  java -cp out Skybound
  ```
- The game launches into the main menu and is playable well within the one-minute requirement.

## Gameplay & Controls
- `A` / `←` move left, `D` / `→` move right. Movement wraps horizontally so you reappear on the other edge (`src/GamePanel.java:174`).
- Land on platforms to keep climbing; cyan booster platforms give a stronger jump (`src/GamePanel.java:197`).
- Avoid enemies—touching one without a jetpack ends the run (`src/GamePanel.java:272`).
- Collect a jetpack pick-up to trigger a 3-second burst of upward thrust (`src/GamePanel.java:73`, `src/GamePanel.java:484`).
- Fall too far below the camera and you lose; press `SPACE` to restart from the menu or after death (`src/GamePanel.java:441`).

## Architecture Highlights
- **Game loop & state:** `GamePanel` owns the Swing timer, update loop, rendering, and input handling in a single cohesive panel (`src/GamePanel.java:79`).
- **Object coordination:** `ObjectManager` manages every platform, enemy, and power-up so the panel can request high-level actions instead of juggling lists (`src/ObjectManager.java:9`).
- **UI flow:** `MainMenu` provides a self-contained menu panel that swaps to the live game when the start button fires its callback (`src/MainMenu.java:55`).
- **Data holders:** Lightweight classes such as `Player`, `Platform`, `Powerup`, `Enemy`, and `Rect` make the physics and rendering code readable while keeping responsibilities narrow (`src/Player.java:1`, `src/Platform.java:1`, `src/Powerup.java:1`, `src/Enemy.java:1`, `src/Rect.java:1`).

## Advanced Topic 1 – Advanced OOP
- **Subsystem encapsulation:** The game loop interacts with a single `ObjectManager` instance instead of mutating raw collections, which keeps spawning logic isolated and testable (`src/GamePanel.java:110`, `src/ObjectManager.java:9`).
- **Dependency injection:** Screen dimensions and the shared RNG are injected through the manager constructor, letting us tune difficulty or seed values without touching the loop (`src/ObjectManager.java:41`).
- **Clear data flow:** Render code reads immutable state while collision helpers reuse the `Rect` utility, giving us clean separation between state, logic, and presentation (`src/Rect.java:1`, `src/GamePanel.java:307`).
- **UI hand-off:** `Skybound.main` wires the menu and gameplay panel through callbacks, demonstrating loose coupling between UI states (`src/Skybound.java:15`, `src/MainMenu.java:55`).

## Advanced Topic 2 – Procedural Generation
- **Adaptive pacing:** Platform gaps scale with score via a simple easing function, gradually increasing difficulty while staying predictable (`src/ObjectManager.java:52`).
- **Layered platform rows:** Each `spawnRowAbove` call mixes main platforms, occasional secondary ledges, and optional boosters for variety (`src/ObjectManager.java:61`).
- **Enemy heuristics:** Enemies spawn with distance checks against the player and each other, capping the count and avoiding unfair overlaps (`src/ObjectManager.java:88`).
- **Endless world streaming:** As the camera rises, new rows spawn while off-screen items get removed, keeping memory usage stable and the climb endless (`src/GamePanel.java:225`, `src/GamePanel.java:280`).

## Product Backlog Snapshot
| Name | How to demo | Notes |
| --- | --- | --- |
| Core jump loop | Start a run and climb by landing on platforms. | Physics, wrapping, and scoring live in the update loop (`src/GamePanel.java:150`). |
| Booster platforms | Land on a cyan booster and feel the stronger launch. | Boost handling is part of the landing check (`src/GamePanel.java:197`). |
| Jetpack power-up | Collect the car icon to trigger the timed jetpack lift. | Activation and decay happen in the power-up logic (`src/GamePanel.java:484`). |
| Enemy hazards | Keep climbing until a croc spawns; collide to see the death flow. | Spawn rules and collision checks sit in the manager and update loop (`src/ObjectManager.java:88`, `src/GamePanel.java:272`). |
| Main menu & restart | Launch the game, click `START`, and press `SPACE` after you die. | Menu wiring and restart handling are separated between the panels (`src/MainMenu.java:55`, `src/GamePanel.java:441`). |