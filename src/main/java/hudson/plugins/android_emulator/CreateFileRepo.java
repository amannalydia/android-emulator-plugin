package hudson.plugins.android_emulator;

import java.io.File;

public class CreateFileRepo {
	public static String createDir(String location) {
		File setupDir = new File(location,"File Repository");
		if(!setupDir.exists())
			setupDir.mkdir();
		String dirPath = setupDir.getAbsolutePath();
		return dirPath;
	}
}