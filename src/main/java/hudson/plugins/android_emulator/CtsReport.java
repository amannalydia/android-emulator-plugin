package hudson.plugins.android_emulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CtsReport {
	public static final String REPORT = "Report";

	public static void createReport(String buildNumber,
			String sourcePath, String ctsWorkspace) {
		File ctsOrigResult;
		File ctsReport;
		File buildDir;
		File sourceDir;
		String testResultDirName = null;
		//String testResultDirPath;
		String ctsReportPath;
		String buildDirPath;
		
		//Get the testResult directory name from CTS source result directory
		ctsOrigResult = new File(sourcePath, "repository\\results");
		if(ctsOrigResult.exists()){
			String[] subDir = ctsOrigResult.list();

			for(String name : subDir)
			{
	    			if (new File(ctsOrigResult+"\\" + name).isDirectory())
	    			{
	        			System.out.println("subdir ="+name);
					testResultDirName = name;
	    			}
			}
		}
		
		//Create Report directory in CTS workspace folder inside current jenkins job.
		ctsReport = new File(ctsWorkspace, REPORT);
		if(!ctsReport.exists()){
			ctsReport.mkdir();
		}
		ctsReportPath = ctsReport.getAbsolutePath();
		
		//Create directory with BUILD_NUMBER name inside report to store testReport.xml
		buildDir = new File(ctsReportPath, buildNumber);
		if(!buildDir.exists()){
			buildDir.mkdir();
		}
		buildDirPath = buildDir.getAbsolutePath();
		
		//Copy testResult.xml from source result folder to current workspace report directory.
		if(ctsReport.exists() && buildDir.exists()){
			try {
				sourceDir = new File(ctsOrigResult,testResultDirName);
				copyFiles(sourceDir, buildDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	private static void copyFiles(File sourceLocation , File targetLocation) throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            File[] files = sourceLocation.listFiles();
            for(File file:files){
                InputStream in = new FileInputStream(file);
                OutputStream out = new FileOutputStream(targetLocation+"\\"+file.getName());

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
}
}
