return function (configs)
	local eSuffix = configs.enititysuffix or "User"
	local pSuffix = configs.passwordsuffix or "Password"
	return function (entity, password)
		local idsize = #entity - #eSuffix
		return idsize > 0
		   and entity:find(eSuffix, idsize+1, true)
		   and password == entity:sub(1, idsize)..pSuffix
		return false 
	end
end
