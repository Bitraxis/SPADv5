local soundboard = {}

function soundboard.beep(times)
    local n = math.max(1, math.floor(times or 1))
    return string.rep("beep ", n)
end

function soundboard.drumroll(level)
    local l = math.max(1, math.floor(level or 1))
    return string.rep("dr", l) .. "um!"
end

return soundboard
