# Cinnamon Game Engine
The purpose of this engine is to learn how game engines work. For now, it's only a playground for me to have fun and build stuff I like and a place to do my uni assignments

# Features
- [x] OpenGL Rendering Pipeline
- [ ] Vulkan Rendering Pipeline
- [x] Assimp Model Loading
- [x] GUI System
- [x] Animations ([blockbench](https://www.blockbench.net/) based)
- [ ] Skeletal Animations
- [x] Sounds System
- [x] AABB Collisions
- [ ] [ODE physics](https://www.ode.org/)
- [x] Entity System
- [x] Physics System
- [x] Items
- [x] Inventory
- [x] Particles
- [x] World System
- [ ] Networking
- [x] Settings File
- [x] Resource Loading
- [x] OpenXR Integration
- [x] Keybinds
- [x] Language Localization System
- [x] Logger
- [ ] Scripting System ([Lua](https://www.lua.org/))
- [ ] Entity AI/Behaviour System
- [ ] What else comes into my very flat brain (burgers)

# Controls
| ACTION         |      BIND | ACTION             |   BIND |
|----------------|----------:|--------------------|-------:|
| Move Forwards  |         W | Hide Hud           |     F1 |
| Move Backwards |         S | Screenshot         |     F2 |
| Strafe Left    |         A | Debug Menu         |     F3 |
| Strafe Right   |         D | Camera Perspective |     F5 |
| Jump           |     Space | Fullscreen         |    F11 |
| Walk           | Left-Ctrl | Reload Assets      |    F12 |
| Sprint         |       Tab | Free Hamburger     | Alt+F4 |
| Fly            |  2x Space |                    |        |

# How to compile
Let Gradle do its thing

# To use as a library
Were going jitpack route here,

On the Gradle build file, add:
```kt
repositories {
    maven("https://jitpack.io")
}
```
and
```kt
dependencies {
    implementation("com.github.meiiraru", "Cinnamon", cinnamonVersion)
}
```
dont forgor to include the LWJGL modules (and natives) you're going to use
