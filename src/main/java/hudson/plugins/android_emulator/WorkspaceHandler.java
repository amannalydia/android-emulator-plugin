package hudson.plugins.android_emulator;

import java.io.File;
import java.io.PrintStream;


public class WorkspaceHandler {
	public static final String ROBOTIUM = "Robotium";
	public static final String UIAUTOMATOR = "UIAutomator";
	public static final String MONKEY = "Monkey";
	public static final String MONKEYTALK = "MonkeyTalk";
	public static final String CTS = "CTS";
       
	//ToDO --remove from final code 
	//public static PrintStream logger;
	/**
	 * Creates directory for the tools in the current job.
	 * @param workSpace Current job path.
	 * @param toolName Name of the tool for which directory has to be created.
	 * @param appPath Absolute path of the application.
	 * @return Absolute path of the application folder in current job.
	 */
	public static String[] createWorkspace(String workSpace, String toolName, String appPath) {

		File toolDir = null;
		String appDirFullPath[] = null;
	
		// create folder for 'robotium'
		if(toolName.equalsIgnoreCase("robotium")) {
			toolDir = new File(workSpace, ROBOTIUM);
			if(!toolDir.exists()){
				toolDir.mkdir();
			}
			if(toolDir != null){
				//if(logger!=null) logger.println("before createsubdir()");
				appDirFullPath = createSubDir(toolDir, appPath);
			}
		}
		
		// create folder for uiautomator
		else if(toolName.equalsIgnoreCase("uiautomator")) {
			toolDir = new File(workSpace, UIAUTOMATOR);
			if(!toolDir.exists()){
				toolDir.mkdir();
			}
			if(toolDir != null){
				appDirFullPath = createSubDir(toolDir, appPath);
			}
		}
		
		//create folder for monkey
		else if (toolName.equalsIgnoreCase("monkey")) {
			/** No need to create any folder for monkey */
			return null;
		}
		
		//create folder for monkeytalk
		else if (toolName.equalsIgnoreCase("monkeytalk")) {
			toolDir = new File(workSpace, MONKEYTALK);
			if(!toolDir.exists()){
				toolDir.mkdir();
			}
			if(toolDir != null){
				appDirFullPath = createSubDir(toolDir, appPath);
			}
		}
		
		//create folder for CTS
		else if (toolName.equalsIgnoreCase("cts")) {
			toolDir = new File(workSpace, CTS);
			if(!toolDir.exists()){
				toolDir.mkdir();
				//appDirFullPath = createSubDir(toolDir, appPath);
			}
			appDirFullPath = new String[]{toolDir.getAbsolutePath(),""};
		}
		return appDirFullPath;
	}
	
	/**
	 * Creates directory for application inside respective tool folder.
	 * @param Parent File path of the tool folder.
	 * @param pAppPath Absolute Path of the application.
	 * @return Absolute path of the application folder in the current job. 
	 */
	private static String[] createSubDir(File Parent, String pAppPath) {
		File appDir;
		File appSubDir;
		/** Application name without extension. */
		String appName = null; 
		String appNameWithExt;
		String appFullPath;
		String appSubDirFullPath;
		appNameWithExt = new File(pAppPath).getName();
		//if(logger!=null) logger.println("in createSubDir()");
		//Get the application name without extension
		if(appNameWithExt.lastIndexOf('.') > 0) {
			appName = appNameWithExt.substring(0, appNameWithExt.lastIndexOf('.'));
		}else {
			appName = new File(pAppPath).getName();
		}
		if(Parent.getName().equalsIgnoreCase("robotium"))
			appDir = new File(Parent, appName+"Test");
		else 
			appDir = new File(Parent, appName);
		if (!appDir.exists()){
			appDir.mkdir();
		}
		
		appFullPath = appDir.getAbsolutePath();
		//if(logger!=null) logger.println("Parent name = "+Parent.getName());
		if(Parent.getName().equalsIgnoreCase("robotium")){
			appSubDir = new File(appDir, appName);
			if (!appSubDir.exists()){
				appSubDir.mkdir();
			}
			appSubDirFullPath = appSubDir.getAbsolutePath();
			//if(logger!=null) logger.println("appSubDirFullPath = "+appSubDirFullPath);
		}else{
			appSubDirFullPath = null;
		}
		return new String[] {appFullPath , appSubDirFullPath};
	}
}
