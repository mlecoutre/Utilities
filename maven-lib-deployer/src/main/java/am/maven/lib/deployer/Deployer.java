package am.maven.lib.deployer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * Basic maven deploy tool.
 * This tool is used to put a set of jar in a folder and its sub folder and deploy them on a maven repository (such as NEXUS)
 * with a same groupId and a same version.
 * Additionally it creates an additional artifact named <i>runtime-libraries</i> that gather all the other artifacts in its dependencies.
 * This tools is very interesting to put on your enterprise repository all java resources on the software editor that does not provide
 * any maven repository for their tools.
 *
 * @author u002617
 * @author e010925
 */
public class Deployer {

    //TODO Input parameters to change EACH TIME you need to make a deploy
    private static final String DESTINATION_VERSION = "4";
    private static final String DESTINATION_GROUPID = "amresources.sap.bo.xi";
    private static final String LOCAL_LIBRARY_PATH = "L:\\bo\\jarfileBOXI4";

    private boolean traceExec = false;

    private List<String> failedList = new ArrayList<String>();

    /**
     * Main Launcher method
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            //Default inputs
            String groupId = DESTINATION_GROUPID;
            String version = DESTINATION_VERSION;
            String localLibraryPath = LOCAL_LIBRARY_PATH;

            if (args != null && args.length == 3) {
                //TODO use commons-cli to make the possibilty to use inline parameters
            }
            Deployer deployer = new Deployer();
            DeployerContext context = new DeployerContext();
            context.initialize();
            deployer.deploy(localLibraryPath, groupId, version, context);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Allow to call maven action to install deploy on the repository
     *
     * @param librariesPath local library path
     * @throws IOException
     * @throws InterruptedException
     */
    public void deploy(String librariesPath, String groupId, String version, DeployerContext context) throws IOException,
            InterruptedException {

        /* list java libraries */
        List<File> libraries = listLibraries(librariesPath);

        /* generate the installation command */
        if (!libraries.isEmpty()) {

            String action = context.getAction();

            Map<String, String> propertiesMap = new LinkedHashMap<String, String>();
            propertiesMap.put("groupId", groupId);
            propertiesMap.put("version", version);
            propertiesMap.put("packaging", "jar");
            propertiesMap.put("generatePom", "true");
            propertiesMap.put("createChecksum", "true");
            if (!context.isTest()) {
                propertiesMap.put("repositoryId", context.getRepositoryId());
                propertiesMap.put("url", context.getRepositoryUrl());
            }

            List<String> dependenciesList = new ArrayList<String>();

            int max = libraries.size();
            int count = 0;
            for (File lib : libraries) {
                propertiesMap.put("file", lib.getAbsolutePath());
                String fileName = lib.getName();
                String artifactId = fileName
                        .substring(0, fileName.length() - 4);
                propertiesMap.put("artifactId", artifactId);

                count++;
                System.out.println(String.valueOf(count) + "/"
                        + String.valueOf(max) + "\t" + action + " "
                        + groupId + ":" + artifactId + ":" + version);
                launchMaven(propertiesMap, context);
                dependenciesList.add(artifactId);
            }

            /* DISPLAY THE LIST OF FAILED UPLOAD */
            System.out.println("FAILED LIST OF UPLOAD");
            for (String failedUpload : this.failedList) {
                System.err.println(failedUpload);
            }

            /* GENERATE VELOCITY */
            File outFile = new File(context.getTempFolder(), "pom.xml");
            Writer w = new FileWriter(outFile);
            boolean isSuccess = runVelocity(groupId, version, dependenciesList, w);
            if (!isSuccess) return;
            System.out.println("Generated " + outFile.getPath());

            /* DEPLOY RUNTIME-LIBRARIES ARTIFACT */
            String artifactId = "runtime-libraries";
            System.out.println(action + " " + groupId + ":" + artifactId
                    + ":" + version);

            propertiesMap.put("packaging", "pom");
            propertiesMap.put("generatePom", "false");
            propertiesMap.put("createChecksum", "true");
            propertiesMap.put("file", outFile.getAbsolutePath());
            propertiesMap.put("artifactId", artifactId);

            launchMaven(propertiesMap, context);
            System.out.println("Done.");
        }
    }

    /**
     * Initialize Velocity context
     *
     * @param groupId          groupId
     * @param version          version
     * @param dependenciesList dependenciesList
     * @param w                output writer
     * @return boolean value to indicate success
     */
    public boolean runVelocity(String groupId, String version, List<String> dependenciesList, Writer w) {
        /* try to localize the pom template */
        File resourceFile = null;
        try {
            ClassLoader classLoader = Thread.currentThread()
                    .getContextClassLoader();
            URL resourceUrl = classLoader.getResource("pom.xml.vm");
            resourceFile = new File(resourceUrl.getFile());
        } catch (Exception e) {
            System.out
                    .println("Problem during template localization : " + e);
            return false;
        }
        /* first, we init the runtime engine. Defaults are fine. */
        try {
            Properties p = new Properties();
            p.setProperty("file.resource.loader.path",
                    resourceFile.getParent());
            p.setProperty("output.encoding", "ISO-8859-1");
            Velocity.init(p);
        } catch (Exception e) {
            System.out.println("Problem initializing Velocity : " + e);
            return false;
        }

        /* lets make a Context and put data into it */
        VelocityContext vContext = new VelocityContext();
        vContext.put("groupId", groupId);
        vContext.put("version", version);
        vContext.put("dependencies", dependenciesList);

        try {
            Velocity.mergeTemplate(resourceFile.getName(), "ISO-8859-1",
                    vContext, w);
            w.flush();
            w.close();
        } catch (Exception e) {
            System.out.println("Problem merging template : " + e);
            return false;
        }
        return true;
    }

    /**
     * list Libraries
     *
     * @param librariesPath
     * @return list of libraries (jar)
     */
    public List<File> listLibraries(String librariesPath) {
        List<File> libraries = new ArrayList<File>();

        String[] subPaths = librariesPath.split(";");
        if ((null != subPaths) && (0 < subPaths.length)) {
            for (String subPath : subPaths) {
                subPath = subPath.trim();
                File dirFile = new File(subPath);
                visitDir(dirFile, libraries);
            }
        }
        return libraries;
    }

    /**
     * Visit Dir. Filter only jar files.
     * TODO remove recursif calls for better performance
     * @param dirFile   folder
     * @param libraries
     */
    private void visitDir(File dirFile, List<File> libraries) {

        File[] files = dirFile.listFiles();
        if ((null != files) && (0 < files.length)) {
            for (File file : files) {
                if (file.isDirectory()) {
                    visitDir(file, libraries);
                    continue;
                }
                if (file.getName().endsWith(".jar")) {
                    libraries.add(file);
                }
            }
        }
    }

    /**
     * launch Maven for install or deploy goal
     *
     * @param propertiesMap
     * @param context       execution context
     * @return process exitVal
     * @throws IOException
     * @throws InterruptedException
     */
    public int launchMaven(Map<String, String> propertiesMap, DeployerContext context)
            throws IOException, InterruptedException {
        String action = "install:install-file";
        if (!context.isTest())
            action = "deploy:deploy-file";

        List<String> command = new ArrayList<String>();
        command.add(context.getMvnCmd());
        command.add(action);
        for (String key : propertiesMap.keySet()) {
            String value = propertiesMap.get(key);
            String optionString = "-D".concat(key).concat("=").concat(value);
            command.add(optionString);
        }

        return execCommand(command, context);
    }

    /**
     * execCommand with a retry mechanism when  exitVal==-1
     *
     * @param command
     * @return exitValue
     * @throws IOException
     * @throws InterruptedException
     */
    public int execCommand(List<String> command, DeployerContext context) throws IOException,
            InterruptedException {
        int nbRetries = context.getMaxRetries();
        boolean isSuccess = false;
        int exitVal = 0;
        if (this.traceExec) {
            command.add("-e");
        }
        while (nbRetries > 0 && !isSuccess) {

            System.out.println(command.toString());
            // Process process = new ProcessBuilder(command).start();
            Process process = Runtime.getRuntime().exec(
                    command.toArray(new String[0]), context.getEnv());

            InputStream stream = process.getInputStream();
            BufferedInputStream stream2 = new BufferedInputStream(stream);
            byte[] buffer = new byte[256];
            int len;
            StringBuffer strBuffer = new StringBuffer();
            while ((len = stream2.read(buffer)) > 0) {
                strBuffer.append(new String(buffer, 0, len));
            }
            if (this.traceExec)
                System.out.println(strBuffer.toString());

            exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println("SUCCEED.");
                isSuccess = true;
            } else if (nbRetries >= 0) {
                System.out.println(" FAILED, retry once more.");
                nbRetries--;
            } else {
                System.out
                        .println(" DEFINITELY FAILED. Push cmd on the failed list.");
                failedList.add(command.toArray(new String[0])[0]);
            }
        }
        return exitVal;
    }



}
