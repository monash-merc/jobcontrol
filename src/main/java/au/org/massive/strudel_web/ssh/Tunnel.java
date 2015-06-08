package au.org.massive.strudel_web.ssh;

/**
 * Methods to control an SSH tunnel
 * @author jrigby
 *
 */
public interface Tunnel {
	public int getLocalPort();
	public int getRemotePort();
	public String getRemoteHost();
	public void stopTunnel();
	public boolean isRunning();
}
