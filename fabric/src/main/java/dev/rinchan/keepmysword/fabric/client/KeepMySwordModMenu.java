package dev.rinchan.keepmysword.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.rinchan.keepmysword.client.KeepMySwordConfigScreen;

public final class KeepMySwordModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KeepMySwordConfigScreen::new;
    }
}
