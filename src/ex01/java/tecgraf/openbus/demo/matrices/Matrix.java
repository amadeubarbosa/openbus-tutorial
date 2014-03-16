package tecgraf.openbus.demo.matrices;

public class Matrix {
	private int _cardinality;
	private double _data[][];

	public Matrix(double[] data) {
		_cardinality = (int)java.lang.Math.floor(java.lang.Math.sqrt(data.length));
		_data = new double[_cardinality][_cardinality];
		for (int i=0; i<data.length; ++i) {
			int row = i%_cardinality;
			int col = i/_cardinality;
			_data[row][col] = data[i];
		}
	}

	public Matrix() {
		this(new double[] {
			0,0,0,0,1,
			0,0,0,1,0,
			0,0,1,0,0,
			0,1,0,0,0,
			1,0,0,0,0
		});
	}

	public int cardinality() { return _cardinality; }

	public double[] multiply(double[] v) {
		double r[] = new double[_cardinality];
		for (int j = 0; j < _cardinality; ++j) {
			r[j] = 0;
			for (int k = 0; k < _cardinality; ++k)
				r[j] += v[k] * _data[k][j];
		}
		return r;
	}
}
