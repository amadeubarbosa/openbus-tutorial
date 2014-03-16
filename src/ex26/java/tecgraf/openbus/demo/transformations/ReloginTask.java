package tecgraf.openbus.demo.transformations;

import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

public class ReloginTask implements Runnable {
	private Object lock = new Object();
	private boolean notification = false;
	private String lastLogin = null;
	private Connection connection;
	private ReloginAllocator allocator;

	public ReloginTask(Connection conn, ReloginAllocator alloc) {
		connection = conn;
		allocator = alloc;
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	private void waitRelogin() {
		synchronized (lock) {
			while (!notification) {
				try { lock.wait(); }
				catch (InterruptedException e) {}
			}
			notification = false;
		}
	}

	public void notifyRelogin() {
		synchronized (lock) {
			notification = true;
			lock.notifyAll();
		}
	}

	public void run() {
		while (true) {
			waitRelogin();
			LoginInfo login = connection.login();
			if ( (login != null) && (login.id != lastLogin) ) {
				for (int i=1; i<Integer.MAX_VALUE; ++i) {
					try {
						lastLogin = allocator.reallocate(login);
						break;
					}
					catch (Exception e) {
						System.err.println("reallocation failure: "+e);
						if (connection.login() == null) break;
						try { Thread.sleep(i*1000); }
						catch (InterruptedException ie) {}
					}
				}
			}
		}
	}
}
