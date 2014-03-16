package tecgraf.openbus.demo.matrices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.omg.CORBA.ORB;
import tecgraf.openbus.core.ORBInitializer;

public class Application {
	public static void main(String[] args) throws Exception {
		ORB orb = ORBInitializer.initORB(args);
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
		finally { orb.shutdown(true); }
	}
}
