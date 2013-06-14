package hudson.plugins.android_emulator;

import hudson.Extension;
import hudson.Functions;
import hudson.EnvVars;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.Launcher;
import hudson.plugins.android_emulator.util.Utils;

import java.io.PrintStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.io.File;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class AndroidBuildWrapper extends BuildWrapper implements Serializable{
	private final String targetId;
	private final String serialNo;
	private final String libsPath;

	@DataBoundConstructor
	public AndroidBuildWrapper(String targetId, String serialNo, String libsPath) {
		this.targetId = targetId;
		this.serialNo = serialNo;
		this.libsPath = libsPath;
	}

	public String getTargetId() {
		return targetId;
	}
	
	public String getSerialNo() {
		return serialNo;
	}

	public String getLibsPath() {
		return libsPath;
	}

	@Override
	public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
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
			deleteStatus = SrcFiles.deleteAllFiles(new File(envVars.get("WORKSPACE")));
			if(deleteStatus){
				AndroidEmulator.log(logger,"Workspace cleaned");				
			} else
				AndroidEmulator.log(logger,"Workspace could not be cleaned");
			String fileRepoPath = CreateFileRepo.createDir(envVars.get("JENKINS_HOME"));
			SrcFiles.copyContents(new File(getLibsPath()),new File(fileRepoPath));
			return new Environment() {
				@Override
				public void buildEnvVars(Map<String, String> env) {
					env.put("ANDROID_TARGET_ID", getTargetId());
					env.put("ANDROID_SERIAL_NO", getSerialNo());
				}
			};	
	}
	
	@Extension(ordinal=-100)
	 public static final class DescriptorImpl extends BuildWrapperDescriptor implements Serializable {
	
		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return Messages.INVOKE_ANDROID_BUILD_WRAPPER();
		}
		
		@Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			return new AndroidBuildWrapper(formData.getString("targetId"),formData.getString("serialNo"),formData.getString("libsPath"));			
		}
		
		 @Override
	        public String getHelpFile() {
	            return Functions.getResourcePath() + "/plugin/ATAF-plugin/help-androidBuildWrapper.html";
	        }
		 
		 @Override
			public boolean isApplicable(AbstractProject<?, ?> item) {
				// TODO Auto-generated method stub
				return true;
			}			
			
	}
}
