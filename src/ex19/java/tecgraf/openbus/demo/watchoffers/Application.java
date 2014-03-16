package tecgraf.openbus.demo.watchoffers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferObserver;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferObserverHelper;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferObserverPOA;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferObserverSubscriptionOperations;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferObserverSubscriptionDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistryObserverHelper;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistryObserverPOA;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.UnauthorizedOperation;
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

				final Map<String,OfferObserverSubscriptionOperations> offSubs =
					new HashMap <String,OfferObserverSubscriptionOperations>();

				final OfferObserver offObs = OfferObserverHelper.narrow(
					poa.servant_to_reference(new OfferObserverPOA () {
						public void propertiesChanged(ServiceOfferDesc offer) {
							for (ServiceProperty prop : offer.properties)
								if (prop.name == "domain" && prop.value == "Tutorial") return;
							removed(offer);
						}
						public void removed(ServiceOfferDesc offer) {
							for (ServiceProperty prop : offer.properties)
								if (prop.name.equals("openbus.offer.id")) {
									OfferObserverSubscriptionOperations subscription;
									synchronized (offSubs) { subscription = offSubs.remove(prop.value); }
									if (subscription != null) {
										try { subscription.remove(); }
										catch (UnauthorizedOperation e) {}
										catch (ServiceFailure e) {}
										catch (org.omg.CORBA.SystemException e) {}
										printOffer(System.out, "REMOVED", offer);
									}
								}
							}
						}));

				OfferRegistry offers = context.getOfferRegistry();

				ServiceProperty[] props = new ServiceProperty[] {
					new ServiceProperty("domain", "Tutorial") };

				OfferRegistryObserverPOA regObs = new OfferRegistryObserverPOA() {
					private OfferObserverSubscriptionOperations fakeSub =
						new OfferObserverSubscriptionOperations() {
							public OfferObserver observer() { return null; }
							public ServiceOffer offer() { return null; }
							public OfferObserverSubscriptionDesc describe() { return null; }
							public void remove() throws ServiceFailure, UnauthorizedOperation {}
						};

					public void offerRegistered(ServiceOfferDesc offer) {
						for (ServiceProperty prop : offer.properties)
							if (prop.name.equals("openbus.offer.id")) {
								synchronized (offSubs) {
									if (offSubs.get(prop.value) != null) break;
									offSubs.put(prop.value, fakeSub);
								}
								printOffer(System.out, "ADDED", offer);

								for (int i=1; i<Integer.MAX_VALUE; ++i)
									try {
										OfferObserverSubscriptionOperations subscription =
											offer.ref.subscribeObserver(offObs);
										synchronized (offSubs) { offSubs.put(prop.value, subscription); }
										break;
									}
									catch (ServiceFailure e) {
										System.err.println("offer watch failure: "+e);
										try { Thread.sleep(i*1000); } catch (InterruptedException ie) {}
									}
									catch (OBJECT_NOT_EXIST e) {
										offObs.removed(offer);
										break;
									}
									catch (org.omg.CORBA.SystemException e) {
										System.err.println("offer watch failure: "+e);
										if (!e.completed.equals(CompletionStatus.COMPLETED_YES))
											offObs.removed(offer);
										break;
									}
								break;
							}
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
