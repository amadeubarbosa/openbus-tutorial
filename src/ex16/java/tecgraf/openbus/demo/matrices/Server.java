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
import scs.core.ComponentContext;
import scs.core.ComponentId;
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
	private OpenBusContext _context;
	private TransformationRepository _transformations = null;
	private ServiceProperty[] _transfServiceProps = new ServiceProperty[] {
		new ServiceProperty("domain", "Tutorial"),
		new ServiceProperty("openbus.offer.entity", "Transformations"),
		new ServiceProperty("openbus.component.interface",
		                    TransformationRepositoryHelper.id())
	};

	public MatrixFactoryServant(OpenBusContext context) { _context = context; }

	private double[] getTransformation(String kind)
		throws UnknownTransformation, GeneralFailure {
		GeneralFailure error;

		if (_transformations != null) {
			try { return _transformations.getTransformation(kind); }
			catch (org.omg.CORBA.SystemException e) {
				error = new GeneralFailure("transformations service failure: "+e);
			}
		} else {
			error = new GeneralFailure("unable to contact transformations service");
		}

		_transformations = null;
		try {
			ServiceOfferDesc[] offers =
				_context.getOfferRegistry().findServices(_transfServiceProps);
			for (ServiceOfferDesc offer : offers)
				try {
					_transformations = TransformationRepositoryHelper.narrow(
						offer.service_ref.getFacet(TransformationRepositoryHelper.id()));
					if (_transformations != null) break;
					System.err.println("found service with missing facet!");
				}
				catch (Exception e) { System.err.println("offered service failure: "+e); }
			if (_transformations != null)
				return _transformations.getTransformation(kind);
		}
		catch (Exception e) { System.err.println("offer query failure: "+e); }

		throw error;
	}

	public SquareMatrix newMatrix(String kind)
		throws UnknownMatrixKind, GeneralFailure {
		CallerChain chain = _context.getCallerChain();
		String entity = chain.caller().entity;
		if (!entity.equals("MatricesUser"))
			throw new NO_PERMISSION("unauthorized caller ("+entity+")");

		_context.joinChain();
		double[] transformation;
		try { transformation = getTransformation(kind); }
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

				ComponentContext component =
					new ComponentContext(orb, poa, new ComponentId("Matrices",$\exlabel{newscs}$
					                                               (byte)1,
					                                               (byte)0,
					                                               (byte)0,
					                                               "java"));
				component.addFacet("Matrices", MatrixFactoryHelper.id(),$\exlabel{addfacet}$
					new MatrixFactoryServant(context));

				context.getOfferRegistry().registerService(component.getIComponent(),$\exlabel{addoffer}$
					new ServiceProperty[] { new ServiceProperty("domain", "Tutorial") });

				System.in.read();
			}
			finally { conn.logout(); }
		}
		finally { orb.shutdown(true); }
	}
}
