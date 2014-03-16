package tecgraf.openbus.demo.matrices;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import tecgraf.openbus.demo.matrices.Matrix;

class MatrixServant extends Matrix implements SquareMatrixOperations {}

public class Server {
	public static void main(String[] args) throws Exception {
		ORB orb = ORB.init(args, null);
		try {
			POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			poa.the_POAManager().activate();

			org.omg.CORBA.Object obj = poa.servant_to_reference(
				new SquareMatrixPOATie(new MatrixServant()));

			System.out.println(orb.object_to_string(obj));

			System.in.read();
		}
		finally { orb.shutdown(true); }
	}
}
