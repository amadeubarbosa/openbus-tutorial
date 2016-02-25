package tecgraf.openbus.demo.matrices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.omg.CORBA.ORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.OpenBusContext;

public class Application {
	private static final String entity = "MatricesUser";
	private static final String password = "MatricesPassword";
	private static final String busHost = "localhost";
	private static final short busPort = 20100;

	public static void main(String[] args) throws Exception {
		ORB orb = ORBInitializer.initORB(args);$\exlabel{initorb}$
		try {
			OpenBusContext context = (OpenBusContext)
				orb.resolve_initial_references("OpenBusContext");$\exlabel{getcontext}$

			Connection conn = context.createConnection(busHost, busPort);$\exlabel{createconn}$
			context.setDefaultConnection(conn);$\exlabel{setdefconn}$

			conn.loginByPassword(entity, password.getBytes());$\exlabel{loginconn}$
			try {
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
			finally { conn.logout(); }$\exlabel{logoutconn}$
		}
		finally { orb.shutdown(true); }
	}
}
