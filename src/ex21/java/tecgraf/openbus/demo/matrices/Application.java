package tecgraf.openbus.demo.matrices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.omg.CORBA.ORB;
import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.assistant.AssistantParams;
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
		AssistantParams params = new AssistantParams();
		params.callback = new AssistantFailurePrinter(System.err);
		Assistant assistant = Assistant.createWithPassword(busHost, busPort,
		                                                   entity, password.getBytes(),
		                                                   params);

		ORB orb = assistant.orb();
		try {
			OpenBusContext context = (OpenBusContext)
				orb.resolve_initial_references("OpenBusContext");

			ServiceOfferDesc[] offers = assistant.findServices(new ServiceProperty[] {
				new ServiceProperty("domain", "Tutorial"),
				new ServiceProperty("openbus.offer.entity", factoryEntity),
				new ServiceProperty("openbus.component.interface",
				                    MatrixFactoryHelper.id())
			}, -1);

			MatrixFactory factory = null;
			for (ServiceOfferDesc offer : offers)
				try {
					factory = MatrixFactoryHelper.narrow(
						offer.service_ref.getFacet(MatrixFactoryHelper.id()));
					if (factory != null) break;
					System.err.println("found service with missing facet!");
				}
				catch (Exception e) { System.err.println("offered service failure: "+e); }
			if (factory == null) {
				System.err.println("no suitable service found");
				return;
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
		finally {
			assistant.shutdown();
			orb.shutdown(true);
		}
	}
}
