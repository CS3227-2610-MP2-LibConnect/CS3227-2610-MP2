package libconnect.cli;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;

/** Starts the temporary interactive librarian CLI. */
public final class Main {
    private Main() {
    }

    /** Starts the CLI using the repository's default data directory. */
    public static void main(String[] args) {
        LibrarianCli cli = CliApplication.create(Path.of("data"),
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                Clock.systemDefaultZone());
        cli.run();
    }
}
