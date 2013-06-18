package hudson.plugins.android_emulator;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.EnvVars;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.TaskListener;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintStream;
import java.io.IOException;
import java.io.File;
import java.util.*;

import hudson.tasks.CommandInterpreter;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.plugins.android_emulator.util.SrcFiles;

public class UiAutomator extends CommandInterpreter {
	private final String projectPath;
	private final String packageName;
	private String appPath[];
	private String fileRepoPath;
	private String targetId;
	private String serialNo;
	
@DataBoundConstructor
    public UiAutomator(String projectPath, String packageName) {		
       super(null);
	   this.projectPath = projectPath;
	   this.packageName = packageName;
    }
	
	public String getProjectPath() {
		return projectPath;
	}
	
	public String getPackageName() {
		return packageName;
	}
	 public String[] buildCommandLine(FilePath script) {
        return new String[] {"cmd","/c","call",script.getRemote()};
    }

    protected String getContents() {
		String absoluteAppPath = appPath[0];
		String appName = absoluteAppPath.substring(absoluteAppPath.lastIndexOf("\\")+1).trim();
		String buildCmd = "call android create uitest-project -n "+appName+" -p "+"\""+absoluteAppPath+"\""+" -t "+targetId;
		String switchToWorkspace = "cd "+"\""+absoluteAppPath+"\"";
		String antTargets = "call ant clean build";	
		String pushJar = "call adb -s "+serialNo+" push "+"\""+absoluteAppPath+"\\bin\\"+appName+".jar"+"\""+" /data/local/tmp";
		createReportFolder();
		String runTestCases = "call adb -s "+serialNo+" shell uiautomator runtest "+appName+".jar -c "+packageName+" > report\\"+appName+"-TEST.txt";
		String parseToXml = "java -jar "+"\""+fileRepoPath+"\\"+retrieveJarName()+"\""+" report\\"+appName+"-TEST.txt";
		StringBuilder sb = new StringBuilder();
		String finalCommand = sb.append(buildCmd).append("\n").append(switchToWorkspace).append("\n").append(antTargets).append("\n").append(pushJar).append("\n")/*.append(createReportFolder()).append("\n")*/.append(runTestCases).append("\n").append(parseToXml).toString();
        return finalCommand+"\r\nexit %ERRORLEVEL%";
    }
	private void createReportFolder(){
		File folder = new File(appPath[0],"report"); 
		if(!folder.exists())
			folder.mkdir();
	}
	
	private String retrieveJarName() {
		String jarFile = null;
		File dir = new File(fileRepoPath);
		File[] contents = dir.listFiles();
		for(File file : contents) {
			if(file.getName().contains("uiautomator2junit"))
					jarFile = file.getName();
		}
		return jarFile;	
	}
	
    protected String getFileExtension() {
        return ".bat";
    }
	
	 public boolean perform(AbstractBuild<?,?> build, Launcher launcher, TaskListener listener) throws InterruptedException {	
		final PrintStream logger = listener.getLogger();
		boolean deleteStatus;
        EnvVars envVars = new EnvVars();
        try {
            envVars = build.getEnvironment(listener);
			} catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }			
		/*	String appName = getProjectPath().substring(getProjectPath().lastIndexOf("\\")+1).trim();
			File project = new File(envVars.get("WORKSPACE")+"\\UIAutomator\\"+appName);
			if(project.exists()){
				deleteStatus = SrcFiles.deleteAllFiles(project);
				if(deleteStatus){
					AndroidEmulator.log(logger,"Workspace cleaned");				
				} else
					AndroidEmulator.log(logger,"Workspace could not be cleaned");
			} */
			try {
				appPath = WorkspaceHandler.createWorkspace(envVars.get("WORKSPACE"), "UiAutomator",projectPath);          
				AndroidEmulator.log(logger,appPath[0]);								
			} catch(Exception e){
				AndroidEmulator.log(logger,e.toString());
			}			
			SrcFiles.copySrcFiles(projectPath,appPath[0]);
			File fileRepo = new File(envVars.get("JENKINS_HOME")+"\\File Repository");
			fileRepoPath = fileRepo.getAbsolutePath();
			targetId = envVars.get("ANDROID_TARGET_ID");		
			serialNo = envVars.get("ANDROID_SERIAL_NO");
			super.perform(build,launcher,listener);
            
		/*	File file = new File(appPath[0]);
			 if (file.isDirectory()) {
				String[] files = file.list();
                    if (files.length > 0) {
						super.perform(build,launcher,listener);
					} else {
						AndroidEmulator.log(logger,"Workspace is empty");
					}
			 }*/
			
        return true;

	 }
	 
	 @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-targetId.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.UIAUTOMATOR_DISPLAYNAME();
        }
		
		@Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {		
            return new UiAutomator(data.getString("projectPath"),data.getString("packageName"));
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

}