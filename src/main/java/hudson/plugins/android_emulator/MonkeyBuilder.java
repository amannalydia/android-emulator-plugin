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
import java.util.*;

import hudson.tasks.CommandInterpreter;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

public class MonkeyBuilder extends CommandInterpreter {	
	private final String packageId;	
	private String eventCount;
	private String delay;
	private String serialNo;
	
	@DataBoundConstructor
	public MonkeyBuilder(String packageId, String eventCount,String delay) {		
		super(null);
		this.packageId = packageId;				
		this.eventCount = eventCount;			
		this.delay = delay;
	}
	
	public String getPackageId() {
		return packageId;
	}
	
	public String getEventCount() {
		return eventCount;
	}	

	public String getDelay() {
		return delay;
	}

	public String[] buildCommandLine(FilePath script) {		
        return new String[] {"cmd","/c","call",script.getRemote()};
    }

	protected String getContents() {   
		//AndroidBuildWrapperSingleton obj = AndroidBuildWrapperSingleton.getSingletonInstance();
		String command = "adb -s "+serialNo+" shell monkey -p "+getPackageId()+" -s 0 -v "+getEventCount()+" --throttle "+delay+" >monkey.txt";
		return command+"\r\nexit %ERRORLEVEL%";
    }

    protected String getFileExtension() {
        return ".bat";
    }
 
	public boolean perform(AbstractBuild<?,?> build, Launcher launcher, TaskListener listener) throws InterruptedException {	
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
            return Functions.getResourcePath() + "/plugin/ATAF-plugin/help-monkeyBuilder.html";
        }
		
		@Override
        public String getDisplayName() {
      //      return Messages.SELECT_TEST_TOOL();
			return Messages.EXECUTE_MONKEY();
        }
		
		@Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
			return new MonkeyBuilder(data.getString("packageId"),data.getString("eventCount"),data.getString("delay"));
        }
		
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }	
	}       
}
