package au.org.massive.strudel_web.ssh;

import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.commons.exec.ExecuteWatchdog;

/**
 * A minimal set of methods for a SSH client
 * @author jrigby
 *
 */
public interface SSHClient {
	
	public String exec(String remoteCommands) throws IOException, SSHExecException;

	// TODO: ExecuteWatchdog should not be here, because it implies a forked process.
	public String exec(String remoteCommands, ExecuteWatchdog watchdog) throws IOException, SSHExecException;
	
	public AsyncCommand<String> execAsync(String remoteCommands) throws InterruptedException, IOException, SSHExecException;
	
	public class AsyncCommand<T> {
		private final ExecuteWatchdog watchdog;
		private final Future<T> future;
		public AsyncCommand(ExecuteWatchdog watchdog, Future<T> future) {
			super();
			this.watchdog = watchdog;
			this.future = future;
		}
		public ExecuteWatchdog getWatchdog() {
			return watchdog;
		}
		public Future<T> getFuture() {
			return future;
		}
	}
}
