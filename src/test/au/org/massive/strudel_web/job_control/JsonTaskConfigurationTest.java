package au.org.massive.strudel_web.job_control;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by jason on 24/09/15.
 */
public class JsonTaskConfigurationTest {

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
        TaskConfiguration taskConfiguration = JsonSystemConfiguration.getInstance(testConfigurationJson);
        doTests(taskConfiguration);
    }

    @Test
    public void testGetInstance1() throws Exception {
        TaskConfiguration taskConfiguration = JsonSystemConfiguration.getInstance(new URL("http://pastebin.com/raw.php?i=x7Bg7d7g"));
        doTests(taskConfiguration);
    }

    private void doTests(TaskConfiguration taskConfiguration) throws Exception {
        TaskParameters taskParameters;

        // *** startdesktop ***
        taskParameters = taskConfiguration.findByTaskType("startdesktop");
        assertEquals("127.0.0.1", taskParameters.getRemoteHost());
        assertEquals("echo -e '#!/bin/bash\n/usr/local/bin/vncsession --vnc turbovnc ; sleep 36000000 ' | " +
                        "sbatch -p ${queue} -N ${nodes} --mincpus ${ppn} --time=${hours}:${minutes}:00 -J desktop_$(whoami) -o .vnc/slurm-%j.out",
                taskParameters.getCommandPattern());
        assertEquals("^Submitted batch job (?<jobId>[0-9]+)$", taskParameters.getResultRegexPattern());
        assertEquals("batch", taskParameters.getDefaultParams().get("queue"));
        assertEquals("1", taskParameters.getDefaultParams().get("nodes"));
        assertEquals("1", taskParameters.getDefaultParams().get("ppn"));
        assertEquals("1", taskParameters.getDefaultParams().get("hours"));
        assertEquals("00", taskParameters.getDefaultParams().get("minutes"));

        // *** stopjob ***
        taskParameters = taskConfiguration.findByTaskType("stopjob");
        assertEquals("127.0.0.1", taskParameters.getRemoteHost());
        assertEquals("squeue -o \"%i %L %T %j\" -u $(whoami)", taskParameters.getCommandPattern());
        assertEquals("(?<jobId>[0-9]+) (?<remainingWalltime>\\S+) (?<state>\\S+) (?<jobName>\\S+)$", taskParameters.getResultRegexPattern());
        assertEquals(0, taskParameters.getDefaultParams().size());

        // *** vncdisplay ***
        taskParameters = taskConfiguration.findByTaskType("vncdisplay");
        assertEquals("127.0.0.1", taskParameters.getRemoteHost());
        assertEquals("cat .vnc/slurm-${jobid}.out", taskParameters.getCommandPattern());
        assertEquals("^Desktop 'Characterisation Virtual Laboratory' started on display .+:(?<vncDisplay>[0-9]+)", taskParameters.getResultRegexPattern());
        assertTrue(taskParameters.getRequiredParams().size() == 1 && taskParameters.getRequiredParams().contains("jobid"));
        assertEquals(0, taskParameters.getDefaultParams().size());

        // *** vncpassword ***
        taskParameters = taskConfiguration.findByTaskType("vncpassword");
        assertEquals("127.0.0.1", taskParameters.getRemoteHost());
        assertEquals("module load turbovnc ; DISP=$(cat .vnc/slurm-${jobid}.out | grep 'started on display' | awk '{print $(NF)}') ; vncpasswd -o -display $DISP", taskParameters.getCommandPattern());
        assertEquals("^Full control one-time password: (?<password>[^\\s]+)", taskParameters.getResultRegexPattern());
        assertTrue(taskParameters.getRequiredParams().size() == 1 && taskParameters.getRequiredParams().contains("jobid"));
        assertEquals(0, taskParameters.getDefaultParams().size());

        // *** exechost ***
        taskParameters = taskConfiguration.findByTaskType("exechost");
        assertEquals("127.0.0.1", taskParameters.getRemoteHost());
        assertEquals("squeue -j ${jobid} -o \"%N\" | tail -n -1 | cut -f 1 -d ',' | xargs -iname getent hosts name | cut -f 1 -d ' ' ", taskParameters.getCommandPattern());
        assertEquals("^(?<execHost>[^\\s]+)", taskParameters.getResultRegexPattern());
        assertTrue(taskParameters.getRequiredParams().size() == 1 && taskParameters.getRequiredParams().contains("jobid"));
        assertEquals(0, taskParameters.getDefaultParams().size());
    }
}