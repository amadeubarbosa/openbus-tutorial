local string = require "string"
local format = string.format
local byteOf = string.byte
local replace = string.gsub

local hashOf = require("lce.hash").md5

local function toHexa(char)
	return format("%.2x", byteOf(char))
end

return function (configs)
	local file = configs.passwords or "passwords.lua"
	local pswHash = {}
	assert(loadfile(file, "t", pswHash))()
	return function (entity, password)
		local hash = replace(hashOf(password), ".", toHexa)
		return hash == pswHash[entity]
	end
end
