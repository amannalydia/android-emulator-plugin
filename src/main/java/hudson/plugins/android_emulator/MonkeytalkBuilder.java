package hudson.plugins.android_emulator;

import hudson.EnvVars;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import javax.annotation.Nonnull;

import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import hudson.plugins.android_emulator.builder.AbstractBuilder;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.util.ValidationResult;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.WorkspaceHandler;
import hudson.plugins.android_emulator.ReadXMLFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

public class MonkeytalkBuilder extends CommandInterpreter {
	private final String packageName;
	private final String activityName;
	private final String xmlPath;
	public String adbPath;
	public String[] appFullPath;
	EnvVars envVars = new EnvVars();
	public String mtargetName;
	public String mreportDir;
	public AndroidSdk androidsdk;
	private String fileRepoPath;
	private String serialNo;

	@DataBoundConstructor
	public MonkeytalkBuilder(String command, String packageName,
			String activityName, String xmlPath) {
		super(command);
		this.packageName = packageName;
		this.activityName = activityName;
		this.xmlPath = xmlPath;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getActivityName() {
		return activityName;
	}

	public String getXmlPath() {
		return xmlPath;
	}

	public String[] buildCommandLine(FilePath script) {
		return new String[] { "cmd", "/c", "call", script.getRemote() };
	}

	protected String getContents() {
		StringBuilder sb = new StringBuilder();
		String apkName;
		String changeDir = null;
		apkName = new File(command).getName();
		// String cmd0 = "export PATH=$PATH:" + adbPath;
		String cmd1 = "adb -s "+serialNo+" uninstall " + packageName;
		String cmd2 = "adb -s "+serialNo+" install " + command;
		String cmd3 = "adb -s "+serialNo+" shell am start -n " + packageName + "/."
				+ activityName;
		String cmd4 = "sleep 6";
		if (appFullPath[0] != null) {
			changeDir = "cd " + appFullPath[0];
		}
		String cmd5 = "ant -lib " + "\"" + fileRepoPath
				+ "\\"+retrieveJarName() + "\"" + " " + mtargetName;

		String finalCommand = sb./* append(cmd0).append("\n"). */append(cmd1)
				.append("\n").append(cmd2).append("\n").append(cmd3)
				.append("\n").append(cmd4).append("\n").append(changeDir)
				.append("\n").append(cmd5).toString();
		return finalCommand + "\r\nexit %ERRORLEVEL%";
	}
	
	private String retrieveJarName() {
		String jarFile = null;
		File dir = new File(fileRepoPath);
		File[] contents = dir.listFiles();
		for(File file : contents) {
			if(file.getName().contains("monkeytalk-ant"))
					jarFile = file.getName();
		}
		return jarFile;	
	}

	protected String getFileExtension() {
		return ".bat";
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	// Get the Path of the android SDK.
	public static AndroidSdk getAndroidSdk(AbstractBuild<?, ?> build,
			Launcher launcher, BuildListener listener) throws IOException,
			InterruptedException {
		// final PrintStream logger = listener.getLogger();
		boolean shouldInstallSdk = true;
		boolean keepInWorkspace = false;
		DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(
				DescriptorImpl.class);
		if (descriptor != null) {
			shouldInstallSdk = descriptor.shouldInstallSdk;
			keepInWorkspace = descriptor.shouldKeepInWorkspace;
		}

		// Get configured, expanded Android SDK root value
		String androidHome = Utils.expandVariables(build, listener,
				Utils.getConfiguredAndroidHome());
		EnvVars envVars = Utils.getEnvironment(build, listener);

		// Retrieve actual SDK root based on given value
		Node node = Computer.currentComputer().getNode();
		String discoveredAndroidHome = Utils.discoverAndroidHome(launcher,
				node, envVars, androidHome);

		// Get Android SDK object from the given root (or locate on PATH)
		final String androidSdkHome = (envVars != null && keepInWorkspace ? envVars
				.get("WORKSPACE") : null);
		AndroidSdk androidSdk = Utils.getAndroidSdk(launcher,
				discoveredAndroidHome, androidSdkHome);
		return androidSdk;
	}

	private void copyfiles(String srcFile, String destFile) throws IOException {
		File srFile = new File(srcFile);
		File dtFile = new File(destFile);
		InputStream in = new FileInputStream(srFile);
		OutputStream out = new FileOutputStream(dtFile + "\\"
				+ srFile.getName());
		// Copy the bits from input stream to output stream
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private void copyDirectory(File sourceLocation, File targetLocation)
			throws IOException {

		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists()) {
				targetLocation.mkdir();
			}
			String[] children = sourceLocation.list();
			for (int i = 0; i < children.length; i++) {
				copyDirectory(new File(sourceLocation, children[i]), new File(
						targetLocation, children[i]));
			}
		} else {
			String filename = targetLocation.getName();

			InputStream in = new FileInputStream(sourceLocation);
			OutputStream out = new FileOutputStream(targetLocation);
			if (filename.contains(".xml")) {
				String appName = filename.substring(5, (filename.length() - 4));
				out = new FileOutputStream(new File(targetLocation.getParent()
						+ "\\", appName.concat("-TEST.xml")));
			}

			// Copy the bits from input stream to output stream
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

			in.close();
			out.close();

		}
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {
		final PrintStream logger = listener.getLogger();
		boolean deleteStatus;
		FilePath ws = build.getWorkspace();
		FilePath script = null;
		try {
			envVars = build.getEnvironment(listener);
		} catch (IOException e1) {
			e1.printStackTrace(listener.fatalError(Messages
					.CommandInterpreter_UnableToGetEnvironment()));
		} catch (InterruptedException e1) {
			e1.printStackTrace(listener.fatalError(Messages
					.CommandInterpreter_UnableToGetEnvironment()));
		}		
	/*	String appName = command.substring(command.lastIndexOf("\\")+1).trim();
		File project = new File(envVars.get("WORKSPACE")+"\\MonkeyTalk\\"+appName);
		if(project.exists()){
			deleteStatus = SrcFiles.deleteAllFiles(project);
			if(deleteStatus){
				AndroidEmulator.log(logger,"Workspace cleaned");				
			} else
				AndroidEmulator.log(logger,"Workspace could not be cleaned");
		} */
		try {			
			appFullPath = WorkspaceHandler.createWorkspace(
					envVars.get("WORKSPACE"), "monkeytalk", command);
			// AndroidEmulator.log(logger,appFullPath[0]);
		} catch (Exception e) {
			AndroidEmulator.log(logger, e.toString());
		}
		serialNo = envVars.get("ANDROID_SERIAL_NO");
		File fileRepo = new File(envVars.get("JENKINS_HOME")+"\\File Repository");
		fileRepoPath = fileRepo.getAbsolutePath();
		// if(appFullPath!=null) AndroidEmulator.log(logger,appFullPath[0]);
		/*
		 * try { androidsdk = getAndroidSdk(build, launcher, listener); // Get
		 * the ADB path adbPath =
		 * androidsdk.getSdkRoot().concat("\\platform-tools"); } catch
		 * (IOException e) { e.printStackTrace(listener.fatalError(Messages
		 * .CommandInterpreter_UnableToGetAdbPath())); }
		 */
		// Copy build.xml to Jenkins workspace
		if (xmlPath != null) {
			try {
				copyfiles(xmlPath, appFullPath[0]);
			} catch (IOException e) {
				// AndroidEmulator.log(logger, "in exception");
				AndroidEmulator.log(logger, e.toString());
			}
		}

		// Parse build.xml file to get target name and report directory.
		ReadXMLFile xmlObj = new ReadXMLFile();
		if (xmlObj != null) {
			xmlObj.ReadXML(xmlPath);
			mtargetName = xmlObj.getTargetName();
			mreportDir = xmlObj.getReportDir();
		}
		try {
			try {
				script = createScriptFile(ws);
			} catch (IOException e) {
				Util.displayIOException(e, listener);
				e.printStackTrace(listener.fatalError(Messages
						.CommandInterpreter_UnableToProduceScript()));
				return false;
			}
			int r;
			try {
				// on Windows environment variables are converted to all upper
				// case,
				// but no such conversions are done on Unix, so to make this
				// cross-platform,
				// convert variables to all upper cases.
				for (Map.Entry<String, String> e : build.getBuildVariables()
						.entrySet())
					envVars.put(e.getKey(), e.getValue());

				r = launcher.launch().cmds(buildCommandLine(script))
						.envs(envVars).stdout(listener).pwd(ws).join();
			} catch (IOException e) {
				Util.displayIOException(e, listener);
				e.printStackTrace(listener.fatalError(Messages
						.CommandInterpreter_CommandFailed()));
				r = -1;
			}
			return r == 0;
		} finally {
			try {
				if (script != null)
					script.delete();

				// copy testReport to current workspace.
				try {
					File SourceLocation = new File(mreportDir);
					File TargetLocation = new File(appFullPath[0], "report");
					copyDirectory(SourceLocation, TargetLocation);
				} catch (IOException e) {
					AndroidEmulator.log(logger, e.toString());
				}

			} catch (IOException e) {
				Util.displayIOException(e, listener);
				e.printStackTrace(listener.fatalError(Messages
						.CommandInterpreter_UnableToDelete(script)));
			} catch (Exception e) {
				e.printStackTrace(listener.fatalError(Messages
						.CommandInterpreter_UnableToDelete(script)));
			}
		}

	}

	@Override
	public FilePath createScriptFile(@Nonnull FilePath dir) throws IOException,
			InterruptedException {
		return dir.createTextTempFile("hudson", getFileExtension(),
				getContents(), false);
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		// private String shell;
		public boolean shouldInstallSdk;
		public boolean shouldKeepInWorkspace;

		public DescriptorImpl() {
			load();
		}

		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		public String getDisplayName() {
			return Messages.RUN_MONKEYTALK();
		}

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject data) {
			return new MonkeytalkBuilder(data.getString("command"),
					data.getString("packageName"),
					data.getString("activityName"), data.getString("xmlPath"));
		}

		/*
		 * @Override public String getHelpFile() { return
		 * Functions.getResourcePath() +
		 * "/plugin/android-emulator/help-ctsSourcepath.html"; }
		 */

		/**
		 * Check the existence of sh in the given location.
		 */
		public FormValidation doCheck(@QueryParameter String value) {
			// Executable requires admin permission
			return FormValidation.validateExecutable(value);
		}

		private static final class Shellinterpreter implements
				Callable<String, IOException> {

			private static final long serialVersionUID = 1L;

			public String call() throws IOException {
				return Functions.isWindows() ? "sh" : "/bin/sh";
			}
		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(MonkeytalkBuilder.class.getName());

}
