package tecgraf.openbus.demo.matrices;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import tecgraf.openbus.demo.matrices.Matrix;

class MatrixServant extends Matrix implements SquareMatrixOperations {}$\exlabel{servant}$

public class Server {
	public static void main(String[] args) throws Exception {
		ORB orb = ORB.init(args, null);$\exlabel{initorb}$
		try {
			POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));$\exlabel{getpoa}$
			poa.the_POAManager().activate();$\exlabel{activatepoa}$

			org.omg.CORBA.Object obj = poa.servant_to_reference($\exlabel{regobj}$
				new SquareMatrixPOATie(new MatrixServant()));$\exlabel{poatie}$

			System.out.println(orb.object_to_string(obj));$\exlabel{printior}$

			System.in.read();$\exlabel{waitend}$
		}
		finally { orb.shutdown(true); }$\exlabel{shutorb}$
	}
}
