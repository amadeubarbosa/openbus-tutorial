package tecgraf.openbus.demo.matrices;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.demo.matrices.Matrix;

class MatrixServant extends SquareMatrixPOA {
	private Matrix _matrix;

	public MatrixServant(Matrix matrix) { _matrix = matrix; }

	public int cardinality() { return _matrix.cardinality(); }

	public double[] multiply(double[] v) throws WrongCardinality {
		if (v.length != cardinality())
			throw new WrongCardinality(v.length, cardinality());
		return _matrix.multiply(v);
	}

	public void dispose() {
		try { _poa().deactivate_object(_object_id()); }
		catch (ObjectNotActive e) {}
		catch (WrongPolicy e) {}
	}
}

class MatrixFactoryServant extends MatrixFactoryPOA {
	public SquareMatrix newMatrix(String kind)
		throws UnknownMatrixKind, GeneralFailure {
		if (!kind.equals("reverse")) throw new UnknownMatrixKind(kind);

		MatrixServant matrix = new MatrixServant(new Matrix());

		try {
			return SquareMatrixHelper.narrow(_poa().servant_to_reference(matrix));
		}
		catch (ServantNotActive e) {
			throw new GeneralFailure("failure while registering matrix servant");
		}
		catch (WrongPolicy e) {
			throw new GeneralFailure("unexpected failure (wrong POA policy)");
		}
	}
}

public class Server {
	public static void main(String[] args) throws Exception {
		ORB orb = ORBInitializer.initORB(args);
		try {
			POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			poa.the_POAManager().activate();

			org.omg.CORBA.Object obj = poa.servant_to_reference(
				new MatrixFactoryServant());

			System.out.println(orb.object_to_string(obj));

			System.in.read();
		}
		finally { orb.shutdown(true); }
	}
}
