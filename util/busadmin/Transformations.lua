local category = "TutorialEntities"
local entity = "Transformations"
local certificate = "Transformations.crt"
local interface = "IDL:tecgraf/openbus/demo/transformations/TransformationRepository:1.0"

Category {
  id = category,
  name = "OpenBus 2.0 Tutorial Entities",
}

Interface { id = interface }
Entity {
  id = entity,
  category = category,
  name = "Transformations Data Provider",
}
Certificate {
  id = entity,
  certificate = certificate,
}
Grant {
  id = entity,
  interfaces = { interface },
}
