import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// java database/RestoreDemoState.java --apply

public class RestoreDemoState {

	public static void main(String[] args) throws Exception {
		boolean apply = false;
		boolean filesOnly = false;
		boolean databaseOnly = false;
		Path seedFolder = Path.of("").toAbsolutePath().resolveSibling("lexflow-beta").resolve("uploads");

		for (String arg : args) {

			if (arg.startsWith("--seed-from=")) {
				seedFolder = Path.of(arg.substring(12)).toAbsolutePath();
			} else if (arg.equals("--apply")) {
				apply = true;
			} else if (arg.equals("--files-only")) {
				filesOnly = true;
			} else if (arg.equals("--database-only")) {
				databaseOnly = true;
			} else {
				fail("Unknown option: " + arg);
			}

		}

		if (filesOnly && databaseOnly) {
			fail("Use only one of --files-only and --database-only.");
		}

		Path root = Path.of("").toAbsolutePath();
		Path propertiesFile = root.resolve("src/main/resources/application.properties");
		Path resetFile = root.resolve("database/full_reset.sql");
		Path seedFile = Files.exists(root.resolve("database/seeder.sql")) ? root.resolve("database/seeder.sql") : root.resolve("src/main/resources/db/migration/V4__seed_data.sql");

		if (!Files.exists(root.resolve("pom.xml")) || !Files.exists(resetFile) || !Files.exists(propertiesFile) || !Files.exists(seedFile)) {
			fail("Run this from the project's root folder (the one with pom.xml).");
		}

		Properties properties = new Properties();

		try (Reader reader = Files.newBufferedReader(propertiesFile)) {
			properties.load(reader);
		}

		String url = properties.getProperty("spring.datasource.url", "");
		boolean postgres = url.startsWith("jdbc:postgresql:");

		if (!postgres && !url.startsWith("jdbc:sqlserver:")) {
			fail("spring.datasource.url in application.properties is neither a SQL Server nor a PostgreSQL address.");
		}

		String host;
		String port;
		String database;

		if (postgres) {
			Matcher matcher = Pattern.compile("jdbc:postgresql://([^:/]+):?(\\d*)/([^?;]+)").matcher(url);

			if (!matcher.find()) {
				fail("Could not read the host and database from spring.datasource.url.");
			}

			host = matcher.group(1);
			port = matcher.group(2).isEmpty() ? "5432" : matcher.group(2);
			database = matcher.group(3);
		} else {
			Matcher matcher = Pattern.compile("jdbc:sqlserver://([^:;/]+):?(\\d*)").matcher(url);
			Matcher name = Pattern.compile("databaseName=([^;\\s]+)").matcher(url);

			if (!matcher.find() || !name.find()) {
				fail("Could not read the host and databaseName from spring.datasource.url.");
			}

			host = matcher.group(1);
			port = matcher.group(2).isEmpty() ? "1433" : matcher.group(2);
			database = name.group(1);
		}

		String username = properties.getProperty("spring.datasource.username", "");
		String password = properties.getProperty("spring.datasource.password", "");
		boolean usesGit = Files.isDirectory(root.resolve(".git"));
		List<String> toRestore = new ArrayList<>();
		List<String> toDelete = new ArrayList<>();
		List<String> toCopy = new ArrayList<>();
		int tracked = 0;

		if (!databaseOnly) {

			if (usesGit) {
				tracked = run(root, null, "git", "ls-files", "uploads").lines().toList().size();

				if (tracked < 200) {
					fail("Only " + tracked + " files in uploads/ are committed to git, so the seeded files are not committed here and would be deleted. Nothing was changed.");
				}

				toRestore.addAll(run(root, null, "git", "status", "--porcelain", "--untracked-files=no", "--", "uploads").lines().map(String::trim).toList());
				toDelete.addAll(run(root, null, "git", "clean", "-n", "--", "uploads").lines().map(line -> line.replace("Would remove ", "")).toList());
			} else {
				Set<String> seeded = seededUploads(seedFile);
				Set<String> present = new HashSet<>();

				if (Files.isDirectory(root.resolve("uploads"))) {

					try (Stream<Path> files = Files.list(root.resolve("uploads"))) {
						files.filter(Files::isRegularFile).forEach(file -> present.add("uploads/" + file.getFileName()));
					}

				}

				for (String file : present) {

					if (!seeded.contains(file)) {
						toDelete.add(file);
					}

				}

				for (String file : seeded) {
					Path source = seedFolder.resolve(file.substring("uploads/".length()));
					boolean exists = present.contains(file);

					if (!Files.exists(source)) {

						if (!exists) {
							toRestore.add(file + " (missing, and not in " + seedFolder + ")");
						}

					} else if (!exists || !Arrays.equals(Files.readAllBytes(root.resolve(file)), Files.readAllBytes(source))) {
						toRestore.add(file + (exists ? " (different from the seed folder)" : " (missing)"));
						toCopy.add(file);
					}

				}

			}

		}

		System.out.println("Project: " + root);
		System.out.println("Database: " + (postgres ? "PostgreSQL" : "SQL Server") + " " + database + " on " + host + ":" + port);
		System.out.println(usesGit ? "Uploads: restored with git (" + tracked + " seeded files committed)" : "Uploads: compared with the files listed in " + root.relativize(seedFile).toString().replace('\\', '/') + " (no git here); seed folder: " + seedFolder);
		System.out.println("Seeded files to put back or missing: " + toRestore.size());

		for (String line : toRestore) {
			System.out.println("  " + line);
		}

		System.out.println("Practice files to delete: " + toDelete.size());

		for (String line : toDelete) {
			System.out.println("  " + line);
		}

		System.out.println(filesOnly ? "Database reset: skipped (--files-only)" : "Database reset with database/full_reset.sql" + (postgres ? " (empties the schema; Flyway rebuilds and reseeds it when the app starts)" : ""));

		if (!apply) {
			System.out.println("\nNothing was changed. Run again with --apply to do it.");
			return;
		}

		if (!databaseOnly) {

			if (usesGit) {
				run(root, null, "git", "checkout", "--", "uploads");
				run(root, null, "git", "clean", "-f", "-q", "--", "uploads");
			} else {

				for (String file : toDelete) {
					Files.delete(root.resolve(file));
				}

				for (String file : toCopy) {
					Files.createDirectories(root.resolve(file).getParent());
					Files.copy(seedFolder.resolve(file.substring("uploads/".length())), root.resolve(file), StandardCopyOption.REPLACE_EXISTING);
				}

			}

			System.out.println("\nuploads/ now holds only the seeded files.");
		}

		if (!filesOnly) {
			String output;

			if (postgres) {
				output = run(root, password, findPsql().toString(), "-h", host, "-p", port, "-U", username.isEmpty() ? "postgres" : username, "-d", database, "-w", "-v", "ON_ERROR_STOP=1", "-f", resetFile.toString());

				if (output.contains("ERROR")) {
					fail("The database reset reported errors:\n" + output);
				}
			} else {
				output = run(root, null, "sqlcmd", "-S", host + (port.equals("1433") ? "" : "," + port), "-d", database, "-E", "-C", "-I", "-b", "-i", resetFile.toString());

				if (output.contains("Msg ")) {
					fail("The database reset reported errors:\n" + output);
				}
			}

			System.out.println("Database " + database + " was reset from database/full_reset.sql.");
		}

		System.out.println("Done. Start the app (stop it first if it was running).");
	}

	static Set<String> seededUploads(Path seedFile) throws IOException {
		Set<String> seeded = new HashSet<>();
		Matcher matcher = Pattern.compile("'(uploads/[^']+)'").matcher(Files.readString(seedFile));

		while (matcher.find()) {
			seeded.add(matcher.group(1));
		}

		return seeded;
	}

	static Path findPsql() throws IOException {
		String pathVariable = System.getenv("PATH") == null ? "" : System.getenv("PATH");

		for (String folder : pathVariable.split(File.pathSeparator)) {
			Path candidate = Path.of(folder, "psql.exe");

			if (!folder.isBlank() && Files.exists(candidate)) {
				return candidate;
			}

		}

		Path installFolder = Path.of("C:\\Program Files\\PostgreSQL");

		if (Files.isDirectory(installFolder)) {

			try (Stream<Path> versions = Files.list(installFolder)) {
				Path newest = versions.map(version -> version.resolve("bin").resolve("psql.exe")).filter(Files::exists).sorted().reduce((first, second) -> second).orElse(null);

				if (newest != null) {
					return newest;
				}
			}

		}

		fail("psql.exe was not found on PATH or under C:\\Program Files\\PostgreSQL. Install the PostgreSQL command line tools or add them to PATH.");

		return null;
	}

	static String run(Path directory, String password, String... command) throws IOException {
		try {
			ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);

			if (password != null) {
				builder.environment().put("PGPASSWORD", password);
			}

			Process process = builder.start();
			String output = new String(process.getInputStream().readAllBytes());

			if (process.waitFor() != 0) {
				fail(String.join(" ", command) + " failed:\n" + output + (output.contains("Connection refused") || output.contains("could not connect") ? "\nIs the PostgreSQL service running? Start it (service postgresql-x64-18) and try again." : ""));
			}

			return output;
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	static void fail(String message) {
		System.err.println("ERROR: " + message);
		System.exit(1);
	}

}
