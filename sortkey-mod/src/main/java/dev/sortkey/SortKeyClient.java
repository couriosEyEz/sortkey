package dev.sortkey;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

/**
 * Registriert EINE Taste (Standard: R). Ist ein Inventar-/Truhen-Bildschirm offen,
 * sortiert ein Druck darauf den Container (falls vorhanden) und dein Inventar.
 */
public class SortKeyClient implements ClientModInitializer {
    public static final String MOD_ID = "sortkey";

    private static KeyMapping sortKey;
    private static long lastSortNanos = 0L;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        sortKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.sortkey.sort",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_R,
                category));

        // Solange ein Bildschirm offen ist, ruft Minecraft consumeClick() NICHT auf,
        // deshalb hoeren wir direkt auf die Tastendruecke des jeweiligen Bildschirms.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((s, keyEvent) -> {
                    if (!sortKey.matches(keyEvent) || isTyping(s)) {
                        return;
                    }
                    // Entprellen: schuetzt vor doppelten Aufrufen (z. B. nach Fenster-Resize)
                    long now = System.nanoTime();
                    if (now - lastSortNanos < 250_000_000L) {
                        return;
                    }
                    lastSortNanos = now;
                    Sorter.sort(client, containerScreen.getMenu());
                });
            }
        });
    }

    /** true, wenn gerade in ein Textfeld getippt wird (z. B. Rezeptbuch-Suche). */
    private static boolean isTyping(Screen screen) {
        GuiEventListener focused = screen.getFocused();
        while (focused instanceof ContainerEventHandler handler && handler.getFocused() != null) {
            focused = handler.getFocused();
        }
        return focused instanceof EditBox box && box.isFocused();
    }
}
