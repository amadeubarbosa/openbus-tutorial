local lfs = require "lfs"
local os = require "os"

local category = "TutorialEntities"
local certpath = os.getenv("OPENBUS_TUTORIAL_CERTIFICATES") or "certificates"
local interface = "IDL:tecgraf/openbus/demo/matrices/MatrixFactory:1.0"

Category {
  id = category,
  name = "OpenBus 2.0 Tutorial Entities",
}
Interface { id = interface }
for name in lfs.dir(certpath) do
  if name:find(".crt", #name-3, true) then
    local entity = name:sub(1, -5).."Service"
    Entity {
      id = entity,
      category = category,
      name = "Tutorial System of "..entity,
    }
    Certificate {
      id = entity,
      certificate = certpath.."/"..name,
    }
    Grant {
      id = entity,
      interfaces = { interface },
    }
  end
end
