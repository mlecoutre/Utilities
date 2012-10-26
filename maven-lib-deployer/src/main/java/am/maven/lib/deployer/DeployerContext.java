package am.maven.lib.deployer;

import java.util.*;

/**
 * Gather all configuration data
 * User: E010925
 */
public class DeployerContext {

    private static final String DEFAULT_DESTINATION_REPOSITORYID = "am-resources-repository-dm";
    private static final String DEFAULT_DESTINATION_REPOSITORY = "http://web-maven-repo/nexus/content/repositories/resources";

    public static final String ACTION_DEPLOY = "deploy";
    public static final String ACTION_INSTALL = "install";

    private String tempFolder;
    private String javaHome;
    private String mavenHome;
    private String mvnCmd;


    private String repositoryId = DEFAULT_DESTINATION_REPOSITORYID;
    private String repositoryUrl = DEFAULT_DESTINATION_REPOSITORY;

    private int maxRetries = 10;

    private boolean test = false;
    private String action = ACTION_DEPLOY;
    private String[] env = null;

    public DeployerContext() {
        tempFolder = System.getProperty("java.io.tmpdir");
        javaHome = System.getProperty("java.home");
        mavenHome = System.getenv("MAVEN_HOME");

    }

    /**
     * Gather all environment information.
     *
     * @return information about the deployment context
     */
    public DeployerContext initialize() {

        Map<String, String> envMap = System.getenv();
        Map<String, String> newEnvMap = new HashMap<String, String>(envMap);
        newEnvMap.put("JAVA_HOME", javaHome);
        newEnvMap.put("MAVEN_HOME", this.mavenHome);

        if (System.getProperty("os.name").contains("Windows")) {
            mvnCmd = mavenHome.concat("/bin/mvn.bat");
        } else {
            mvnCmd = mavenHome.concat("/bin/mvn.sh");
        }

        List<String> env = new ArrayList<String>();
        for (String key : envMap.keySet()) {
            String value = envMap.get(key);
            String envVar = key.concat("=").concat(value);
            env.add(envVar);
        }
        this.env = env.toArray(new String[0]);

        return this;
    }

    public String getMvnCmd() {
        return mvnCmd;
    }

    public void setMvnCmd(String mvnCmd) {
        this.mvnCmd = mvnCmd;
    }

    public String[] getEnv() {
        return env;
    }

    public void setEnv(String[] env) {
        this.env = env;
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

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMavenHome() {
        return mavenHome;
    }

    public void setMavenHome(String mavenHome) {
        this.mavenHome = mavenHome;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getTempFolder() {
        return tempFolder;
    }

    public void setTempFolder(String tempFolder) {
        this.tempFolder = tempFolder;
    }

    @Override
    public String toString() {
        return "DeployerContext{" +
                "tempFolder='" + tempFolder + '\'' +
                ", javaHome='" + javaHome + '\'' +
                ", mavenHome='" + mavenHome + '\'' +
                ", mvnCmd='" + mvnCmd + '\'' +
                ", repositoryId='" + repositoryId + '\'' +
                ", repositoryUrl='" + repositoryUrl + '\'' +
                ", maxRetries=" + maxRetries +
                ", test=" + test +
                ", action='" + action + '\'' +
                ", env=" + (env == null ? null : Arrays.asList(env)) +
                '}';
    }
}
