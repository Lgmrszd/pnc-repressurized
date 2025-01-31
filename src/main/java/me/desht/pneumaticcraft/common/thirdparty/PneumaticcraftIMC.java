package me.desht.pneumaticcraft.common.thirdparty;

import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class containing manager for sending all IMC messages to other mods
 */
@Mod.EventBusSubscriber(modid = Names.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PneumaticcraftIMC {

    // List of all IMC messages to be sent at the InterModEnqueueEvent stage
    public static Queue<InterModComms.IMCMessage> iMCMessageQueue = new ConcurrentLinkedQueue<>();

    /**
     * Adds the passed IMC message to the IMC message cache
     * @param message IMC message to add to IMC message cache
     */
    public static void addIMCMessageToCache(InterModComms.IMCMessage message) {
        iMCMessageQueue.add(message);
    }

    /**
     * Sends all cached IMC messages to other mods
     */
    @SubscribeEvent
    public static void sendIMCMessages(InterModEnqueueEvent event) {
        Log.info("Sending IMC messages.");

        while (!iMCMessageQueue.isEmpty()) {
            InterModComms.IMCMessage message = iMCMessageQueue.remove();
            InterModComms.sendTo(Names.MOD_ID, message.modId(), message.method(), message.messageSupplier());
        }
    }
}