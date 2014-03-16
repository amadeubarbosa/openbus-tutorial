package tecgraf.openbus.demo.watchoffers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistryObserverHelper;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistryObserverPOA;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.OpenBusContext;

public class Application {
	private static final String entity = "guest";
	private static final String password = "guest";
	private static final String busHost = "localhost";
	private static final short busPort = 20100;

	public static void printOffer(PrintStream out,
	                              String tag,
	                              ServiceOfferDesc offer) {
		String id = "??";
		String login = "??";
		String entity = "??";
		String timestamp = "??";
		String year = "??";
		String month = "??";
		String day = "??";
		String hour = "??";
		String minute = "??";
		String second = "??";
		String name = "??";
		String major = "??";
		String minor = "??";
		String patch = "??";
		String platform = "??";
		for (ServiceProperty prop : offer.properties) {
			if (prop.name.equals("openbus.offer.id"))
				id = prop.value;
			else if (prop.name.equals("openbus.offer.login"))
				login = prop.value;
			else if (prop.name.equals("openbus.offer.entity"))
				entity = prop.value;
			else if (prop.name.equals("openbus.offer.timestamp"))
				timestamp = prop.value;
			else if (prop.name.equals("openbus.offer.year"))
				year = prop.value;
			else if (prop.name.equals("openbus.offer.month"))
				month = prop.value;
			else if (prop.name.equals("openbus.offer.day"))
				day = prop.value;
			else if (prop.name.equals("openbus.offer.hour"))
				hour = prop.value;
			else if (prop.name.equals("openbus.offer.minute"))
				minute = prop.value;
			else if (prop.name.equals("openbus.offer.second"))
				second = prop.value;
			else if (prop.name.equals("openbus.component.name"))
				name = prop.value;
			else if (prop.name.equals("openbus.component.version.major"))
				major = prop.value;
			else if (prop.name.equals("openbus.component.version.minor"))
				minor = prop.value;
			else if (prop.name.equals("openbus.component.version.patch"))
				patch = prop.value;
			else if (prop.name.equals("openbus.component.platform"))
				platform = prop.value;
		}
		out.println(String.format("[%s] offer %s", tag, id));
		out.println(String.format("\tOwner       : %s (%s)", entity, login));
		out.println(String.format("\tRegistration: %s/%s/%s %s:%s:%s (%s)",
			year, month, day, hour, minute, second, timestamp));
		out.println(String.format("\tComponent   : %s v%s.%s.%s (%s)",
			name, major, minor, patch, platform));
		out.println("\tFacets      :");
		for (ServiceProperty prop : offer.properties)
			if (prop.name.equals("openbus.component.facet"))
				out.println("\t\t"+prop.value);
		out.println("\tInterfaces  :");
		for (ServiceProperty prop : offer.properties)
			if (prop.name.equals("openbus.component.interface"))
				out.println("\t\t"+prop.value);
	}

	public static void main(String[] args) throws Exception {
		ORB orb = ORBInitializer.initORB(args);
		try {
			OpenBusContext context = (OpenBusContext)
				orb.resolve_initial_references("OpenBusContext");

			Connection conn = context.createConnection(busHost, busPort);
			context.setDefaultConnection(conn);

			conn.loginByPassword(entity, password.getBytes());
			try {
				POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
				poa.the_POAManager().activate();

				OfferRegistry offers = context.getOfferRegistry();

				ServiceProperty[] props = new ServiceProperty[] {
					new ServiceProperty("domain", "Tutorial") };

				OfferRegistryObserverPOA regObs = new OfferRegistryObserverPOA() {
					public void offerRegistered(ServiceOfferDesc offer) {
						printOffer(System.out, "ADDED", offer);
					}
				};
				offers.subscribeObserver(
					OfferRegistryObserverHelper.narrow(poa.servant_to_reference(regObs)), props);

				for (ServiceOfferDesc offer : offers.findServices(props))
					regObs.offerRegistered(offer);

				System.in.read();
			}
			finally { conn.logout(); }
		}
		finally { orb.shutdown(true); }
	}
}
