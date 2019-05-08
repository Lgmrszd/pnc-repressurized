package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IHackableEntity;
import me.desht.pneumaticcraft.common.hacking.HackableHandler;
import me.desht.pneumaticcraft.common.CommonArmorHandler;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class PacketHackingEntityFinish extends AbstractPacket<PacketHackingEntityFinish> {
    private int entityId;

    public PacketHackingEntityFinish() {
    }

    public PacketHackingEntityFinish(Entity entity) {
        entityId = entity.getEntityId();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    @Override
    public void handleClientSide(PacketHackingEntityFinish message, EntityPlayer player) {
        Entity entity = player.world.getEntityByID(message.entityId);
        if (entity != null) {
            IHackableEntity hackableEntity = HackableHandler.getHackableForEntity(entity, player);
            if (hackableEntity != null) {
                hackableEntity.onHackFinished(entity, player);
                PneumaticCraftRepressurized.proxy.getHackTickHandler().trackEntity(entity, hackableEntity);
                CommonArmorHandler.getHandlerForPlayer(player).setHackedEntity(null);
                player.playSound(Sounds.HELMET_HACK_FINISH, 1.0F, 1.0F);
            }
        }

    }

    @Override
    public void handleServerSide(PacketHackingEntityFinish message, EntityPlayer player) {
    }

}
