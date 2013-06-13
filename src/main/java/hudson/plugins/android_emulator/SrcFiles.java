package hudson.plugins.android_emulator;

import java.io.PrintStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class SrcFiles {
	
	public static void copySrcFiles(String srcPath, String destPath) {
		File srcDir = new File(srcPath);
		File source = null;
		File destination = null;
		File[] subDir = srcDir.listFiles();
		String[] filesToCopy = {"src", "res","libs",".classpath","AndroidManifest.xml"};	
		for (File file : subDir) {		
			for(int i=0;i<filesToCopy.length;i++){			
				if(file.getName().equals(filesToCopy[i])){									
					source = new File(file.getAbsolutePath());
					destination = new File(destPath,file.getName());
					try {
						copyContents(source,destination);		
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
			}
		}
	 }
	 
	 public static void copySrcFiles(String srcPath, String destPath, String fileRepoPath) {
		File srcDir = new File(srcPath);
		File source = null;
		File destination = null;
		File newDir = new File(destPath,"libs");
		File[] subDir = srcDir.listFiles();
	//	String[] fileNames = {"robotium-solo", "polidea_test_runner"};
		for (File file : subDir) {
			File folder = new File(srcPath,"libs");
			if(!folder.exists()){
				newDir.mkdir();
				copyRobotiumLibs(fileRepoPath,newDir.getAbsolutePath());
			/*	File fileRepo = new File(fileRepoPath);			
				File[] repoContents = fileRepo.listFiles();
				for (File jarFiles : repoContents) {
					for(int i=0;i<fileNames.length;i++){			
						if(jarFiles.getName().contains(fileNames[i])){
							source = new File(jarFiles.getAbsolutePath());
							destination = new File(newDir.getAbsolutePath(),jarFiles.getName());
							try {
								copyContents(source,destination);		
							} catch (IOException e) {
								// TODO Auto-generated catch block
							e.printStackTrace();
							}
						}
					}
				}*/
			}
		}
		String[] filesToCopy = {"src", "res","libs",".classpath","AndroidManifest.xml"};	
		for (File file : subDir) {		
			for(int i=0;i<filesToCopy.length;i++){			
				if(file.getName().equals(filesToCopy[i])){									
					source = new File(file.getAbsolutePath());
					destination = new File(destPath,file.getName());
					try {
						copyContents(source,destination);		
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
			}
		}
		if(newDir.exists()) {
			File[] libsDir = newDir.listFiles();
			if(libsDir.length == 0) 
				copyRobotiumLibs(fileRepoPath, newDir.getAbsolutePath());
			else {
				String[] fileNames = {"robotium-solo", "polidea_test_runner"};
				for(File jars : libsDir) {
					for(int i=0;i<fileNames.length;i++){	
						if(!jars.getName().contains(fileNames[i]))
							copyRobotiumLibs(fileRepoPath, newDir.getAbsolutePath());										
					}				
				}
			}		
		}
	 }
	 private static void copyRobotiumLibs(String fileRepoPath,String finalPath) {
		File source = null;
		File destination = null;
		String[] fileNames = {"robotium-solo", "polidea_test_runner"};
		File fileRepo = new File(fileRepoPath);			
		File[] repoContents = fileRepo.listFiles();
		for (File jarFiles : repoContents) {
			for(int i=0;i<fileNames.length;i++){			
				if(jarFiles.getName().contains(fileNames[i])){
					source = new File(jarFiles.getAbsolutePath());
					destination = new File(finalPath,jarFiles.getName());
					try {
						copyContents(source,destination);		
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	 }
	 private static void copyContents(File source, File destination)
		    throws IOException {
	
			if (source.isDirectory()) {
		        if (!destination.exists()) 
		            destination.mkdir();		            
		        String[] subFolders = source.list();
		        for (String fileName : subFolders) {
					File srcFile = new File(source, fileName);
					File destFile = new File(destination, fileName);   		   
					copyContents(srcFile,destFile);
		        }
		    } else {
		        InputStream in = new FileInputStream(source);
		        OutputStream out = new FileOutputStream(destination);
		        byte[] buf = new byte[1024];
		        int len;
		        while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
		        }
		        in.close();
		        out.close();
		    }
	}
	
	public static boolean deleteAllFiles(File path) {
		boolean status = false;
		if(path.exists()){
			File[] files = path.listFiles();
			for (File file : files){
				if (file.isDirectory()){
					status = deleteAllFiles(file);
					status = file.delete();
				} else 
					status = file.delete();
			}				
		}
		return status;
	}
}