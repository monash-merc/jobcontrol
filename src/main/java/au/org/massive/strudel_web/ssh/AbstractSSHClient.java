package au.org.massive.strudel_web.ssh;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;

import au.org.massive.strudel_web.AsyncTasks;

/**
 * An abstract SSH client that provides command execution and async command execution methods
 * @author jrigby
 *
 */
public abstract class AbstractSSHClient implements SSHClient {
	
	private final CertAuthInfo authInfo;
	private final String remoteHost;

	public AbstractSSHClient(CertAuthInfo authInfo, String remoteHost) {
		this.authInfo = authInfo;
		this.remoteHost = remoteHost;
	}
	
	protected Executor getForkedProcessExecutor(ExecuteWatchdog watchdog) {
		Executor exec = new DefaultExecutor();
		if (watchdog != null) {
			exec.setWatchdog(watchdog);
		}
		return exec;
	}
	
	protected ExecutorService getExecutorService() {
		return AsyncTasks.getExecutorService();
	}
	
	protected CertAuthInfo getAuthInfo() {
		return authInfo;
	}
	
	protected String getRemoteHost() {
		return remoteHost;
	}
	
	@Override
	public String exec(String remoteCommands) throws IOException, SSHExecException {
		return exec(remoteCommands, null);
	}
	
	@Override
	public AsyncCommand<String> execAsync(final String remoteCommands) {
		final ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
		return new AsyncCommand<String>(watchdog, getExecutorService().submit(new Callable<String>(){

			@Override
			public String call() throws Exception {
				return exec(remoteCommands, watchdog);
			}
			
		}));
	}
}
