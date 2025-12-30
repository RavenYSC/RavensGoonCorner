package com.raven.client.voicechat;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Handles global keyboard events for voice chat push-to-talk
 */
public class VoiceChatEventHandler {
    
    private boolean pttWasPressed = false;
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        // Don't process if a GUI is open (let the GUI handle it)
        if (mc.currentScreen != null) {
            // Reset PTT when GUI opens
            if (pttWasPressed) {
                VoiceChatManager.getInstance().setPushToTalkActive(false);
                pttWasPressed = false;
            }
            return;
        }
        
        VoiceChatManager voiceManager = VoiceChatManager.getInstance();
        if (!voiceManager.isInitialized() || !voiceManager.isConnected()) {
            return;
        }
        
        int pttKey = voiceManager.getPushToTalkKey();
        boolean pttPressed = Keyboard.isKeyDown(pttKey);
        
        if (pttPressed && !pttWasPressed) {
            // PTT key pressed
            voiceManager.setPushToTalkActive(true);
            pttWasPressed = true;
            System.out.println("[VoiceChat] PTT activated (key " + pttKey + ")");
        } else if (!pttPressed && pttWasPressed) {
            // PTT key released
            voiceManager.setPushToTalkActive(false);
            pttWasPressed = false;
            System.out.println("[VoiceChat] PTT deactivated");
        }
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Additional key input handling if needed
        // This fires when any key is pressed/released during gameplay
    }
}
