package hudson.plugins.android_emulator;

import java.io.File;

public class CreateFileRepo {
	public static String createDir(String location) {
		File fileRepo = new File(location,"File Repository");
		if(!fileRepo.exists())
			fileRepo.mkdir();
		String dirPath = fileRepo.getAbsolutePath();
		return dirPath;
	}
}