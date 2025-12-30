package com.raven.client.skyblock;

import com.raven.client.gui.GuiOpener;
import com.raven.client.skyblock.gui.GuiSkyblockDPS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Manages keybindings for Skyblock features
 */
public class SkyblockKeyBinds {
    
    public static KeyBinding dpsCalculatorKey;
    
    /**
     * Register all Skyblock keybinds
     */
    public static void register() {
        dpsCalculatorKey = new KeyBinding(
            "DPS Calculator",
            Keyboard.KEY_J,  // Default key: J
            "RavenClient - Skyblock"
        );
        
        ClientRegistry.registerKeyBinding(dpsCalculatorKey);
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        
        // Only process when in-game and not in a GUI
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        
        if (dpsCalculatorKey != null && dpsCalculatorKey.isPressed()) {
            GuiOpener.openGuiNextTick(new GuiSkyblockDPS());
        }
    }
}
