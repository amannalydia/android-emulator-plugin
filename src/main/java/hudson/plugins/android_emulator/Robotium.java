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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import hudson.tasks.CommandInterpreter;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

public class Robotium extends CommandInterpreter {
	private final String packageName;		
	private final String targetAppPath;
	private final String testAppPath;
	private String appPath[];
	private String targetId;
	private String serialNo;

	@DataBoundConstructor
	public Robotium(String targetAppPath, String testAppPath, String packageName) {		
		super(null);
		this.targetAppPath = targetAppPath;	
		this.testAppPath = testAppPath;			
		this.packageName = packageName;					
	}

	public String getTargetAppPath() {
		return targetAppPath;
	}
	
	public String getTestAppPath() {
		return testAppPath;
	}
	
	public String getPackageName() {
		return packageName;
	}	

	public String[] buildCommandLine(FilePath script) {		
        return new String[] {"cmd","/c","call",script.getRemote()};
    }

	protected String getContents() {   
		String absoluteAppPath = appPath[0];
		String targetPath = appPath[1];
		String appName = targetPath.substring(targetPath.lastIndexOf("\\")+1).trim();		
		String targetBuildCmd = "call android update project -n "+appName+" -p "+"\""+targetPath+"\""+" -t "+targetId;	
		String testBuildCmd = "call android update test-project -p "+"\""+absoluteAppPath+"\""+" -m "+"\""+targetPath+"\"";
		String switchToWorkspace = "cd "+"\""+absoluteAppPath+"\"";
		String antTargets = "call ant -Dtest.runner=pl.polidea.instrumentation.PolideaInstrumentationTestRunner -Dadb.device.arg="+"\"-s "+serialNo+"\" clean debug uninstall install";	
		String runTestCases = "call adb -s "+serialNo+" shell am instrument -w -e junitOutputDirectory /mnt/sdcard/ "+getPackageName()+"/pl.polidea.instrumentation.PolideaInstrumentationTestRunner";
		String fetchReport = "call adb -s "+serialNo+" pull /mnt/sdcard/"+getPackageName()+"-TEST.xml report/"+appName+"-TEST.xml";
		StringBuilder sb = new StringBuilder();
		String finalCommand = sb.append(targetBuildCmd).append("\n").append(testBuildCmd).append("\n").append(switchToWorkspace).append("\n").append(antTargets).append("\n").append(runTestCases).append("\n").append(fetchReport).toString();
		return finalCommand+"\r\nexit %ERRORLEVEL%";
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
		
		try {
			appPath = WorkspaceHandler.createWorkspace(envVars.get("WORKSPACE"), "Robotium",targetAppPath);          
			AndroidEmulator.log(logger,appPath[0]);
			} catch(Exception e){
				AndroidEmulator.log(logger,e.toString());
			}	    
		SrcFiles.copySrcFiles(testAppPath,appPath[0],envVars.get("JENKINS_HOME")+"\\File Repository");
		SrcFiles.copySrcFiles(targetAppPath,appPath[1]);
		targetId = envVars.get("ANDROID_TARGET_ID");
		serialNo = envVars.get("ANDROID_SERIAL_NO");
		super.perform(build,launcher,listener);
        return true;
	 }

	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	 @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>  {

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/ATAF-plugin/help-robotium.html";
        }
		
		@Override
        public String getDisplayName() {
			return Messages.EXECUTE_ROBOTIUM();
        }
		
		@Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
			return new Robotium(data.getString("targetAppPath"),data.getString("testAppPath"),data.getString("packageName"));
        }
		
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }	
	}       
}
