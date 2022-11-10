package datemanager;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;


/*
 * Inspired by:
 * https://github.com/JacksonBrienen/VBS-Shortcut
 *
 */

public class ShortcutFactory {
	
	public static void createShortcut(Path sourceFile, Path linkPath) throws FileNotFoundException {
		sourceFile = sourceFile.toAbsolutePath();
		if(!Files.isRegularFile(sourceFile)) {
			throw new FileNotFoundException("Can't create shortcut to non-existing file: "+sourceFile);
		}
		try {
			
			String vbsCode = String.format(
				  "Set wsObj = WScript.CreateObject(\"WScript.shell\")%n"
				+ "scPath = \"%s\"%n"
				+ "Set scObj = wsObj.CreateShortcut(scPath)%n"
				+ "\tscObj.TargetPath = \"%s\"%n"
				+ "\tscObj.Arguments = \"--hidden\"%n"
				+ "\tscObj.WindowStyle = 7%n"	//minimized prop, doesn't work
				+ "scObj.Save%n",
				linkPath, sourceFile
				);
		
			newVBS(vbsCode);
		} catch (IOException | InterruptedException e) {
			System.err.println("Could not create and run VBS!");
			e.printStackTrace();
		} 
	}
	
	private static void newVBS(String code) throws IOException, InterruptedException {
		File script = File.createTempFile("scvbs", ".vbs");
		
		FileWriter writer = new FileWriter(script);
		writer.write(code);
		writer.close();
		
		var command = new String[]{"wscript", script.getAbsolutePath().toString()};
		System.out.println(Arrays.toString(command));
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
		if(!script.delete()) {
			System.err.println("Failed to delete tempory VBS file: "+script.getAbsolutePath());
		}
	}
}