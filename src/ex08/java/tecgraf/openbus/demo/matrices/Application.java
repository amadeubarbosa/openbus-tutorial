package tecgraf.openbus.demo.matrices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.xml.bind.DatatypeConverter;
import org.omg.CORBA.ORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;

public class Application {
	private static final String entity = "MatricesUser";
	private static final String password = "MatricesPassword";
	private static final String busHost = "localhost";
	private static final short busPort = 20100;

	public static void main(String[] args) throws Exception {
		ORB orb = ORBInitializer.initORB(args);
		try {
			OpenBusContext context = (OpenBusContext)
				orb.resolve_initial_references("OpenBusContext");

			Connection conn = context.createConnection(busHost, busPort);
			context.setDefaultConnection(conn);

			conn.loginByPassword(entity, password.getBytes());
			try {
				SharedAuthSecret secret = conn.startSharedAuth();

				byte[] encoded = context.encodeSharedAuth(secret);
				System.out.println(DatatypeConverter.printBase64Binary(encoded));

				MatrixFactory factory = MatrixFactoryHelper.narrow(orb.string_to_object(
					new BufferedReader(new InputStreamReader(System.in)).readLine()));

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
