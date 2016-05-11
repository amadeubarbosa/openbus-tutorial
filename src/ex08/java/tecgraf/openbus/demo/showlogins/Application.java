package tecgraf.openbus.demo.showlogins;

import javax.xml.bind.DatatypeConverter;
import org.omg.CORBA.ORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.OpenBusContext;

public class Application {
	private static final String busHost = "localhost";
	private static final short busPort = 20100;

	public static void main(String[] args) throws Exception {
		String secret = args[0];

		ORB orb = ORBInitializer.initORB(args);
		try {
			OpenBusContext context = (OpenBusContext)
				orb.resolve_initial_references("OpenBusContext");

			Connection conn = context.createConnection(busHost, busPort);
			context.setDefaultConnection(conn);

			byte[] encoded = DatatypeConverter.parseBase64Binary(secret);
			conn.loginBySharedAuth(context.decodeSharedAuth(encoded));
			try {
				LoginRegistry logins = context.getLoginRegistry();
				LoginInfo[] list = logins.getEntityLogins(conn.login().entity);
				for (LoginInfo login : list)
					System.out.println(login.id);
			}
			finally { conn.logout(); }
		}
		finally { orb.shutdown(true); }
	}
}
