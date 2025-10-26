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
- `P` toggles a pause overlay; the loop halts until you press `P` again (`src/GamePanel.java:59`, `src/GamePanel.java:361`, `src/GamePanel.java:463`).
- Fall too far below the camera and you lose; press `SPACE` to restart from the menu or after death (`src/GamePanel.java:441`).

## Architecture Highlights
- **Game loop & state:** `GamePanel` owns the Swing timer, update loop, rendering, and input handling in a single cohesive panel (`src/GamePanel.java:79`).
- **Object coordination:** `ObjectManager` manages every platform, enemy, and power-up so the panel can request high-level actions instead of juggling lists (`src/ObjectManager.java:9`).
- **UI flow:** `MainMenu` provides a self-contained menu panel that swaps to the live game when the start button fires its callback (`src/MainMenu.java:55`).
- **Data holders:** Lightweight classes such as `Player`, `Platform`, `Powerup`, `Enemy`, and `Rect` make the physics and rendering code readable while keeping responsibilities narrow (`src/Player.java:1`, `src/Platform.java:1`, `src/Powerup.java:1`, `src/Enemy.java:1`, `src/Rect.java:1`).

## Advanced Topic 1 – Advanced OOP
- **Separating game logic from object management:** Instead of `GamePanel` juggling raw lists, `ObjectManager` owns every platform, enemy, and power-up (`src/ObjectManager.java:9`). The update loop simply calls helpers like `platformGapForScore`, `spawnRowAbove`, and `maybeSpawnEnemy` while it runs (`src/GamePanel.java:225`), letting us change the internal rules without touching the loop.
- **Making the code flexible with dependency injection:** Screen size and the shared RNG go through the manager constructor (`src/ObjectManager.java:41`), so tweaking difficulty values such as `baseGap` and `maxGap` happens inside the manager (`src/ObjectManager.java:33`) instead of in the game loop.
- **Keeping enemy spawning organized:** All spawn caps, distance checks, and fairness rules live in `maybeSpawnEnemy`, with every guard clause in one place (`src/ObjectManager.java:88`). `GamePanel` just triggers the method and moves on (`src/GamePanel.java:232`).
- **Dividing responsibilities between classes:** Rendering code like `drawPlayer` only draws the data it receives (`src/GamePanel.java:405`), while collision checks reuse the standalone `Rect` helper for the maths (`src/Rect.java:36`). That split keeps physics, rendering, and data holders tidy.

## Advanced Topic 2 – Procedural Generation
- **Making platforms harder as you progress:** `platformGapForScore` adjusts the vertical spacing based on the current score, so the climb gradually stretches out as you get better (`src/ObjectManager.java:52`).
- **Creating random platform layouts:** `spawnRowAbove` rolls random positions, widths, booster chances, and secondary ledges whenever a new row gets added above the camera (`src/ObjectManager.java:61`).
- **Smart enemy placement:** `maybeSpawnEnemy` samples multiple candidates but rejects anything too close to the player or another enemy, enforcing a hard cap of two active enemies for fairness (`src/ObjectManager.java:88`).
- **Infinite world generation:** Each update spawns fresh rows just above the view (`src/GamePanel.java:225`) and prunes anything that falls well below the camera (`src/GamePanel.java:280`), giving us an endless level without handcrafted layouts.

## Product Backlog Snapshot
| Name | How to demo | Notes |
| --- | --- | --- |
| Core jump loop | Start a run and climb by landing on platforms. | Physics, wrapping, and scoring live in the update loop (`src/GamePanel.java:150`). |
| Booster platforms | Land on a cyan booster and feel the stronger launch. | Boost handling is part of the landing check (`src/GamePanel.java:197`). |
| Jetpack power-up | Collect the car icon to trigger the timed jetpack lift. | Activation and decay happen in the power-up logic (`src/GamePanel.java:484`). |
| Enemy hazards | Keep climbing until a croc spawns; collide to see the death flow. | Spawn rules and collision checks sit in the manager and update loop (`src/ObjectManager.java:88`, `src/GamePanel.java:272`). |
| Main menu & restart | Launch the game, click `START`, and press `SPACE` after you die. | Menu wiring and restart handling are separated between the panels (`src/MainMenu.java:55`, `src/GamePanel.java:441`). |
| Pause menu | Press `P` to freeze the action and show the pause overlay, press again to resume. | Pause state, overlay, and key handling sit in the game panel (`src/GamePanel.java:59`, `src/GamePanel.java:361`, `src/GamePanel.java:463`). |
