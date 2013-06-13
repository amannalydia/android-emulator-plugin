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

import java.io.IOException;
import java.io.PrintStream;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.CommandInterpreter;

public class ConsolidatedReport extends CommandInterpreter {
	private final boolean reportEnabled;	
	String jenkinsPath;

@DataBoundConstructor
	public ConsolidatedReport(boolean reportEnabled) {
		super(null);
		this.reportEnabled = reportEnabled;
	
	}
	
	public boolean isReportEnabled(){
		return reportEnabled;
	}
	
	public String[] buildCommandLine(FilePath script) {		
        return new String[] {"cmd","/c","call",script.getRemote()};
    }

	protected String getContents() {  
		
		if(reportEnabled) {
			String copyXmlParser = "java -jar \""+jenkinsPath+"\\File Repository\\XmlParser.jar\""+" .";
			String copyHtml = "call copy \""+jenkinsPath+"\\File Repository\\htmlbuild.xml\""+" .";
			String generateHtml = "call ant -buildfile htmlbuild.xml html-report";
			StringBuilder sb = new StringBuilder();
			String command = sb.append(copyXmlParser).append("\n").append(copyHtml).append("\n").append(generateHtml).toString();					
			return command+"\r\nexit %ERRORLEVEL%";
		}
		return "";
	
	}
	
	 protected String getFileExtension() {
        return ".bat";
    }
	
	 public boolean perform(AbstractBuild<?,?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
		final PrintStream logger = listener.getLogger();       
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
		jenkinsPath = envVars.get("JENKINS_HOME");
		AndroidEmulator.log(logger,jenkinsPath);
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
            return Functions.getResourcePath() + "/plugin/android-emulator/help-uninstallPackage.html";
        }
		
		@Override
        public String getDisplayName() {
			return Messages.CONSOLIDATED_REPORT();
        }
		
		@Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
		
			return new ConsolidatedReport(data.getBoolean("reportEnabled"));
        }
		
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }	
	}       

}