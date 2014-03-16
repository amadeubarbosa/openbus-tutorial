package tecgraf.openbus.demo.transformations;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

public interface ReloginAllocator {
	public String reallocate(LoginInfo login) throws Exception;
}

