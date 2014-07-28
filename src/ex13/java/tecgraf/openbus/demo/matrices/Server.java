package tecgraf.openbus.demo.matrices;

import java.util.HashMap;
import java.util.Map;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserver;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserverHelper;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserverPOA;
import tecgraf.openbus.core.v2_0.services.access_control.LoginObserverSubscription;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.demo.matrices.Matrix;
import tecgraf.openbus.demo.transformations.TransformationRepository;
import tecgraf.openbus.demo.transformations.TransformationRepositoryHelper;
import tecgraf.openbus.demo.transformations.UnknownTransformation;
import tecgraf.openbus.OpenBusContext;

class MatrixServant extends SquareMatrixPOA {
	private Matrix _matrix;
	private OpenBusContext _context;
	private MatrixFactoryServant _factory;
	private String _loginId;

	public MatrixServant(Matrix matrix, OpenBusContext context,
	                     MatrixFactoryServant factory, String loginId) {
		_matrix = matrix;
		_context = context;
		_factory = factory;
		_loginId = loginId;
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
		_factory.disposeMatrix(this, _loginId);
	}
}

class MatrixFactoryServant extends MatrixFactoryPOA {
	private TransformationRepository _transformations;
	private OpenBusContext _context;
	private final Map<String,Map<MatrixServant,byte[]>> _matricesOf;
	private LoginObserverSubscription _subscription;

	public MatrixFactoryServant(TransformationRepository transformations,
	                            OpenBusContext context, POA poa)
		throws ServantNotActive , WrongPolicy , ServiceFailure {
		_transformations = transformations;
		_context = context;
		_matricesOf = new HashMap<String,Map<MatrixServant,byte[]>>();
		_subscription = context.getLoginRegistry().subscribeObserver(
			LoginObserverHelper.narrow(poa.servant_to_reference(new LoginObserverPOA () {
				public void entityLogout(LoginInfo login) {
					Map<MatrixServant ,byte[]> map;
					synchronized (_matricesOf) { map = _matricesOf.remove(login.id); }
					if (map != null)
						for (byte[] oid : map.values())
							try { _poa().deactivate_object(oid); }
							catch (ObjectNotActive e) {}
							catch (WrongPolicy e) {}
				}
			})));
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

		String loginId = chain.caller().id;
		MatrixServant matrix = new MatrixServant(new Matrix(transformation), _context,
		                                                                     this,
		                                                                     loginId);

		SquareMatrix result;
		try {
			byte[] oid = _poa().activate_object(matrix);
			synchronized (_matricesOf) {
				Map<MatrixServant ,byte[]> map = _matricesOf.get(loginId);
				if (map == null) {
					map = new HashMap<MatrixServant,byte[]>();
					_matricesOf.put(loginId , map);
				}
				map.put(matrix, oid);
			}
			result = SquareMatrixHelper.narrow(_poa().id_to_reference(oid));
		}
		catch (ServantAlreadyActive e) {
			throw new GeneralFailure("failure while activating matrix servant");
		}
		catch (ObjectNotActive e) {
			disposeMatrix(matrix, loginId);
			throw new GeneralFailure("failure while registering matrix servant");
		}
		catch (WrongPolicy e) {
			disposeMatrix(matrix, loginId);
			throw new GeneralFailure("unexpected failure (wrong POA policy)");
		}

		boolean watching = false;
		try { watching = _subscription.watchLogin(loginId); }
		catch(Exception e) {
			disposeMatrix(matrix, loginId);
			System.err.println("failure on watchin login: "+e);
			throw new GeneralFailure("failure while watching login");
		}
		if (!watching) throw new NO_PERMISSION("caller became invalid",
		                                       InvalidLoginCode.value,
		                                       CompletionStatus.COMPLETED_NO);

		return result;
	}

	void disposeMatrix(MatrixServant servant, String loginId) {
		boolean forgetLogin = false;
		synchronized (_matricesOf) {
			Map<MatrixServant ,byte[]> map = _matricesOf.get(loginId);
			if (map != null && map.remove(servant) != null) {
				if (map.isEmpty()) _matricesOf.remove(loginId);
				forgetLogin = true;
			}
		}

		try { _poa().deactivate_object(_object_id()); }
		catch (ObjectNotActive e) {}
		catch (WrongPolicy e) {}

		if (forgetLogin)
			try { _subscription.forgetLogin(loginId); }
			catch(Exception e) {
				System.err.println("failure on forgeting login: "+e);
			}
	}
}

public class Server {
	private static final String entity = "MatricesService";
	private static final String privateKeyFile = "Matrices.key";
	private static final String busHost = "localhost";
	private static final short busPort = 20100;
	private static final String tHost = "localhost";
	private static final short tPort = 22222;

	public static void main(String[] args) throws Exception {
		OpenBusPrivateKey privateKey =
			OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);

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

				TransformationRepository transformations =
					TransformationRepositoryHelper.narrow(
						orb.string_to_object( "corbaloc::"+tHost+":"+tPort+"/Transformations"));

				org.omg.CORBA.Object obj = poa.servant_to_reference(
					new MatrixFactoryServant(transformations, context, poa));

				System.out.println(orb.object_to_string(obj));

				System.in.read();
			}
			finally { conn.logout(); }
		}
		finally { orb.shutdown(true); }
	}
}
