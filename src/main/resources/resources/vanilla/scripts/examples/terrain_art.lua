-- terrain_art.lua - Create procedural terrain art using Lua math.

print("=== Terrain Art ===")
print("Generating terrain patterns...")

-- Spiral staircase
local radius = 6
local height = 12
local steps = 48
for i = 0, steps do
    local angle = (i / steps) * math.pi * 4  -- 2 full rotations
    local x = math.floor(math.cos(angle) * radius)
    local z = math.floor(math.sin(angle) * radius)
    local y = math.floor((i / steps) * height) + 1

    -- Alternate materials for visual interest
    local materials = {"COBBLESTONE", "BRICK_WALL", "BROWN_WOOD_PLANKS", "OAK_LOG"}
    local mat = materials[(i % #materials) + 1]
    terrain.place("BOX", x, y, z, mat)
end

-- Central pillar
for y = 1, height + 1 do
    terrain.place("BOX", 0, y, 0, "GOLD")
end

-- Crown at the top
local crownY = height + 2
for dx = -1, 1 do
    for dz = -1, 1 do
        terrain.place("BOX", dx, crownY, dz, "GOLD")
    end
end

-- Light the staircase
for y = 2, height, 3 do
    lights.addPoint({x=0, y=y, z=0, color=0xFFAA44, intensity=1.0})
end

-- Beacon light at top
lights.addPoint({x=0, y=crownY + 2, z=0, color=0xFFFFAA, intensity=3.0})

-- Menger sponge showcase
terrain.mengerSponge(2, 12, 1, 0, "COBBLESTONE")

-- Light the sponge
lights.addPoint({x=12, y=6, z=0, color=0x88CCFF, intensity=2.5})

-- Pyramid
local pyramidX = -12
local pyramidSize = 5
for y = 0, pyramidSize do
    local s = pyramidSize - y
    terrain.fill(
        pyramidX - s, y + 1, -s,
        pyramidX + s, y + 1, s,
        "SAND"
    )
end
lights.addPoint({x=pyramidX, y=pyramidSize + 3, z=0, color=0xFFDD44, intensity=2.0})

print("Terrain art complete!")
print("- Spiral staircase at origin")
print("- Menger sponge at x=12")
print("- Pyramid at x=-12")
