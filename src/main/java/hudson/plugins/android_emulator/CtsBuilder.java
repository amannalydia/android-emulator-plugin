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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import hudson.util.FormValidation;

import javax.annotation.Nonnull;

import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import hudson.plugins.android_emulator.builder.AbstractBuilder;
import hudson.plugins.android_emulator.AndroidEmulator.DescriptorImpl;
import hudson.plugins.android_emulator.util.Utils;
import hudson.plugins.android_emulator.util.ValidationResult;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.WorkspaceHandler;
import hudson.plugins.android_emulator.CtsReport;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

public class CtsBuilder extends CommandInterpreter {
	private final String testPlanName;
	public AndroidSdk androidsdk;
	public static PrintStream logger;
	public String adbPath;
	public String[] appFullPath;
	private String serialNo;

	@DataBoundConstructor
	public CtsBuilder(String command, String testPlanName) {
		super(command);
		this.testPlanName = testPlanName;
	}

	public String getTestPlanName() {
		return testPlanName;
	}

	public String[] buildCommandLine(FilePath script) {
		return new String[] { "cmd", "/c", "call", script.getRemote() };
	}

	protected String getContents() {
		StringBuilder sb = new StringBuilder();
		String cmd5;
		//AndroidBuildWrapperSingleton obj = AndroidBuildWrapperSingleton.getSingletonInstance();		
		String device = serialNo;
		String ctsResultPath = command.concat("\\repository\\results");
		String ctsTestcaseAS = command
				.concat("\\repository\\testcases\\CtsDelegatingAccessibilityService.apk");
		String ctsTestcaseDA = command
				.concat("\\repository\\testcases\\CtsDeviceAdmin.apk");
		String ctsToolPath = command.concat("\\tools\\cts-tradefed.bat");
		// this.adbPath = adbPath;
		// String adbPath = androidsdk.getSdkRoot().concat("platform-tools");
		// if (logger != null)logger.println(adbPath);

		String cmd1 = "rm -rf " + ctsResultPath + "\\*";
		// String cmd2 = "export PATH=$PATH:" + adbPath;
		String cmd3 = " adb -s "+serialNo+" install " + ctsTestcaseAS;
		String cmd4 = " adb -s "+serialNo+" install " + ctsTestcaseDA;
		// Check for Emulator or Device
		if (device.toLowerCase().contains("emulator")) {
			cmd5 = " run cts --disable-reboot --plan "; 
		} else {
			cmd5 = " run cts --plan ";
		}

		String finalCommand = sb.append(cmd1).append("\n")/* .append(cmd2) */
		.append("\n").append(cmd3).append("\n").append(cmd4).append("\n")
				.append(ctsToolPath).append(cmd5).append(testPlanName)
				.toString();
		return finalCommand + "\r\nexit %ERRORLEVEL%";
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

	private void writeFile(String sourcePath) {
		String content = "java -cp {0}ddmlib-prebuilt.jar;{1}tradefed-prebuilt.jar;{2}hosttestlib.jar;{3}cts-tradefed.jar -DCTS_ROOT={4} com.android.cts.tradefed.command.CtsConsole %*";
		String result = MessageFormat.format(content, sourcePath, sourcePath,
				sourcePath, sourcePath, command);
		try {
			File file = new File(sourcePath, "cts-transfed.bat");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(result);
			bw.close();

			System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {
		final PrintStream logger = listener.getLogger();
	boolean deleteStatus;
		FilePath ws = build.getWorkspace();
		FilePath script = null;
		EnvVars envVars = new EnvVars();
		try {
			envVars = build.getEnvironment(listener);
		} catch (IOException e1) {
			e1.printStackTrace(listener.fatalError(Messages
					.CommandInterpreter_UnableToGetEnvironment()));
		} catch (InterruptedException e1) {
			e1.printStackTrace(listener.fatalError(Messages
					.CommandInterpreter_UnableToGetEnvironment()));
		}
	/*	File project = new File(envVars.get("WORKSPACE")+"\\CTS");
		if(project.exists()){
			deleteStatus = SrcFiles.deleteAllFiles(project);
			if(deleteStatus){
				AndroidEmulator.log(logger,"Workspace cleaned");				
			} else
				AndroidEmulator.log(logger,"Workspace could not be cleaned");
		} 	*/
		// Create workspace for CTS inside current job.
		try {
			appFullPath = WorkspaceHandler.createWorkspace(
					envVars.get("WORKSPACE"), "CTS", "");
			// AndroidEmulator.log(logger,appFullPath[0]);
		} catch (Exception e) {
			AndroidEmulator.log(logger, e.toString());
		}		    		
		// Create cts-transfed.bat file for Windows
		writeFile(command.concat("\\tools\\"));		
		serialNo = envVars.get("ANDROID_SERIAL_NO");
		// if(appFullPath!=null) AndroidEmulator.log(logger,appFullPath[0]);
		/*
		 * try { androidsdk = getAndroidSdk(build, launcher, listener); // Get
		 * the ADB path adbPath =
		 * androidsdk.getSdkRoot().concat("\\platform-tools"); } catch
		 * (IOException e) { e.printStackTrace(listener.fatalError(Messages
		 * .CommandInterpreter_UnableToGetAdbPath())); }
		 */
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
				if (appFullPath != null) {
					// AndroidEmulator.log(logger, "before report");
					CtsReport.createReport(envVars.get("BUILD_NUMBER"),
							command, appFullPath[0]);
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

		public boolean shouldInstallSdk;
		public boolean shouldKeepInWorkspace;

		/*
		 * public DescriptorImpl() { load(); }
		 */

		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		/*
		 * public String getShell() { return shell; }
		 */

		/**
		 * @deprecated 1.403 Use
		 *             {@link #getShellOrDefault(hudson.remoting.VirtualChannel) }
		 *             .
		 */
		/*
		 * public String getShellOrDefault() { if (shell == null) return
		 * Functions.isWindows() ? "sh" : "/bin/sh"; return shell; }
		 * 
		 * public String getShellOrDefault(VirtualChannel channel) { if (shell
		 * != null) return shell;
		 * 
		 * String interpreter = null; try { interpreter = channel.call(new
		 * Shellinterpreter()); } catch (IOException e) {
		 * LOGGER.warning(e.getMessage()); } catch (InterruptedException e) {
		 * LOGGER.warning(e.getMessage()); } if (interpreter == null) {
		 * interpreter = getShellOrDefault(); }
		 * 
		 * return interpreter; }
		 * 
		 * public void setShell(String shell) { this.shell =
		 * Util.fixEmptyAndTrim(shell); save(); }
		 */

		public String getDisplayName() {
			return Messages.RUN_CTS();
		}

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject data) {
			return new CtsBuilder(data.getString("command"),
					data.getString("testPlanName"));
		}

		/*
		 * @Override public boolean configure(StaplerRequest req, JSONObject
		 * data) { setShell(req.getParameter("shell")); return true; }
		 */

		@Override
		public String getHelpFile() {
			return Functions.getResourcePath()
					+ "/plugin/android-emulator/help-ctsSourcepath.html";
		}

		/**
		 * Check the existence of sh in the given location.
		 */
		public FormValidation doCheck(@QueryParameter String value) {
			// Executable requires admin permission
			return FormValidation.validateExecutable(value);
		}

		/**
		 * Check the path of the CTS source code.
		 * 
		 * @param command
		 *            Path entered by the User.
		 * @return Whether the CTS path is correct or not.
		 */
		public FormValidation doCheckCommand(@QueryParameter String command) {
			final String[] sdkDirectories = { "repository", "tools" };
			for (String dirName : sdkDirectories) {
				File dir = new File(command, dirName);
				if (logger != null)
					logger.println(dir);
				if (!dir.exists() || !dir.isDirectory()) {
					return ValidationResult.error(
							Messages.INVALID_CTS_DIRECTORY())
							.getFormValidation();
				}
			}
			return ValidationResult.ok().getFormValidation();
		}

		/*
		 * private static final class Shellinterpreter implements
		 * Callable<String, IOException> {
		 * 
		 * private static final long serialVersionUID = 1L;
		 * 
		 * public String call() throws IOException { return
		 * Functions.isWindows() ? "sh" : "/bin/sh"; } }
		 */

	}

	// private static final Logger LOGGER =
	// Logger.getLogger(CtsBuilder.class.getName());

}
