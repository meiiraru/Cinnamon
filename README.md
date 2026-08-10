# Cinnamon Game Engine
<img width="928" height="396" alt="logo" src="https://github.com/user-attachments/assets/9b66ecf8-737c-41be-8e78-bfc6044e2c88" />

A simple and lightweight game and render engine built in Java on top of OpenGL and [LWJGL](https://www.lwjgl.org/), with a focus on simplicity and entire customization of its features.

The engine is designed to be in use of fast prototyping and development of games, although, can also be used to build more complex games and desktop applications.

It is built with a modular approach, allowing for easy extension and modification of its features.

It can also be used as a library, allowing for easy integration into other projects.

***

# Features
* [Blockbench](https://www.blockbench.net/) animations
    * Model part based (no bones, no mesh deformation)
    * Applied per transform channel
    * Keyframes based (linear and catmullrom interpolations)
* Messages/Console System
    * Console and command parser to allow for easy debugging and testing the world around
    * Also a message system that works similar to a chat system
    * Messages can be sent by different sources, and can be filtered by type
* Event System
    * Hooks for varous events in the engine to be run by the target application
    * Allows for easy modding and extending the engine
    * Also includes a simple Await system to queue events to be run in the future
* GUI System
    * Custom GUI system built based around widgets and containers hierarchy
    * Fully visually customizable, with the ability to use custom fonts and textures through GUI Skins
    * Widgets can be aligned in all 9 alignment directions
    * Input events are handled by the Screen and then the containers/widgets
    * Can be fully extended to add new widgets types
    * Widgets can also have tooltips and different popup menus
    * Toast system for quick user notifications
* Keybind/Controller System
    * Allows for easy keybinds and controller bindings
    * Bindings can be set from either a controller or a keyboard/mouse
    * Runs only inside the world
    * Each controller can have multiple bindings, and each binding can have multiple actions
    * Bindings are set in a per-entity basis
* Language Localization System
    * Allows for easy localization of any text in the engine
    * Set in a key-value pair format
    * Can be extended to support any language
* Logger System
    * Simple logger system to log messages to the console and a file
    * Can be extended to log to other outputs
    * Can be customized to log different levels of messages and in different message formats
* Math Modules
    * Includes a math module with various math functions that helps and extends the [joml](https://github.com/JOML-CI/JOML) library
    * Includes easing functions, timers (delta time), transforms matrices, FFT calculations, noise generations, parametric curves and more
* Collision System
    * AABB vs OBB vs Sphere collisions detection and resolution
    * Separating Axis Theorem (SAT) based detection
    * Raycasting and ray intersection tests
    * Collider intersection and sweep tests
    * Different collision solvers strategies like, Stick, Slide, Bounce, Force and Push
* Meshes and Geometry
    * Optimized for the OBJ format, but also supports other formats through the [Assimp](https://github.com/assimp/assimp) library
    * Supports loading of meshes, materials, textures and animations
    * Primitive Geometry can be created at runtime through helper functions
    * Vertices can be transformed and manipulated at runtime
    * Supports custom vertex attributes and shaders
* Mesh IO
    * Importing and exporting of OBJ meshes
    * Converter between raw Vertex and the OBJ format
    * Mesh Merger
    * Export of parametric curves into meshes
* Registry
    * Serializable system to use as a lookup for different types of objects in the engine
* Rendering System
    * OpenGL Rendering Pipeline
        * Targeted to OpenGL 4.3
    * Supports immediate mode rendering and batched rendering
    * Batched rendering is used more with raw vertices and the GUI system through a Vertex Consumer
    * Supports custom shaders and custom framebuffers
    * Supports post-processing effects through a PostProcessing Pipeline
    * Supports multiple cameras and viewports
    * Can also offscreen render to a texture or a cubemap
    * Mesh rendering system that supports different mesh formats and materials
    * Material system based on the [MTL-PBR](https://en.wikipedia.org/wiki/Wavefront_.obj_file#Physically-based_rendering) format
    * Built-in support for lights and shadows (Directional Light, Point Light, Spotlight and Cookie Light)
    * Custom built-in effects for different rendering features, like, Bloom, SSAO, SSR, Outlines and more
* Resource Loading System
  * Simple resource loading system that loads resources from the classpath or from the filesystem
  * The resource is namespaced based, allowing for multiple resources with the same name to exist in different namespaces
  * The "vanilla" namespace is reserved for the engine resources
  * An empty resource tries to fetch the path from the machine file system
  * Resources are loaded through their respective loaders
  * Resources are cached and kept in memory until they are manually unloaded 
* Settings System
    * Simple settings system that loads and saves the settings to a JSON file
    * Supports versioning and migration of settings files across different versions
    * Supports different types of settings, like, boolean, integer, float, string and enum
* Sound System
    * Simple sound system through OpenAL
    * Supports only the ogg format
    * Supports spatial sound, alongside volume, pitch, looping, attenuation and distance
    * Full playback control of the sound
    * Sound category system, so different sounds can be grouped together and controlled by a single volume setting
* Text Object
    * Simple text wrapper that allows for customization of text
    * The text object can have more texts attached to it
    * Each text object can have its own style
    * Style system to allow for customization of the text
    * The style supports italic, bold, colors, font, shadow, outline, background and more
    * Styles are chained through the text children, meaning the parent style is also applied to its children
    * Supports click and hover events, allowing for interactive text
* Utility Classes
    * Includes various utility classes to help with different tasks in the engine, like, file IO, colors, text, UI and mode
* Virtual Reality
    * Full OpenXR integration, allowing for VR support in the engine
    * Supports different VR devices and controllers
    * Unsupported controllers can be mapped through a custom controller mapping file
    * Supports getting the position, orientation and input of the controllers and the headset
    * Supports mirroring the view of the headset to the main window
* World System
    * Game world system that allows for the easy creation of a level
    * Supports different types of entities, terrain, lights, cameras and more
    * World generation can be done through a custom world generator, or through a loader
    * Supports tile-based voxel terrain
    * Everything in the world can be edited at runtime
    * Supports saving and loading of the world to a file
    * Supports for a HUD view of the player information, alongside a first person overlay
* Entity System
    * Class based entity system
    * Entities can have a simple built-in physics system
    * Support for living entities like players and NPCs
    * Support for items and inventory system
    * Support for projectiles and vehicles
    * Special entity type to allow for VR input and interaction
* Particle System
    * Simple particle system for easy creation of particle effects
    * Supports Quad (Sprite) particles, alongside Mesh and Text particles
    * Particles can have billboarding, motion, colision, color, emission, lifetime and more
* Terrain System
    * Mesh based terrain objects with support for different materials
    * Collision mask against entities
    * Terrains can be interacted by the player and other entities
    * Octree based terrain storage
* Skybox
    * Cubemap texture based skybox
    * Dynamic procedural skybox with stars, fog and sky colors
* Game Loop
    * Game loop split into tick and render phases
    * Tick phase is where the game logic is updated, run at 20 ticks per second
    * Render phase is where the game is rendered, most things are lerped between ticks to allow for smooth rendering
* Initialization
    * Support for custom command line arguments
    * Window creation and management, alongside user input and the OpenGL context through GLFW
    * Initial window settings can be set before running the engine

# Future considerations
- [ ] Vulkan Rendering Pipeline
- [ ] External Level Editor Application
- [ ] Skeletal Animations
- [ ] Component-Based Game Object 
- [ ] [ODE physics](https://www.ode.org/)
- [ ] Networking
- [ ] Scripting System ([Lua](https://www.lua.org/))
- [ ] Entity AI/Behavior System
- [ ] What else comes into my very flat brain (burgers)

***

# How to compile
Using a Gradle setup, import the dependencies and run the main class `Cinnamon.java`

Use `-h` to print into the command line arguments when the engine runs 

***

# To use as a library
Through [jitpack](https://jitpack.io/#meiiraru/Cinnamon),

On the Gradle build file, add:
```kt
repositories {
    maven("https://jitpack.io")
}
```
and
```kt
dependencies {
    //replace "cinnamonVersion" with the desired version, check jitpack for latest
    implementation("com.github.meiiraru", "Cinnamon", cinnamonVersion)
}
```
**dont forgor to include the LWJGL modules (and natives) you're going to use!**
