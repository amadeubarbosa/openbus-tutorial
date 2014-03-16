package tecgraf.openbus.demo.matrices;

public class Application {
	public static void main(String[] args) {
		Matrix matrix = new Matrix();

		double vector[] = new double[matrix.cardinality()];
		for (int i=0; i<vector.length; ++i) vector[i] = i+1;

		double result[] = matrix.multiply(vector);

		for (int i=0; i<result.length; ++i)
			System.out.print(result[i] + " ");
		System.out.println();
	}
}
