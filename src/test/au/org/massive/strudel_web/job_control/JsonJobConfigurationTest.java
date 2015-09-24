package au.org.massive.strudel_web.job_control;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by jason on 24/09/15.
 */
public class JsonJobConfigurationTest {

    private static final String testConfigurationJson = "{\n" +
            "    \"loginHost\": \"127.0.0.1\",\n" +
            "    \"tasks\": {\n" +
            "        \"startdesktop\": {\n" +
            "            \"commandPattern\": \"echo -e '#!/bin/bash\\n/usr/local/bin/vncsession --vnc turbovnc ; sleep 36000000 ' | sbatch -p ${queue} -N ${nodes} --mincpus ${ppn} --time=${hours}:${minutes}:00 -J desktop_$(whoami) -o .vnc/slurm-%j.out\",\n" +
            "            \"resultPattern\": \"^Submitted batch job (?<jobId>[0-9]+)$\",\n" +
            "            \"defaults\": {\n" +
            "                \"queue\": \"batch\",\n" +
            "                \"nodes\": \"1\",\n" +
            "                \"ppn\": \"1\",\n" +
            "                \"hours\": \"1\",\n" +
            "                \"minutes\": \"00\"\n" +
            "            }\n" +
            "        },\n" +
            "        \"stopjob\": {\n" +
            "            \"commandPattern\": \"squeue -o \\\"%i %L %T %j\\\" -u $(whoami)\",\n" +
            "            \"resultPattern\": \"(?<jobId>[0-9]+) (?<remainingWalltime>\\\\S+) (?<state>\\\\S+) (?<jobName>\\\\S+)$\"\n" +
            "        },\n" +
            "        \"vncdisplay\": {\n" +
            "            \"commandPattern\": \"cat .vnc/slurm-${jobid}.out\",\n" +
            "            \"resultPattern\": \"^Desktop 'Characterisation Virtual Laboratory' started on display .+:(?<vncDisplay>[0-9]+)\",\n" +
            "            \"required\": [\n" +
            "                \"jobid\"\n" +
            "            ]\n" +
            "        },\n" +
            "        \"vncpassword\": {\n" +
            "            \"commandPattern\": \"module load turbovnc ; DISP=$(cat .vnc/slurm-${jobid}.out | grep 'started on display' | awk '{print $(NF)}') ; vncpasswd -o -display $DISP\",\n" +
            "            \"resultPattern\": \"^Full control one-time password: (?<password>[^\\\\s]+)\",\n" +
            "            \"required\": [\n" +
            "                \"jobid\"\n" +
            "            ]\n" +
            "        },\n" +
            "        \"exechost\": {\n" +
            "            \"commandPattern\": \"squeue -j ${jobid} -o \\\"%N\\\" | tail -n -1 | cut -f 1 -d ',' | xargs -iname getent hosts name | cut -f 1 -d ' ' \",\n" +
            "            \"resultPattern\": \"^(?<execHost>[^\\\\s]+)\",\n" +
            "            \"required\": [\n" +
            "                \"jobid\"\n" +
            "            ]                                                                                                                                                                                             \n" +
            "        }                                                                                                                                                                                                 \n" +
            "    }                                                                                                                                                                                                     \n" +
            "}";

    @Test
    public void testGetInstance() throws Exception {
        JobConfiguration jobConfiguration = JsonJobConfiguration.getInstance(testConfigurationJson);
        doTests(jobConfiguration);
    }

    @Test
    public void testGetInstance1() throws Exception {
        JobConfiguration jobConfiguration = JsonJobConfiguration.getInstance(new URL("http://pastebin.com/raw.php?i=x7Bg7d7g"));
        doTests(jobConfiguration);
    }

    private void doTests(JobConfiguration jobConfiguration) throws Exception {
        JobParameters jobParameters;

        // *** startdesktop ***
        jobParameters = jobConfiguration.findByJobType("startdesktop");
        assertEquals("127.0.0.1", jobParameters.getRemoteHost());
        assertEquals("echo -e '#!/bin/bash\n/usr/local/bin/vncsession --vnc turbovnc ; sleep 36000000 ' | " +
                        "sbatch -p ${queue} -N ${nodes} --mincpus ${ppn} --time=${hours}:${minutes}:00 -J desktop_$(whoami) -o .vnc/slurm-%j.out",
                jobParameters.getCommandPattern());
        assertEquals("^Submitted batch job (?<jobId>[0-9]+)$", jobParameters.getResultRegexPattern());
        assertEquals("batch", jobParameters.getDefaultParams().get("queue"));
        assertEquals("1", jobParameters.getDefaultParams().get("nodes"));
        assertEquals("1", jobParameters.getDefaultParams().get("ppn"));
        assertEquals("1", jobParameters.getDefaultParams().get("hours"));
        assertEquals("00", jobParameters.getDefaultParams().get("minutes"));

        // *** stopjob ***
        jobParameters = jobConfiguration.findByJobType("stopjob");
        assertEquals("127.0.0.1", jobParameters.getRemoteHost());
        assertEquals("squeue -o \"%i %L %T %j\" -u $(whoami)", jobParameters.getCommandPattern());
        assertEquals("(?<jobId>[0-9]+) (?<remainingWalltime>\\S+) (?<state>\\S+) (?<jobName>\\S+)$", jobParameters.getResultRegexPattern());
        assertEquals(0, jobParameters.getDefaultParams().size());

        // *** vncdisplay ***
        jobParameters = jobConfiguration.findByJobType("vncdisplay");
        assertEquals("127.0.0.1", jobParameters.getRemoteHost());
        assertEquals("cat .vnc/slurm-${jobid}.out", jobParameters.getCommandPattern());
        assertEquals("^Desktop 'Characterisation Virtual Laboratory' started on display .+:(?<vncDisplay>[0-9]+)", jobParameters.getResultRegexPattern());
        assertTrue(jobParameters.getRequiredParams().size() == 1 && jobParameters.getRequiredParams().contains("jobid"));
        assertEquals(0, jobParameters.getDefaultParams().size());

        // *** vncpassword ***
        jobParameters = jobConfiguration.findByJobType("vncpassword");
        assertEquals("127.0.0.1", jobParameters.getRemoteHost());
        assertEquals("module load turbovnc ; DISP=$(cat .vnc/slurm-${jobid}.out | grep 'started on display' | awk '{print $(NF)}') ; vncpasswd -o -display $DISP", jobParameters.getCommandPattern());
        assertEquals("^Full control one-time password: (?<password>[^\\s]+)", jobParameters.getResultRegexPattern());
        assertTrue(jobParameters.getRequiredParams().size() == 1 && jobParameters.getRequiredParams().contains("jobid"));
        assertEquals(0, jobParameters.getDefaultParams().size());

        // *** exechost ***
        jobParameters = jobConfiguration.findByJobType("exechost");
        assertEquals("127.0.0.1", jobParameters.getRemoteHost());
        assertEquals("squeue -j ${jobid} -o \"%N\" | tail -n -1 | cut -f 1 -d ',' | xargs -iname getent hosts name | cut -f 1 -d ' ' ", jobParameters.getCommandPattern());
        assertEquals("^(?<execHost>[^\\s]+)", jobParameters.getResultRegexPattern());
        assertTrue(jobParameters.getRequiredParams().size() == 1 && jobParameters.getRequiredParams().contains("jobid"));
        assertEquals(0, jobParameters.getDefaultParams().size());
    }
}