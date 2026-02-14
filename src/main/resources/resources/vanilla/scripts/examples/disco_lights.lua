-- disco_lights.lua - Dynamic colored lights that animate over time.

print("=== Disco Lights ===")
print("Setting up animated light show...")

-- Place a dance floor
terrain.fill(-5, 0, -5, 5, 0, 5, "PLASTIC")

-- Add corner pillars
for _, pos in ipairs({{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) do
    for y = 1, 4 do
        terrain.place("BOX", pos[1], y, pos[2], "WHITE_MARBLE")
    end
end

-- Disco ball (gold block at center ceiling)
terrain.place("BOX", 0, 6, 0, "GOLD")

-- Static ambient lights on pillars
local pillarColors = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00}
local pillarPositions = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}
for i, pos in ipairs(pillarPositions) do
    lights.addPoint({
        x = pos[1], y = 4, z = pos[2],
        color = pillarColors[i],
        intensity = 1.5
    })
end

-- Central spotlight pointing down
lights.addSpot({
    x = 0, y = 6, z = 0,
    dx = 0, dy = -1, dz = 0,
    color = 0xFFFFFF,
    intensity = 3.0,
    angle = 45
})

-- Animated color cycling via tick events
local tick = 0
local colors = {0xFF0000, 0xFF8800, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0x8800FF, 0xFF00FF}

events.every(20, function()
    tick = tick + 1

    -- Cycle particle effects around the dance floor
    local angle = (tick * 0.5) % (2 * math.pi)
    local px = math.cos(angle) * 4
    local pz = math.sin(angle) * 4
    local colorIdx = (tick % #colors) + 1

    particles.spawn("CONFETTI", px, 2, pz, {count=5})
    particles.spawn("STAR", -px, 3, -pz, {count=3})

    -- Firework burst every 8 cycles
    if tick % 8 == 0 then
        particles.spawn("EXPLOSION", 0, 4, 0, {count=10})
        print("Disco boom! (tick " .. tick .. ")")
    end
end)

-- HUD display
hud.setText("disco_title", 10, 30, "DISCO MODE", 0xFF00FF)

print("Disco floor is ready! Dance away!")
print("Particles will animate continuously.")
