import am.maven.lib.deployer.Deployer;
import am.maven.lib.deployer.DeployerContext;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Basic tests
 * User: E010925
 * Date: 26/10/12
 * Time: 11:49
 */
public class DeployerTest {

    @Test
    public void testDeployerContext() {
        DeployerContext ctx = new DeployerContext();
        ctx.initialize();
        System.out.println("Context: " + ctx);
        assertTrue(ctx.getAction().equals("deploy"));
        assertTrue(ctx.getMavenHome() != null);
        assertTrue(ctx.getJavaHome() != null);
        assertTrue(ctx.getMvnCmd() != null && ctx.getMvnCmd().contains("/bin/mvn."));
    }

    @Test
    public void testListLibraries() {
        String mavenhome = System.getenv("MAVEN_HOME");
        Deployer dd = new Deployer();
        List<File> files = dd.listLibraries(mavenhome);
        assertTrue("Maven has several jar",  files != null && files.size() >= 1);
    }

    @Test
    public void testExecCommand() throws Exception {
        Deployer dd = new Deployer();
        DeployerContext dc = new DeployerContext();
        dc.initialize();
        List<String> cmd = new ArrayList<String>();
        cmd.add("ping");
        cmd.add("-a");
        cmd.add("localhost");
        int returnVal = dd.execCommand(cmd, dc);
        assertTrue("ping cmd should work", returnVal == 0);

        List<String> errCmd = new ArrayList<String>();
        cmd.add("errCmd");
        cmd.add("-zx");
        returnVal = dd.execCommand(cmd, dc);
        assertTrue("errCmd exitVal should be equal to -1", returnVal == 1);
    }

    @Test
    public void testRunVelocity() {
        Deployer dd = new Deployer();
        Writer w = new StringWriter();
        List<String> dependencies = new ArrayList<String>();
        dependencies.add("d1");
        dependencies.add("d2");
        boolean result = dd.runVelocity("groupId", "version", dependencies, w);
        System.out.println("testRunVelocity: " + w);

        assertTrue("result should be ok", result);
        assertTrue("resultString should contains d1", w.toString().contains("d1"));
        assertTrue("resultString should not be d2", w.toString().contains("d2"));
    }
}
