package tecgraf.openbus.demo.transformations;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserver;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserverHelper;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserverPOA;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserverSubscription;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;

class TransformationRepository_Impl extends TransformationRepositoryPOA
{
	private String _callerSuffix;
	private String _originatorSuffix;
	private Map<String, double[]> _transformations;

	private void assertAuthorized() {$\exlabel{auxfunc}$
		try {
			OpenBusContext context = (OpenBusContext)
				_orb().resolve_initial_references("OpenBusContext");
			CallerChain chain = context.getCallerChain();$\exlabel{getchain}$
			String caller = chain.caller().entity;$\exlabel{getcaller}$
			LoginInfo[]
				originators = chain.originators();$\exlabel{listorigins}$
			if (caller.endsWith(_callerSuffix)) {$\exlabel{checkcaller}$
				if (originators.length == 1) {$\exlabel{countorigins}$
					String originator = originators[0].entity;$\exlabel{getorigin}$
					if (!originator.endsWith(_originatorSuffix))$\exlabel{checkorigin}$
						throw new NO_PERMISSION("unauthorized originator ("+originator+")");
				}
				else throw new NO_PERMISSION("no authorized originator");
			}
			else throw new NO_PERMISSION("unauthorized caller ("+caller+")");
		}
		catch (InvalidName e) {
			throw new NO_PERMISSION("unverified caller (no context)");
		}
	}

	public TransformationRepository_Impl(Map<String, double[]> transformations,
	                                     String callerSuffix,
	                                     String originatorSuffix) {
		_callerSuffix = callerSuffix;
		_originatorSuffix = originatorSuffix;
		_transformations = transformations;
	}
	public String[] listTransformations() {
		return _transformations.keySet().toArray(new String[0]);
	}
	public double[] getTransformation(String id)
		throws UnknownTransformation {
		assertAuthorized();$\exlabel{callaux}$
		double[] transformation = _transformations.get(id);
		if (transformation == null) throw new UnknownTransformation(id);
		return transformation;
	}
}

public class Server {
	public static void main(String[] args) throws Exception {
		String busHost          = args.length > 0 ?                  args[0]  : "localhost";
		short busPort           = args.length > 1 ? Short.parseShort(args[1]) : 20100;
		final String entity     = args.length > 2 ?                  args[2]  : "Transformations";
		String privateKeyFile   = args.length > 3 ?                  args[3]  : "Transformations.key";
		String callerSuffix     = args.length > 4 ?                  args[4]  : "Service";
		String originatorSuffix = args.length > 5 ?                  args[5]  : "User";
		String serverPort       = args.length > 6 ?                  args[6]  : "22222";

		System.out.println("Command-Line Parameters:");
		System.out.println("\t[bus core host]     (current: "+busHost+")");
		System.out.println("\t[bus core port]     (current: "+busPort+")");
		System.out.println("\t[entity name]       (current: "+entity+")");
		System.out.println("\t[private key file]  (current: "+privateKeyFile+")");
		System.out.println("\t[caller suffix]     (current: "+callerSuffix+")");
		System.out.println("\t[originator suffix] (current: "+originatorSuffix+")");
		System.out.println("\t[server port]       (current: "+serverPort+")");

		final OpenBusPrivateKey privateKey =
			OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);

		Map<String, double[]> transformations =
			new HashMap<String, double[]>();
		transformations.put("null", new double[] {
			0,0,0,0,0,
			0,0,0,0,0,
			0,0,0,0,0,
			0,0,0,0,0,
			0,0,0,0,0
		});
		transformations.put("identity", new double[] {
			1,0,0,0,0,
			0,1,0,0,0,
			0,0,1,0,0,
			0,0,0,1,0,
			0,0,0,0,1
		});
		transformations.put("reverse", new double[] {
			0,0,0,0,1,
			0,0,0,1,0,
			0,0,1,0,0,
			0,1,0,0,0,
			1,0,0,0,0
		});
		transformations.put("rshift", new double[] {
			0,1,0,0,0,
			0,0,1,0,0,
			0,0,0,1,0,
			0,0,0,0,1,
			1,0,0,0,0
		});
		transformations.put("lshift", new double[] {
			0,0,0,0,1,
			1,0,0,0,0,
			0,1,0,0,0,
			0,0,1,0,0,
			0,0,0,1,0,
		});

		Properties props = new Properties(); 
		props.setProperty("OAPort", serverPort); 

		ORB orb = ORBInitializer.initORB(args, props);
		try {
			POA poa = POAHelper.narrow(
				orb.resolve_initial_references("RootPOA"));
			poa.the_POAManager().activate();

			final OpenBusContext context = (OpenBusContext) 
				orb.resolve_initial_references("OpenBusContext");

			Connection conn = context.createConnection(busHost, busPort);
			context.setDefaultConnection(conn);

			final LoginObserver observer = LoginObserverHelper.narrow(
				poa.servant_to_reference(new LoginObserverPOA() {
					public void entityLogout(LoginInfo login) {
						try { context.getLoginRegistry().getLoginValidity(login.id); }
						catch (Exception e) {}
					}
				}));

			final ReloginTask obsSubscriber = new ReloginTask(conn,
				new ReloginAllocator() {
					public String reallocate(LoginInfo login)
						throws Exception {
						LoginRegistry logins = context.getLoginRegistry();
						LoginObserverSubscription subscription =
							logins.subscribeObserver(observer);
						try { subscription.watchLogin(login.id); }
						catch (OBJECT_NOT_EXIST e) {
							System.err.println("obs. was discarted");
						}
						return login.id;
					}
				});

			final ComponentContext component = new ComponentContext(orb, poa,
				new ComponentId("Transformations", (byte)1, (byte)0, (byte)0, "java"));
			component.addFacet("Transformations", TransformationRepositoryHelper.id(),
				new TransformationRepository_Impl(transformations,
				                                  callerSuffix,
				                                  originatorSuffix));

			final ReloginTask offerRegister = new ReloginTask(conn,
				new ReloginAllocator() {
					public String reallocate(LoginInfo login)
						throws Exception {
						OfferRegistry offers = context.getOfferRegistry();
						String loginId = null;
						do {
							ServiceOffer offer = offers.registerService(
								component.getIComponent(),
								new ServiceProperty[] {
									new ServiceProperty("domain", "Tutorial")
								});
							try {
								ServiceOfferDesc desc = offer.describe();
								for (ServiceProperty prop : desc.properties)
									if (prop.name.equals("openbus.offer.login"))
										loginId = prop.value;
							}
							catch (OBJECT_NOT_EXIST e) {
								System.err.println("offer was removed");
							}
						} while (loginId == null);
						return loginId;
					}
				});

			conn.onInvalidLoginCallback(new InvalidLoginCallback() {
				public void invalidLogin(Connection conn, LoginInfo login) {
					for (int i=1; i<Integer.MAX_VALUE; ++i) {
						try {
							conn.loginByCertificate(entity, privateKey);
							break;
						}
						catch (AlreadyLoggedIn e) { break; }
						catch (org.omg.CORBA.SystemException e) {
							System.err.println("wait "+i+" sec. after login failure: "+e);
							try { Thread.sleep(i*1000); }
							catch (InterruptedException ie) {}
						}
						catch (Exception e) {
							System.err.println("bus login failure: "+e);
							break;
						}
					}
					obsSubscriber.notifyRelogin();
					offerRegister.notifyRelogin();
				}
			});
			conn.onInvalidLoginCallback().invalidLogin(conn, null);
			try {
				String ior = orb.object_to_string(
					component.getFacetByName("Transformations").getReference());
				((org.jacorb.orb.ORB)orb).addObjectKey("Transformations", ior);

				System.in.read();
			}
			finally { conn.logout(); }
		}
		finally { orb.shutdown(true); }
	}
}
