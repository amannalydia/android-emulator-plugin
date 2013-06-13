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

//	private RobotiumAppEntries[] appEntries;
	
	@DataBoundConstructor
	public Robotium(String targetAppPath, String testAppPath, String packageName) {		
		super(null);
	//	this.targetApp = (robotium != null) ? robotium.targetApp : null;		
		this.targetAppPath = targetAppPath;	
		this.testAppPath = testAppPath;			
		this.packageName = packageName;			
		
	}
	
/*	@DataBoundConstructor
	public Robotium(RobotiumAppEntries[] appEntries) {		
		super(null);
		this.appEntries = appEntries;
	}
	
/*	public static class OptionalTextBlock {
        final String targetApp;
	
        @DataBoundConstructor
        public OptionalTextBlock(String targetApp) {
			this.targetApp = targetApp;
        }
    }
*/	
	public String getTargetAppPath() {
		return targetAppPath;
	}
	
	public String getTestAppPath() {
		return testAppPath;
	}
	
	public String getPackageName() {
		return packageName;
	}	

/*	public RobotiumAppEntries[] getAppEntries() {
        return appEntries;
    }

    public void setAppEntries(RobotiumAppEntries[] appEntries) {
        this.appEntries = appEntries;
    }
*/
	public String[] buildCommandLine(FilePath script) {		
        return new String[] {"cmd","/c","call",script.getRemote()};
    }

	protected String getContents() {   
	//	SetTarget buildWrapperValues = new SetTarget();
		String absoluteAppPath = appPath[0];
		String targetPath = appPath[1];
		String appName = targetPath.substring(targetPath.lastIndexOf("\\")+1).trim();
		//AndroidBuildWrapperSingleton obj = AndroidBuildWrapperSingleton.getSingletonInstance();
		
		String targetBuildCmd = "call android update project -n "+appName+" -p "+"\""+targetPath+"\""+" -t "+targetId;	
		//String targetBuildCmd = "call android update project -n "+otb.getTargetApp()+" -p "+"\""+getProjectPath()+"\""+" -t "+buildWrapperValues.getTargetId();		
		String testBuildCmd = "call android update test-project -p "+"\""+absoluteAppPath+"\""+" -m "+"\""+targetPath+"\"";
		String switchToWorkspace = "cd "+"\""+absoluteAppPath+"\"";
		String antTargets = "call ant -Dtest.runner=pl.polidea.instrumentation.PolideaInstrumentationTestRunner -Dadb.device.arg="+"\"-s "+serialNo+"\" clean debug uninstall install";	
	//	String targetApkUninstall = "call adb -s "+serialNo+" uninstall "+appName+"-debug.apk";
	//	String testtApkUninstall = "call adb -s "+serialNo+" uninstall "+appName+"Test-debug.apk";
	//	String targetApkInstall = "call adb -s "+serialNo+" install "+targetPath+"\\bin\\"+appName+"-debug.apk";
	//	String testApkInstall = "call adb -s "+serialNo+" install "+absoluteAppPath+"\\bin\\"+appName+"Test-debug.apk";
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
		
	/*	String appName = getTargetAppPath().substring(getTargetAppPath().lastIndexOf("\\")+1).trim();
		AndroidEmulator.log(logger,appName);
		File project = new File(envVars.get("WORKSPACE")+"\\Robotium\\"+appName+"Test");
		AndroidEmulator.log(logger,project.getAbsolutePath());
		if(project.exists()){
			deleteStatus = SrcFiles.deleteAllFiles(project);
			if(deleteStatus){
				AndroidEmulator.log(logger,"Workspace cleaned");				
			} else
				AndroidEmulator.log(logger,"Workspace could not be cleaned");
		} */
		try {
			appPath = WorkspaceHandler.createWorkspace(envVars.get("WORKSPACE"), "Robotium",targetAppPath);          
			AndroidEmulator.log(logger,appPath[0]);
			//AndroidEmulator.log(logger,appPath[1]);
			} catch(Exception e){
				AndroidEmulator.log(logger,e.toString());
			}	    
		SrcFiles.copySrcFiles(testAppPath,appPath[0],envVars.get("JENKINS_HOME")+"\\File Repository");
		SrcFiles.copySrcFiles(targetAppPath,appPath[1]);
		targetId = envVars.get("ANDROID_TARGET_ID");
		//AndroidEmulator.log(logger,targetId);
		serialNo = envVars.get("ANDROID_SERIAL_NO");
		//AndroidEmulator.log(logger,serialNo);
		super.perform(build,launcher,listener);
			
		/*	File file = new File(appPath[0]);
			String appName = appPath[1].substring(appPath[1].lastIndexOf("\\")+1).trim();
			if (file.isDirectory()) {
				File[] files = file.listFiles();				
				if ((files.length == 1)&&(files[0].getName().equals(appName)))		
					AndroidEmulator.log(logger,"Workspace is empty! Test Cases not present.");					
				else 
					super.perform(build,launcher,listener);							
			}*/
        return true;

	 }

	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	 @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>  {

		public DescriptorImpl() {			
			load();
		}
		
		@Override
        public boolean configure(StaplerRequest req, JSONObject formData) {			
			save();
            return true;
        }
		
        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/android-emulator/help-uninstallPackage.html";
        }
		
		@Override
        public String getDisplayName() {
      //      return Messages.SELECT_TEST_TOOL();
			return Messages.EXECUTE_ROBOTIUM();
        }
		
		@Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
		/*	Robotium robotiumBuilder = new Robotium();
			List<RobotiumAppEntries> appEntries = req.bindParametersToList(RobotiumAppEntries.class, "RobotiumAppEntries.");
            robotiumBuilder.setAppEntries(appEntries.toArray(new RobotiumAppEntries[appEntries.size()]));
			return robotiumBuilder;*/
			return new Robotium(data.getString("targetAppPath"),data.getString("testAppPath"),data.getString("packageName"));
        }
		
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }	
	}       
}
