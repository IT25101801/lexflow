import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// java database/RestoreDemoState.java --apply

public class RestoreDemoState {
	
	public static void main(String[] args) throws Exception {
		boolean apply = false;
		boolean filesOnly = false;
		
		for (String arg : args) {
			
			if (arg.equals("--apply")) {
				apply = true;
			} else if (arg.equals("--files-only")) {
				filesOnly = true;
			} else {
				fail("Unknown option: " + arg);
			}
			
		}
		
		Path root = Path.of("").toAbsolutePath();
		Path properties = root.resolve("src/main/resources/application.properties");
		
		if (!Files.exists(root.resolve("pom.xml")) || !Files.exists(root.resolve("database/full_reset.sql")) || !Files.exists(properties)) {
			fail("Run this from the project's root folder (the one with pom.xml).");
		}
		
		int tracked = run(true, "git", "ls-files", "uploads").lines().toList().size();
		
		if (tracked < 200) {
			fail("Only " + tracked + " files in uploads/ are committed to git, so the seeded files are not committed here and would be deleted. Nothing was changed.");
		}
		
		List<String> restore = new ArrayList<>(run(true, "git", "status", "--porcelain", "--untracked-files=no", "--", "uploads").lines().toList());
		List<String> delete = new ArrayList<>(run(true, "git", "clean", "-n", "--", "uploads").lines().toList());
		String database = databaseName(Files.readString(properties));
		
		System.out.println("Seeded files committed in uploads/: " + tracked);
		System.out.println("Seeded files to put back (deleted or changed): " + restore.size());
		
		for (String line : restore) {
			System.out.println("  " + line.trim());
		}
		
		System.out.println("Practice files to delete: " + delete.size());
		
		for (String line : delete) {
			System.out.println("  " + line.replace("Would remove ", ""));
		}
		
		System.out.println(filesOnly ? "Database: skipped (--files-only)" : "Database to rebuild from database/full_reset.sql: " + database);
		
		if (!apply) {
			System.out.println("\nNothing was changed. Run again with --apply to do it.");
			return;
		}
		
		run(false, "git", "checkout", "--", "uploads");
		run(false, "git", "clean", "-f", "-q", "--", "uploads");
		System.out.println("\nuploads/ is back to the " + tracked + " seeded files.");
		
		if (!filesOnly) {
			String output = run(true, "sqlcmd", "-S", "localhost", "-d", database, "-E", "-C", "-I", "-b", "-i", root.resolve("database/full_reset.sql").toString());
			
			if (output.contains("Msg ")) {
				fail("The database reset reported errors:\n" + output);
			}
			
			System.out.println("Database " + database + " was rebuilt from database/full_reset.sql.");
		}
		
		System.out.println("Done. Restart the app before using it.");
	}
	
	static String databaseName(String properties) {
		Matcher matcher = Pattern.compile("databaseName=([^;\\s]+)").matcher(properties);
		
		if (!matcher.find()) {
			fail("Could not find databaseName in application.properties.");
		}
		
		return matcher.group(1);
	}
	
	static String run(boolean quiet, String... command) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		String output = new String(process.getInputStream().readAllBytes());
		int exitCode = process.waitFor();
		
		if (exitCode != 0) {
			fail(String.join(" ", command) + " failed:\n" + output);
		}
		
		if (!quiet && !output.isBlank()) {
			System.out.print(output);
		}
		
		return output;
	}
	
	static void fail(String message) {
		System.err.println("ERROR: " + message);
		System.exit(1);
	}
	
}
