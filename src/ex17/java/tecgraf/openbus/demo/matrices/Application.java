package tecgraf.openbus.demo.matrices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.omg.CORBA.ORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.OpenBusContext;

public class Application {
	private static final String entity = "MatricesUser";
	private static final String password = "MatricesPassword";
	private static final String busHost = "localhost";
	private static final short busPort = 20100;
	private static final String factoryEntity = "MatricesService";

	public static void main(String[] args) throws Exception {
		ORB orb = ORBInitializer.initORB(args);
		try {
			OpenBusContext context = (OpenBusContext)
				orb.resolve_initial_references("OpenBusContext");

			Connection conn = context.createConnection(busHost, busPort);
			context.setDefaultConnection(conn);

			conn.loginByPassword(entity, password.getBytes());
			try {
				ServiceOfferDesc[] offers = context.getOfferRegistry().findServices($\exlabel{findoffer}$
					new ServiceProperty[] {
						new ServiceProperty("domain", "Tutorial"),$\exlabel{domainprop}$
						new ServiceProperty("openbus.offer.entity", factoryEntity),$\exlabel{entityprop}$
						new ServiceProperty("openbus.component.interface",$\exlabel{ifaceprop}$
						                    MatrixFactoryHelper.id())
					});

				MatrixFactory factory = null;
				for (ServiceOfferDesc offer : offers)
					try {
						factory = MatrixFactoryHelper.narrow(
							offer.service_ref.getFacet(MatrixFactoryHelper.id()));$\exlabel{getfacet}$
						if (factory != null) break;$\exlabel{servfound}$
						System.err.println("found service with missing facet!");
					}
					catch (Exception e) { System.err.println("offered service failure: "+e); }$\exlabel{catchex}$
				if (factory == null) {
					System.err.println("no suitable service found");
					return;$\exlabel{endapp}$
				}

				SquareMatrix matrix = factory.newMatrix("reverse");
				try {
					double vector[] = new double[matrix.cardinality()];
					for (int i=0; i<vector.length; ++i) vector[i] = i+1;

					double result[] = matrix.multiply(vector);

					for (int i=0; i<result.length; ++i)
						System.out.print(result[i] + " ");
					System.out.println();
				}
				finally { matrix.dispose(); }
			}
			finally { conn.logout(); }
		}
		finally { orb.shutdown(true); }
	}
}
