package dev.rinchan.keepmysword;

import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import java.nio.file.Path;

public final class KeepMySword {
    public static final String MOD_ID = "keep_my_sword";

    private KeepMySword() {}

    public static void init(Path configDir) {
        KeepMySwordConfig.load(configDir);
    }
}
