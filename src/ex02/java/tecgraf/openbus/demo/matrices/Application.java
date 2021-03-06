package tecgraf.openbus.demo.matrices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.omg.CORBA.ORB;

public class Application {
	public static void main(String[] args) throws Exception {
		ORB orb = ORB.init(args, null);$\exlabel{initorb}$
		try {
			SquareMatrix matrix = SquareMatrixHelper.narrow(orb.string_to_object($\exlabel{createstub}$
				new BufferedReader(new InputStreamReader(System.in)).readLine()));$\exlabel{readior}$

			double vector[] = new double[matrix.cardinality()];
			for (int i=0; i<vector.length; ++i) vector[i] = i+1;

			double result[] = matrix.multiply(vector);

			for (int i=0; i<result.length; ++i)
				System.out.print(result[i] + " ");
			System.out.println();
		}
		finally { orb.shutdown(true); }$\exlabel{shutorb}$
	}
}
