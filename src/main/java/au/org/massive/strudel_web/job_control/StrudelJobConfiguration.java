package au.org.massive.strudel_web.job_control;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import au.org.massive.strudel_web.Session;
import au.org.massive.strudel_web.SessionManager;
import au.org.massive.strudel_web.vnc.GuacamoleSessionManager;

/**
 * A sample configuration object that is similar to the Strudel desktop application
 * @author jrigby
 *
 */
@SuppressWarnings("unused")
public class StrudelJobConfiguration extends AbstractJobConfiguration {
	
	public StrudelJobConfiguration(String loginHost) {
		super(loginHost);
		
		// Use reflection to invoke all configuration methods, i.e. methods beginning with config_
		for (Method m : this.getClass().getDeclaredMethods()) {
			if (m.getName().startsWith("config_")) {
				try {
					m.invoke(this);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	private void config_StartDesktop() {
		// Job name
		String jobName = "startdesktop";
		
		Map<String,String> defaults = new HashMap<String,String>();
		// default values
		defaults.put("queue", "batch");
		defaults.put("nodes", "1");
		defaults.put("ppn", "1");
		defaults.put("hours", "1");
		defaults.put("minutes", "00");
		
		// required parameters
		String[] requiredParams = {};

		String commandPattern = "echo -e '#!/bin/bash\n/usr/local/bin/vncsession --vnc turbovnc ; sleep 36000000 ' | "
				+ "sbatch -p ${queue} -N ${nodes} --mincpus ${ppn} --time=${hours}:${minutes}:00 -J desktop_$(whoami) -o .vnc/slurm-%j.out";
		String resultPattern = "^Submitted batch job (?<jobId>[0-9]+)$";
		
		addConfiguration(jobName, defaults, requiredParams, commandPattern, resultPattern);
	}
	
	private void config_StopJob() {
		// Job name
		String jobName = "stopjob";
		
		Map<String,String> defaults = new HashMap<String,String>();
		// default values
		// none
		
		// required parameters
		String[] requiredParams = { "jobid" };
		
		String commandPattern = "scancel ${jobid}";
		String resultPattern = "(?<message>.+)";
		
		addConfiguration(jobName, defaults, requiredParams, commandPattern, resultPattern);
	}
	
	private void config_ListJobs() {
		// Job name
		String jobName = "listjobs";
		
		Map<String,String> defaults = new HashMap<String,String>();
		// default values
		// none
		
		// required parameters
		String[] requiredParams = {};
		
		String commandPattern = "squeue -o \"%i %L %T %j\" -u $(whoami)";
		String resultPattern = "(?<jobId>[0-9]+) (?<remainingWalltime>\\S+) (?<state>\\S+) (?<jobName>\\S+)$";
		
		addConfiguration(jobName, defaults, requiredParams, commandPattern, resultPattern);
	}
	
	private void config_VNCDisplayNumber() {
		// Job name
		String jobName = "vncdisplay";
		
		Map<String,String> defaults = new HashMap<String,String>();
		// default values
		// none
		
		// required parameters
		String[] requiredParams = { "jobid" };
		
		String commandPattern = "cat .vnc/slurm-${jobid}.out";
		String resultPattern = "^Desktop 'Characterisation Virtual Laboratory' started on display .+:(?<vncDisplay>[0-9]+)";
		//String resultPattern = "New 'X' desktop is \\S+:(?<vncDisplay>[0-9]+)";
		
		addConfiguration(jobName, defaults, requiredParams, commandPattern, resultPattern);
	}
	
	private void config_VNCPassword() {
		// Job name
		String jobName = "vncpassword";
		
		Map<String,String> defaults = new HashMap<String,String>();
		// default values
		// none
		
		// required parameters
		String[] requiredParams = { "jobid" };
		
		String commandPattern = "module load turbovnc ; " +
				"DISP=$(cat .vnc/slurm-${jobid}.out | grep 'started on display' | awk '{print $(NF)}') ; " +
				"vncpasswd -o -display $DISP";
		String resultPattern = "^Full control one-time password: (?<password>[^\\s]+)";
		
		addConfiguration(jobName, defaults, requiredParams, commandPattern, resultPattern);
	}
	
	private void config_ExecHost() {
		// Job name
		String jobName = "exechost";
		
		Map<String,String> defaults = new HashMap<String,String>();
		// default values
		// none
		
		// required parameters
		String[] requiredParams = { "jobid" };
		
		String commandPattern = "squeue -j ${jobid} -o \"%N\" | tail -n -1 | cut -f 1 -d ',' | xargs -iname getent hosts name | cut -f 1 -d ' ' ";
		String resultPattern = "^(?<execHost>[^\\s]+)";
				
		addConfiguration(jobName, defaults, requiredParams, commandPattern, resultPattern);
	}

}
