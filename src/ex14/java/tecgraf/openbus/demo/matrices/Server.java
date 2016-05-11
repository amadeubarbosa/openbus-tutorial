package tecgraf.openbus.demo.matrices;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.matrices.Matrix;
import tecgraf.openbus.demo.transformations.TransformationRepository;
import tecgraf.openbus.demo.transformations.TransformationRepositoryHelper;
import tecgraf.openbus.demo.transformations.UnknownTransformation;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.security.Cryptography;

class MatrixServant extends SquareMatrixPOA {
	private Matrix _matrix;
	private OpenBusContext _context;

	public MatrixServant(Matrix matrix, OpenBusContext context) {
		_matrix = matrix;
		_context = context;
	}

	private void assertAuthorized() {
		String entity = _context.getCallerChain().caller().entity;
		if (!entity.equals("MatricesUser"))
			throw new NO_PERMISSION("unauthorized caller ("+entity+")");
	}

	public int cardinality() { return _matrix.cardinality(); }

	public double[] multiply(double[] v) throws WrongCardinality {
		assertAuthorized();
		if (v.length != cardinality())
			throw new WrongCardinality(v.length, cardinality());
		return _matrix.multiply(v);
	}

	public void dispose() {
		assertAuthorized();
		try { _poa().deactivate_object(_object_id()); }
		catch (ObjectNotActive e) {}
		catch (WrongPolicy e) {}
	}
}

class MatrixFactoryServant extends MatrixFactoryPOA {
	private TransformationRepository _transformations;
	private OpenBusContext _context;

	public MatrixFactoryServant(TransformationRepository transformations,
	                            OpenBusContext context) {
		_transformations = transformations;
		_context = context;
	}

	public SquareMatrix newMatrix(String kind)
		throws UnknownMatrixKind, GeneralFailure {
		CallerChain chain = _context.getCallerChain();
		String entity = chain.caller().entity;
		if (!entity.equals("MatricesUser"))
			throw new NO_PERMISSION("unauthorized caller ("+entity+")");

		_context.joinChain();
		double[] transformation;
		try { transformation = _transformations.getTransformation(kind); }
		catch(UnknownTransformation e) { throw new UnknownMatrixKind(kind); }

		MatrixServant matrix = new MatrixServant(new Matrix(transformation), _context);

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
	private static final String entity = "MatricesService";
	private static final String privateKeyFile = "Matrices.key";
	private static final String busHost = "localhost";
	private static final short busPort = 20100;

	public static void main(String[] args) throws Exception {
		RSAPrivateKey privateKey =
			Cryptography.getInstance().readKeyFromFile(privateKeyFile);

		ORB orb = ORBInitializer.initORB(args, null);
		try {
			OpenBusContext context = (OpenBusContext)
				orb.resolve_initial_references("OpenBusContext");

			Connection conn = context.createConnection(busHost, busPort);
			context.setDefaultConnection(conn);

			conn.loginByCertificate(entity, privateKey);
			try {
				POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
				poa.the_POAManager().activate();

				ServiceOfferDesc[] offers = context.getOfferRegistry().findServices($\exlabel{findserv}$
					new ServiceProperty[] {
						new ServiceProperty("domain", "Tutorial"),
						new ServiceProperty("openbus.offer.entity", "Transformations"),
						new ServiceProperty("openbus.component.interface",
						                    TransformationRepositoryHelper.id())
					});

				TransformationRepository transformations = null;
				for (ServiceOfferDesc offer : offers)
					try {
						transformations = TransformationRepositoryHelper.narrow(
							offer.service_ref.getFacet(TransformationRepositoryHelper.id()));$\exlabel{getfacet}$
						if (transformations != null) break;$\exlabel{servfound}$
						System.err.println("found service with missing facet!");
					}
					catch (Exception e) { System.err.println("offered service failure: "+e); }$\exlabel{catchex}$
				if (transformations == null) {$\exlabel{checkserv}$
					System.err.println("no suitable service found");
					return;$\exlabel{endexec}$
				}

				org.omg.CORBA.Object obj = poa.servant_to_reference(
					new MatrixFactoryServant(transformations, context));

				System.out.println(orb.object_to_string(obj));

				System.in.read();
			}
			finally { conn.logout(); }
		}
		finally { orb.shutdown(true); }
	}
}
