local tuple = require "tuple"

local function pack(matched, ...)
	if matched then
		return tuple.create(matched, ...)
	end
end

return function (configs)
	local ePat = configs.enititypattern or "^(.-)User$"
	local pPat = configs.passwordpattern or "^(.-)Password$"
	return function (entity, password)
		local captures = pack(entity:match(ePat))
		return captures and captures == pack(password:match(pPat))
	end
end
