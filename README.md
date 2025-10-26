# Skybound

## Quick Start
- **Prerequisites:** Java 17+ (JDK).
- **Build & run:**
Open a terminal in the Skybound folder and run the following in succession:
  ```bash
  javac -d out src/*.java
  java -cp out Skybound
  ```
- The game launches into the main menu and is playable well within one minute.

## Gameplay & Controls
- `A` / `←` move left, `D` / `→` move right. Movement wraps horizontally so you reappear on the other edge (`src/GamePanel.java:174`).
- Land on platforms to keep climbing; cyan booster platforms give a stronger jump (`src/GamePanel.java:197`).
- Avoid enemies, touching one without a jetpack ends the run (`src/GamePanel.java:272`).
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
| Priority | Name | How to demo | Notes |
| --- | --- | --- | --- |
| 1 | Core Window & Game Loop | Launch the game; a window opens and the score counter is ready to track your climb. | Sets up the `JFrame` and the Swing timer-driven update cycle that everything else builds on (`src/Skybound.java:12`, `src/GamePanel.java:91`). |
| 1 | Player Movement & Physics | Hold `A`/`D` to move; let go to see friction; watch gravity pull the player down. | Horizontal acceleration, friction, and gravity constants live in the update loop (`src/GamePanel.java:150`). |
| 1 | Collision Detection | Land on platforms and collide with enemies without clipping through them. | Uses the `Rect` helper to resolve landings and side hits (`src/Rect.java:36`, `src/GamePanel.java:187`). |
| 1 | Static Platform | Bounce on a standard platform and see the auto-jump reset. | Base platform behaviour handled when landing (`src/GamePanel.java:197`). |
| 1 | Procedural Platform Generation | Keep climbing; observe that new rows appear without a fixed pattern yet remain reachable. | `ObjectManager.spawnRowAbove` rolls random offsets and widths as the camera rises (`src/ObjectManager.java:61`, `src/GamePanel.java:225`). |
| 1 | Entity & Object Tracking | Early on, see plenty of platforms; later, gaps widen and enemies appear more often while power-up chance stays steady. | Centralised in `ObjectManager`, which tracks active objects and their spawn rules (`src/ObjectManager.java:9`, `src/ObjectManager.java:93`). |
| 1 | Camera Follow & Cleanup | Jump upward and watch the camera track you; falling below the camera ends the run. | Camera threshold logic and pruning live in the update loop (`src/GamePanel.java:207`, `src/GamePanel.java:280`). |
| 1 | HUD & Score | Score increases with height; high score persists after death. | Overlay text rendered with each frame; high score updated on death (`src/GamePanel.java:355`, `src/GamePanel.java:284`). |
| 2 | Start Menu | Opening screen shows start/quit buttons; clicking start swaps to gameplay. | Menu panel swaps in the `GamePanel` through a callback (`src/MainMenu.java:55`, `src/Skybound.java:19`). |
| 2 | Game Over Flow | Fall off or hit an enemy to trigger the game-over overlay with restart prompt. | Game state toggles to dead and shows the overlay (`src/GamePanel.java:377`). |
| 2 | Bouncy Pad Platform | Land on a booster platform to get a visibly higher launch. | Boost behaviour is toggled with a boolean flag; no separate subclass needed (`src/GamePanel.java:197`, `src/ObjectManager.java:67`). |
| 2 | Enemy – SPAR Crocodile | Encounter the pink croc moving sideways; touching it ends the run unless the jetpack is active. | Enemy movement and collision handled in the update loop, with jetpack immunity baked into the hit check (`src/GamePanel.java:248`, `src/GamePanel.java:272`). |
| 2 | Power-Up Framework | Collect a power-up; the jetpack effect kicks in and the player sprite swaps while the timer runs. | Power-ups are tracked and applied via shared manager hooks (no HUD icon yet) (`src/ObjectManager.java:20`, `src/GamePanel.java:234`, `src/GamePanel.java:424`). |
| 2 | URE FS Car Jetpack | Grab the car sprite to gain a 3-second jetpack burst that ignores enemies. | Jetpack activation clamps vertical speed and shows a special sprite; immunity comes from the collision guard (`src/GamePanel.java:484`, `src/GamePanel.java:272`). |
| 3 | Procedural Difficulty Scaling | Notice platform gaps stretch and enemies spawn more frequently as your score climbs. | Difficulty curve driven by score-based gap calculation and the enemy spawn probability ramp (`src/ObjectManager.java:52`, `src/ObjectManager.java:93`). |
| 4 | Pause Menu | Press `P` to freeze the game and show the “Paused” overlay; press again to resume. | Pause flag halts updates and displays an overlay (`src/GamePanel.java:59`, `src/GamePanel.java:361`, `src/GamePanel.java:463`). |
| 4 | Parallax Background | Watch the sky scroll slower than the platforms for a depth effect. | Backdrop scrolls using parallax math in `drawBackdrop` (`src/GamePanel.java:394`). |
