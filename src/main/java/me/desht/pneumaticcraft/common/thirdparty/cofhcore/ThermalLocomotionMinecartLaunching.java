package me.desht.pneumaticcraft.common.thirdparty.cofhcore;

import com.google.common.collect.Maps;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.lib.ModIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class ThermalLocomotionMinecartLaunching {
    private static final Map<ResourceLocation, EntityType<?>> launchMap = Maps.newHashMap();

    public static void registerMinecartLaunchBehaviour() {
        // Adds thermal minecarts to launch map
        register("underwater_minecart");
        register("fire_tnt_minecart");
        register("ice_tnt_minecart");
        register("lightning_tnt_minecart");
        register("earth_tnt_minecart");
        register("ender_tnt_minecart");
        register("glowstone_tnt_minecart");
        register("redstone_tnt_minecart");
        register("slime_tnt_minecart");
        register("phyto_tnt_minecart");
        register("nuke_tnt_minecart");

        // Registers launch map
        PneumaticRegistry.getInstance().getItemRegistry().registerItemLaunchBehaviour((stack, player) -> {
            EntityType<?> entityType = launchMap.get(stack.getItem().getRegistryName());
            return entityType != null ? entityType.create(player.getLevel()) : null;
        });
    }

    /**
     * Adds the item and entity matching the passed ID to the launch map to be registered as launch behaviors
     * @param itemIDString item ID of item/entity to add to launch map
     */
    private static void register(String itemIDString) {
        ResourceLocation itemId = new ResourceLocation(ModIds.THERMAL, itemIDString);
        ResourceLocation entityId = new ResourceLocation(ModIds.THERMAL, itemIDString);
        EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(entityId);
        if (entityType != null) {
            launchMap.put(itemId, entityType);
        }
    }
}