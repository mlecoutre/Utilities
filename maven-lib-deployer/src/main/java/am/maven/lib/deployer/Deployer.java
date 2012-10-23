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
 * Basic maven deployer tool.
 * This tool is used to put a set of jar in a folder and its sub fodler and deploy them on a maven repository (such as NEXUS)
 * with a same groupId and a same version. 
 * Additionally it creates an additional artifact named <i>runtime-libraries</i> that gather all the other artifacts in its dependencies.
 * This tools is very interesting to put on your enterprise repository all java resources on the software editor that does not provide
 * any maven repository for their tools.
 * 
 * @author u002617
 * @author e010925
 *
 */
public class Deployer {

	private static final String DESTINATION_REPOSITORYID = "am-resources-repository-dm";
	private static final String DESTINATION_REPOSITORY = "http://web-maven-repo/nexus/content/repositories/resources";
	private static final String DESTINATION_VERSION = "4";
	private static final String DESTINATION_GROUPID = "amresources.sap.bo.xir4";
	
	//TODO should catch environment values
	private static final String JAVA_HOME = "C:/program files/java/jdk1.7.0_07";
	private static final String MAVEN_HOME = "L:/appli/apache-maven-3.0.4";

	private static final String LOCAL_LIBRARY_PATH = "l:/bo";
	
	
	private static final int MAX_RETRIES = 10;
	private boolean traceExec = false;
	private boolean test = true;

	private String javaHome = "";
	private String mavenHome = "";
	private String[] env = null;

	private String repositoryUrl = "";
	private String repositoryId = "";

	private String groupId = "";
	private String version = "";

	private List<String> failedList = new ArrayList<String>();

	public static void main(String[] args) {

	

		try {
			String groupId = DESTINATION_GROUPID;
			String version = DESTINATION_VERSION;
			boolean test = false;
			Deployer deployer = new Deployer(JAVA_HOME, MAVEN_HOME, groupId,
					version, test);
			deployer.setRepositoryId(DESTINATION_REPOSITORYID);
			deployer.setRepositoryUrl(DESTINATION_REPOSITORY);
			deployer.setTraceExec(true);
			deployer.deploy(LOCAL_LIBRARY_PATH);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param javaHome
	 * @param mavenHome
	 * @param test
	 */
	public Deployer(String javaHome, String mavenHome, String groupId,
			String version, boolean test) {
		super();
		this.javaHome = javaHome;
		this.mavenHome = mavenHome;
		this.groupId = groupId;
		this.version = version;
		this.test = test;

		Map<String, String> envMap = System.getenv();
		Map<String, String> newEnvMap = new HashMap<String, String>(envMap);
		newEnvMap.put("JAVA_HOME", this.javaHome);
		newEnvMap.put("MAVEN_HOME", this.mavenHome);

		List<String> env = new ArrayList<String>();
		for (String key : envMap.keySet()) {
			String value = envMap.get(key);
			String envVar = key.concat("=").concat(value);
			env.add(envVar);
		}
		this.env = env.toArray(new String[0]);

	}

	public boolean isTraceExec() {
		return traceExec;
	}

	public void setTraceExec(boolean traceExec) {
		this.traceExec = traceExec;
	}

	/**
	 * 
	 * @param librariesPath
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void deploy(String librariesPath) throws IOException,
			InterruptedException {

		/* list java libraries */
		List<File> libraries = listLibraries(librariesPath);

		/* generate the installation command */
		if (!libraries.isEmpty()) {

			String action = "install";
			if (!this.test)
				action = "deploy";

			Map<String, String> propertiesMap = new LinkedHashMap<String, String>();
			propertiesMap.put("groupId", this.groupId);
			propertiesMap.put("version", this.version);
			propertiesMap.put("packaging", "jar");
			propertiesMap.put("generatePom", "true");
			propertiesMap.put("createChecksum", "true");
			if (!this.test) {
				propertiesMap.put("repositoryId", this.repositoryId);
				propertiesMap.put("url", this.repositoryUrl);
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
						+ this.groupId + ":" + artifactId + ":" + version);
				launchMaven(propertiesMap);

				dependenciesList.add(artifactId);

			}

			/* DISPLAY THE LIST OF FAILED UPLOAD */
			System.out.println("FAILED LIST OF UPDLOAD");
			for (String failedUpload : this.failedList) {
				System.err.println(failedUpload);
			}

			/* try to localized the pom template */
			File resourceFile;
			try {
				ClassLoader classLoader = Thread.currentThread()
						.getContextClassLoader();
				URL resourceUrl = classLoader.getResource("pom.xml.vm");
				resourceFile = new File(resourceUrl.getFile());
			} catch (Exception e) {
				System.out
						.println("Problem during template loclization : " + e);
				return;
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
				return;
			}

			/* lets make a Context and put data into it */
			VelocityContext context = new VelocityContext();
			context.put("groupId", this.groupId);
			context.put("version", this.version);
			context.put("dependencies", dependenciesList);

			File outFile = new File("c:/temp", "pom.xml");
			try {
				Writer w = new FileWriter(outFile);
				Velocity.mergeTemplate(resourceFile.getName(), "ISO-8859-1",
						context, w);
				w.flush();
				w.close();
				System.out.println("Generated " + outFile.getPath());
			} catch (Exception e) {
				System.out.println("Problem merging template : " + e);
				return;
			}

			String artifactId = "runtime-libraries";
			System.out.println(action + " " + this.groupId + ":" + artifactId
					+ ":" + version);

			propertiesMap.put("packaging", "pom");
			propertiesMap.put("generatePom", "false");
			propertiesMap.put("createChecksum", "true");
			propertiesMap.put("file", outFile.getAbsolutePath());
			propertiesMap.put("artifactId", artifactId);

			launchMaven(propertiesMap);
			System.out.println("Done.");

		}

	}

	/**
	 * 
	 * @param librariesPath
	 * @return
	 */
	private List<File> listLibraries(String librariesPath) {
		List<File> libraries = new ArrayList<File>();

		String[] subPaths = librariesPath.split(",");
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
	 * launch Maven for install or deploy goal
	 * @param propertiesMap
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int launchMaven(Map<String, String> propertiesMap)
			throws IOException, InterruptedException {

		String cmd = this.mavenHome.concat("/bin/mvn.bat");
		String action = "install:install-file";
		if (!test)
			action = "deploy:deploy-file";

		List<String> command = new ArrayList<String>();
		command.add(cmd);
		command.add(action);
		for (String key : propertiesMap.keySet()) {
			String value = propertiesMap.get(key);
			String optionString = "-D".concat(key).concat("=").concat(value);
			command.add(optionString);
		}

		return execCommand(command);

	}

	/**
	 * execCommand with a retry mechanism when  exitVal==-1
	 * @param command
	 * @return exitValue
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int execCommand(List<String> command) throws IOException,
			InterruptedException {
		int nbRetries = MAX_RETRIES;
		boolean isSuccess = false;
		int exitVal = 0;
		if (this.traceExec) {
			command.add("-e");
		}
		while (nbRetries > 0 && !isSuccess) {

			System.out.println(command.toString());
			// Process process = new ProcessBuilder(command).start();
			Process process = Runtime.getRuntime().exec(
					command.toArray(new String[0]), env);

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
				System.out.println("SUCCEED ");
				isSuccess = true;
			} else if (nbRetries >= 0) {
				System.out.println(" FAILED, retry once more");
				nbRetries--;
			} else {
				System.out
						.println(" DEFINITELY FAILED. Push cmd on the failed list.");
				failedList.add(command.toArray(new String[0])[0]);
			}
		}

		return exitVal;
	}

	/**
	 * visitDir
	 * @param dirFile folder
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

	public boolean isTest() {
		return test;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	public String getJavaHome() {
		return javaHome;
	}

	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}

	public String getMavenHome() {
		return mavenHome;
	}

	public void setMavenHome(String mavenHome) {
		this.mavenHome = mavenHome;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	public void setRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl;
	}

}
