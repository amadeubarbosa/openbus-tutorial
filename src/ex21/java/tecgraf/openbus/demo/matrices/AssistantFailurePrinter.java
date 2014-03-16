package tecgraf.openbus.demo.matrices;

import java.io.PrintStream;
import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IComponent;
import scs.core.IMetaInterface;
import scs.core.IMetaInterfaceHelper;
import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.assistant.OnFailureCallback;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_0.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;

public class AssistantFailurePrinter implements OnFailureCallback {
	private PrintStream _out;

	public AssistantFailurePrinter(PrintStream out) { _out = out; }

	private String getErrorMessage(Throwable except) {
		StringBuilder errmsg = new StringBuilder();
		try { throw except; }
		catch (InvalidService e) {
			errmsg.append("invalid service (");
			errmsg.append(e.message);
			errmsg.append(")");
		}
		catch (UnauthorizedFacets e) {
			errmsg.append("unauthorized facets provided (");
			String[] facets = e.facets;
			for (int i = 0; i < facets.length; ) {
				errmsg.append(facets[i]);
				errmsg.append(++i == facets.length ? ")" : ", ");
			}
		}
		catch (InvalidProperties e) {
			errmsg.append("invalid service properties (");
			ServiceProperty[] invprops = e.properties;
			for (int i = 0; i < invprops.length; ) {
				errmsg.append(invprops[i].name);
				errmsg.append(++i == invprops.length ? ")" : ", ");
			}
		}
		catch (ServiceFailure e) {
			errmsg.append("bus core service failure (");
			errmsg.append(e.message);
			errmsg.append(")");
		}
		catch (Throwable e) { errmsg.append(e); }
		return errmsg.toString();
	}

	public void onLoginFailure(Assistant assistant, Throwable except) {
		_out.println("[ASSISTANT] Login Failure: "+getErrorMessage(except));
	}

	public void onRegisterFailure(Assistant assistant,
	                       scs.core.IComponent component,
	                       ServiceProperty[] properties,
	                       Throwable except) {
		_out.println("[ASSISTANT] Offer Failure: "+getErrorMessage(except));
		_out.println("              Properties:");
		for (ServiceProperty prop : properties)
			_out.println("                "+prop.name+": "+prop.value);
		/* TODO: uncomment this after JacORB bug on local calls is fixed.
		_out.println("              Component ID:");
		ComponentId compId = component.getComponentId();
		_out.println("                comp. name: "+compId.name);
		_out.println("                major ver.: "+compId.major_version);
		_out.println("                minor ver.: "+compId.minor_version);
		_out.println("                patch ver.: "+compId.patch_version);
		_out.println("                platform  : "+compId.platform_spec);
		_out.println("              Component Facets:");
		IMetaInterface meta = IMetaInterfaceHelper.narrow(
			component.getFacet(IMetaInterfaceHelper.id()));
		for (FacetDescription facet : meta.getFacets())
			_out.println("                "+facet.name+": "+facet.interface_name);
		*/
	}

	public void onFindFailure(Assistant assistant, Throwable except) {
		_out.println("[ASSISTANT] Offer Query Failure: "+getErrorMessage(except));
	}

	public void onStartSharedAuthFailure(Assistant assistant, Throwable except) {
		_out.println("[ASSISTANT] Shared Auth. Failure: "+getErrorMessage(except));
	}
}
