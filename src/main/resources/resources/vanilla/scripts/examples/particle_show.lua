-- particle_show.lua - Showcase all particle types with labeled displays.

print("=== Particle Showcase ===")
print("Spawning all particle types...")

-- List of particle types to demonstrate
local particleTypes = {
    "HEART", "BROKEN_HEART", "BUBBLE", "CONFETTI",
    "DUST", "ELECTRO", "EXPLOSION", "FIRE",
    "LIGHT", "SMOKE", "SQUARE", "STAR",
    "STEAM", "VOXEL"
}

-- Arrange in a grid
local cols = 4
local spacing = 4

for i, pType in ipairs(particleTypes) do
    local col = (i - 1) % cols
    local row = math.floor((i - 1) / cols)
    local x = (col - cols / 2 + 0.5) * spacing
    local z = (row - 1) * spacing - 5

    -- Platform
    terrain.place("BOX", math.floor(x), 0, math.floor(z), "COBBLESTONE")

    -- Label
    particles.spawnText(pType, x, 3.5, z, 200)

    -- Spawn the particle type
    particles.spawn(pType, x, 2, z, {count=8})
end

-- Continuous re-spawning so particles stay visible
events.every(60, function()
    for i, pType in ipairs(particleTypes) do
        local col = (i - 1) % cols
        local row = math.floor((i - 1) / cols)
        local x = (col - cols / 2 + 0.5) * spacing
        local z = (row - 1) * spacing - 5

        particles.spawn(pType, x, 2, z, {count=5})
    end
end)

-- Text particle demo
particles.spawnText("Cinnamon Lua!", 0, 5, -10, 300)

-- Voxel particle rainbow
for i = 0, 7 do
    local angle = (i / 8) * math.pi * 2
    local x = math.cos(angle) * 3
    local z = math.sin(angle) * 3 + 8
    local r = math.floor(math.cos(angle) * 127 + 128)
    local g = math.floor(math.cos(angle + 2.094) * 127 + 128)
    local b = math.floor(math.cos(angle + 4.189) * 127 + 128)
    local color = r * 65536 + g * 256 + b

    particles.spawnVoxel(x, 2, z, color, 200, 0.3)
end

print("Particle showcase active!")
print("Particles will refresh every 3 seconds.")
