package tecgraf.openbus.demo.matrices;

public class Application {
	public static void main(String[] args) {
		Matrix matrix = new Matrix();$\exlabel{matrix}$

		double vector[] = new double[matrix.cardinality()];$\exlabel{makevector}$
		for (int i=0; i<vector.length; ++i) vector[i] = i+1;

		double result[] = matrix.multiply(vector);$\exlabel{multiply}$

		for (int i=0; i<result.length; ++i)$\exlabel{print}$
			System.out.print(result[i] + " ");
		System.out.println();
	}
}
