-- demo.lua - A comprehensive demonstration of the Cinnamon Lua API.

print("=== Running Demo Script ===")
print("")

-- Build a small structure
print("Building a structure...")
terrain.fill(-3, 0, -3, 3, 0, 3, "GRANITE")     -- floor
terrain.fill(-3, 1, -3, -3, 3, 3, "PLASTIC_BRICKS")    -- left wall
terrain.fill(3, 1, -3, 3, 3, 3, "PLASTIC_BRICKS")      -- right wall
terrain.fill(-3, 1, -3, 3, 3, -3, "PLASTIC_BRICKS")    -- back wall
terrain.fill(-2, 1, 3, 2, 3, 3, "PLASTIC_BRICKS")      -- front wall (with gap)
terrain.fill(-3, 4, -3, 3, 4, 3, "OAK_LOG")   -- roof

-- Place a door gap
terrain.remove(-1, 1, 3)
terrain.remove(0, 1, 3)
terrain.remove(1, 1, 3)
terrain.remove(-1, 2, 3)
terrain.remove(0, 2, 3)
terrain.remove(1, 2, 3)

-- Add interior lighting
lights.addPoint({x=0, y=3, z=0, color=0xFFDD88, intensity=2.0})

-- Spawn a training dummy inside
entity.spawn("DUMMY", 0, 1, -1)

-- Add some exterior decoration
terrain.place("BOX", -4, 1, -4, "GOLD")
terrain.place("BOX", 4, 1, -4, "GOLD")
terrain.place("BOX", -4, 1, 4, "GOLD")
terrain.place("BOX", 4, 1, 4, "GOLD")

-- Colored corner lights
lights.addPoint({x=-4, y=3, z=-4, color=0xFF0000, intensity=1.5})
lights.addPoint({x=4, y=3, z=-4, color=0x00FF00, intensity=1.5})
lights.addPoint({x=-4, y=3, z=4, color=0x0000FF, intensity=1.5})
lights.addPoint({x=4, y=3, z=4, color=0xFF00FF, intensity=1.5})

-- Spawn particles at corners
particles.spawn("FIRE", -4, 2, -4, {count=5})
particles.spawn("FIRE", 4, 2, -4, {count=5})
particles.spawn("FIRE", -4, 2, 4, {count=5})
particles.spawn("FIRE", 4, 2, 4, {count=5})

-- HUD elements
hud.setText("demo_title", 10, 30, "Demo World", 0xFFFFDD88)
hud.setProgressBar("demo_bar", 10, 45, 120, 6, 1.0, 0xFF00FF88)

-- Delayed effects
events.after(40, function()
    print("Spawning confetti celebration!")
    particles.spawn("CONFETTI", 0, 5, 0, {count=20})
    particles.spawnText("Welcome!", 0, 6, 0, 80)
end)

-- Repeating particle effect on the gold pillars
events.every(60, function()
    particles.spawn("STAR", -4, 2, -4, {count=2})
    particles.spawn("STAR", 4, 2, -4, {count=2})
    particles.spawn("STAR", -4, 2, 4, {count=2})
    particles.spawn("STAR", 4, 2, 4, {count=2})
end)

print("Demo structure built!")
print("Walk up to the building and explore inside.")
