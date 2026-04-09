local partybot = {}

function partybot.banner(name)
    local user = name or "Pilot"
    return "[partybot] Welcome to the disco, " .. user .. "!"
end

function partybot.random_move(seed)
    local moves = { "moonwalk", "robot", "shuffle", "spin" }
    local idx = (math.abs(seed or os.time()) % #moves) + 1
    return moves[idx]
end

return partybot
