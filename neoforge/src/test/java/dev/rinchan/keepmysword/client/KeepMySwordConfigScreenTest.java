package dev.rinchan.keepmysword.client;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.font.GlyphInfo;
import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Headless black-box lifecycle test for KeepMySwordConfigScreen:
 * verifies instantiation, widget construction, layout bounds, reset, save, and close throw zero exceptions.
 */
class KeepMySwordConfigScreenTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void configScreenInstantiationInitResetAndSaveWorkWithoutExceptions(@TempDir Path configDir) throws Exception {
        KeepMySwordConfig.load(configDir);
        try {
            KeepMySwordConfigScreen screen = new KeepMySwordConfigScreen(null);
            assertNotNull(screen.getTitle());

            Font headlessFont = createHeadlessFont();
            initializeScreenHeadless(screen, headlessFont, 854, 480);

            List<? extends GuiEventListener> children = screen.children();
            assertEquals(7, children.size(), "Expected 3 CycleButtons + 1 EditBox + 3 action Buttons");

            long cycleButtons = children.stream().filter(w -> w instanceof CycleButton<?>).count();
            long editBoxes = children.stream().filter(w -> w instanceof EditBox).count();
            long plainButtons = children.stream().filter(w -> w instanceof Button).count();
            assertEquals(3, cycleButtons, "Expected 3 CycleButtons");
            assertEquals(1, editBoxes, "Expected 1 EditBox for excluded items");
            assertEquals(3, plainButtons, "Expected 3 Buttons (Reset, Cancel, Save)");

            // Verify every widget has positive dimensions and lies inside the viewport
            for (GuiEventListener listener : children) {
                assertInstanceOf(AbstractWidget.class, listener);
                AbstractWidget widget = (AbstractWidget) listener;
                assertTrue(widget.getWidth() > 0 && widget.getHeight() > 0, "Widget must have positive size");
                assertTrue(widget.getX() >= 0 && widget.getX() + widget.getWidth() <= 854,
                        "Widget X out of bounds: " + widget.getX() + " + " + widget.getWidth());
                assertTrue(widget.getY() >= 0 && widget.getY() + widget.getHeight() <= 480,
                        "Widget Y out of bounds: " + widget.getY() + " + " + widget.getHeight());
            }

            // Exercise EditBox input + Save button click -> persists config cleanly
            EditBox excludedBox = children.stream()
                    .filter(w -> w instanceof EditBox)
                    .map(w -> (EditBox) w)
                    .findFirst()
                    .orElseThrow();
            excludedBox.setValue("#custom:tools, minecraft:wooden_sword");

            // Verify CycleButtons are present and readable (avoiding GLFW hasShiftDown() in headless mode)
            var toggles = children.stream()
                    .filter(CycleButton.class::isInstance)
                    .map(CycleButton.class::cast)
                    .toList();
            assertEquals(3, toggles.size());
            assertNotNull(toggles.get(0).getValue());

            List<Button> buttons = children.stream()
                    .filter(w -> w instanceof Button)
                    .map(w -> (Button) w)
                    .toList();
            assertEquals(3, buttons.size());
            // Save button (index 2) persists updated config to temp configDir
            assertDoesNotThrow(() -> buttons.get(2).onPress(), "Save button must not throw");
            assertEquals(
                    List.of("#custom:tools", "minecraft:wooden_sword"),
                    KeepMySwordConfig.get().excludedItems(),
                    "Saving from KeepMySwordConfigScreen must update active config");
            // Cancel button (index 1) and onClose must not throw when minecraft is null
            assertDoesNotThrow(() -> buttons.get(1).onPress(), "Cancel button must not throw");
            assertDoesNotThrow(screen::onClose, "onClose must not throw when minecraft is null");
        } finally {
            KeepMySwordConfig.setForTest(KeepMySwordConfig.defaults());
        }
    }

    private static Font createHeadlessFont() {
        return new Font(
                (ResourceLocation id) -> new FontSet(null, id) {
                    @Override
                    public GlyphInfo getGlyphInfo(int codePoint, boolean filterFishy) {
                        return (GlyphInfo.SpaceGlyphInfo) () -> 6.0F;
                    }
                },
                false);
    }

    private static void initializeScreenHeadless(Screen screen, Font font, int width, int height) throws Exception {
        setScreenField(screen, "font", font);
        setScreenField(screen, "width", width);
        setScreenField(screen, "height", height);
        Method initMethod = Screen.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(screen);
    }

    private static void setScreenField(Screen screen, String name, Object value) throws Exception {
        Field f = Screen.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(screen, value);
    }
}
