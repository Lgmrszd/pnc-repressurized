/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.block.entity;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.common.block.ChargingStationBlock;
import me.desht.pneumaticcraft.common.block.entity.RedstoneController.EmittingRedstoneMode;
import me.desht.pneumaticcraft.common.block.entity.RedstoneController.RedstoneMode;
import me.desht.pneumaticcraft.common.core.ModBlockEntities;
import me.desht.pneumaticcraft.common.core.ModUpgrades;
import me.desht.pneumaticcraft.common.inventory.ChargingStationMenu;
import me.desht.pneumaticcraft.common.inventory.ChargingStationUpgradeManagerMenu;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.inventory.handler.ChargeableItemHandler;
import me.desht.pneumaticcraft.common.item.IChargeableContainerProvider;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.util.GlobalTileEntityCacheManager;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChargingStationBlockEntity extends AbstractAirHandlingBlockEntity implements IRedstoneControl<ChargingStationBlockEntity>, CamouflageableBlockEntity, MenuProvider {
    private static final List<RedstoneMode<ChargingStationBlockEntity>> REDSTONE_MODES = ImmutableList.of(
            new EmittingRedstoneMode<>("standard.never", new ItemStack(Items.GUNPOWDER), te -> false),
            new EmittingRedstoneMode<>("chargingStation.idle", Textures.GUI_CHARGE_IDLE, ChargingStationBlockEntity::isIdle),
            new EmittingRedstoneMode<>("chargingStation.charging", Textures.GUI_CHARGING, te -> te.charging),
            new EmittingRedstoneMode<>("chargingStation.discharging", Textures.GUI_DISCHARGING, te -> te.discharging)
    );
    private static final int INVENTORY_SIZE = 1;
    public static final int CHARGE_INVENTORY_INDEX = 0;
    private static final int MAX_REDSTONE_UPDATE_FREQ = 10;  // in ticks; used to reduce lag from rapid updates

    @DescSynced
    private ItemStack chargingStackSynced = ItemStack.EMPTY;  // the item being charged, minus any nbt - for client display purposes

    private ChargingStationHandler itemHandler = new ChargingStationHandler();  // holds the item being charged
    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> itemHandler);

    private ChargeableItemHandler chargeableInventory;  // inventory of the item being charged

    @GuiSynced
    public float chargingItemPressure;
    @GuiSynced
    public boolean charging;
    @GuiSynced
    public boolean discharging;
    private boolean oldRedstoneStatus;
    private BlockState camoState;
    private long lastRedstoneUpdate;
    private int pendingRedstoneStatus = -1;
    @GuiSynced
    private final RedstoneController<ChargingStationBlockEntity> rsController = new RedstoneController<>(this, REDSTONE_MODES);
    @GuiSynced
    public boolean upgradeOnly = false;

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_STATION.get(), pos, state, PressureTier.TIER_TWO, PneumaticValues.VOLUME_CHARGING_STATION, 4);
    }

    @Nonnull
    public ItemStack getChargingStack() {
        return itemHandler.getStackInSlot(CHARGE_INVENTORY_INDEX);
    }

    @Nonnull
    public ItemStack getChargingStackSynced() { return chargingStackSynced; }

    @Override
    public void tickServer() {
        super.tickServer();

        discharging = false;
        charging = false;

        chargingStackSynced = itemHandler.getStackInSlot(CHARGE_INVENTORY_INDEX);

        int airToTransfer = (int) (PneumaticValues.CHARGING_STATION_CHARGE_RATE * getSpeedMultiplierFromUpgrades());

        for (IAirHandler itemAirHandler : findChargeable()) {
            float itemPressure = itemAirHandler.getPressure();
            float itemVolume = itemAirHandler.getVolume();
            float chargerPressure = getPressure();
            float delta = Math.abs(chargerPressure - itemPressure) / 2.0F;
            int airInItem = itemAirHandler.getAir();

            if (PneumaticCraftUtils.epsilonEquals(chargerPressure, 0f) && delta < 0.1f) {
                // small kludge to get last tiny bit of air out of an item (arithmetic rounding)
                itemAirHandler.addAir(-airInItem);
            } else if (itemPressure > chargerPressure + 0.01F && itemPressure > 0F) {
                // move air from item to charger
                int airToMove = Math.min(Math.min(airToTransfer, airInItem), (int) (delta * airHandler.getVolume()));
                itemAirHandler.addAir(-airToMove);
                this.addAir(airToMove);
                discharging = true;
            } else if (itemPressure < chargerPressure - 0.01F && itemPressure < itemAirHandler.maxPressure()) {
                // move air from charger to item
                int maxAirInItem = (int) (itemAirHandler.maxPressure() * itemVolume);
                float boost = chargerPressure < 15f ? 1f : 1f + (chargerPressure - 15f) / 5f;
                int airToMove = Math.min(Math.min((int)(airToTransfer * boost), airHandler.getAir()), maxAirInItem - airInItem);
                airToMove = Math.min((int) (delta * itemVolume), airToMove);
                itemAirHandler.addAir(airToMove);
                this.addAir(-airToMove);
                charging = true;
            }
        }

        boolean shouldEmit = rsController.shouldEmit();
        if (oldRedstoneStatus != shouldEmit) {
            if (nonNullLevel().getGameTime() - lastRedstoneUpdate > MAX_REDSTONE_UPDATE_FREQ) {
                updateRedstoneOutput();
            } else {
                pendingRedstoneStatus = shouldEmit ? 1: 0;
            }
        } else if (pendingRedstoneStatus != -1 && nonNullLevel().getGameTime() - lastRedstoneUpdate > MAX_REDSTONE_UPDATE_FREQ) {
            updateRedstoneOutput();
        }

        airHandler.setSideLeaking(!upgradeOnly && hasNoConnectedAirHandlers() ? getRotation() : null);
    }

    private void updateRedstoneOutput() {
        oldRedstoneStatus = rsController.shouldEmit();
        updateNeighbours();
        pendingRedstoneStatus = -1;
        lastRedstoneUpdate = nonNullLevel().getGameTime();
    }

    private List<IAirHandler> findChargeable() {
        if (upgradeOnly) return Collections.emptyList();

        List<IAirHandler> res = new ArrayList<>();

        if (getChargingStack().getCount() == 1) {
            getChargingStack().getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).ifPresent(h -> {
                res.add(h);
                chargingItemPressure = h.getPressure();
            });
        }

        if (getUpgrades(ModUpgrades.DISPENSER.get()) > 0) {
            List<Entity> entitiesOnPad = nonNullLevel().getEntitiesOfClass(Entity.class, new AABB(getBlockPos().above()));
            for (Entity entity : entitiesOnPad) {
                if (entity instanceof ItemEntity ie) {
                    ie.getItem().getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).ifPresent(res::add);
                } else if (entity instanceof Player p) {
                    Inventory inv = p.getInventory();
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        ItemStack stack = inv.getItem(i);
                        if (stack.getCount() == 1) {
                            stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).ifPresent(res::add);
                        }
                    }
                } else {
                    entity.getCapability(PNCCapabilities.AIR_HANDLER_CAPABILITY).ifPresent(res::add);
                }
            }
        }
        return res;
    }

    @Override
    public boolean canConnectPneumatic(Direction side) {
        return getRotation() == side || side == Direction.DOWN;
    }

    @Override
    public void handleGUIButtonPress(String tag, boolean shiftHeld, ServerPlayer player) {
        if (rsController.parseRedstoneMode(tag))
            return;

        switch (tag) {
            case "open_upgrades":
                if (getChargingStack().getItem() instanceof IChargeableContainerProvider) {
                    MenuProvider provider = ((IChargeableContainerProvider) getChargingStack().getItem()).getContainerProvider(this);
                    NetworkHooks.openGui(player, provider, getBlockPos());
                }
                break;
            case "close_upgrades":
                NetworkHooks.openGui(player, this, getBlockPos());
                break;
            case "toggle_upgrade_only":
                upgradeOnly = !upgradeOnly;
                break;
        }
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return itemHandler;
    }

    private boolean isIdle() {
        return !charging && !discharging &&
                !getChargingStack().isEmpty() &&
                getChargingStack().getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).isPresent();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getBlockPos().getX() + 1, getBlockPos().getY() + 1, getBlockPos().getZ() + 1);
    }

    public ChargeableItemHandler getChargeableInventory() {
        return nonNullLevel().isClientSide ? new ChargeableItemHandler(this) : chargeableInventory;
    }

    @Override
    protected LazyOptional<IItemHandler> getInventoryCap() {
        return inventoryCap;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        itemHandler = new ChargingStationHandler();
        itemHandler.deserializeNBT(tag.getCompound("Items"));

        ItemStack chargeSlot = getChargingStack();
        if (chargeSlot.getItem() instanceof IChargeableContainerProvider) {
            chargeableInventory = new ChargeableItemHandler(this);
        }
        upgradeOnly = tag.getBoolean("UpgradeOnly");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (chargeableInventory != null) {
            chargeableInventory.writeToNBT();
        }
        tag.put("Items", itemHandler.serializeNBT());
        if (upgradeOnly) tag.putBoolean("UpgradeOnly", true);
    }

    @Override
    public void serializeExtraItemData(CompoundTag blockEntityTag, boolean preserveState) {
        if (upgradeOnly) blockEntityTag.putBoolean("UpgradeOnly", true);
    }

    @Override
    public void writeToPacket(CompoundTag tag) {
        super.writeToPacket(tag);

        CamouflageableBlockEntity.writeCamo(tag, camoState);
    }

    @Override
    public void readFromPacket(CompoundTag tag) {
        super.readFromPacket(tag);

        camoState = CamouflageableBlockEntity.readCamo(tag);
    }

    @Override
    public void onUpgradesChanged() {
        super.onUpgradesChanged();

        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.setBlockAndUpdate(worldPosition, state.setValue(ChargingStationBlock.CHARGE_PAD, getUpgrades(ModUpgrades.DISPENSER.get()) > 0));
        }
    }

    @Override
    public RedstoneController<ChargingStationBlockEntity> getRedstoneController() {
        return rsController;
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public BlockState getCamouflage() {
        return camoState;
    }

    @Override
    public void setCamouflage(BlockState state) {
        camoState = state;
        CamouflageableBlockEntity.syncToClient(this);
    }

    @Override
    public void setRemoved(){
        super.setRemoved();
        GlobalTileEntityCacheManager.getInstance().chargingStations.remove(this);
    }

    @Override
    public void clearRemoved(){
        super.clearRemoved();
        GlobalTileEntityCacheManager.getInstance().chargingStations.add(this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
        return new ChargingStationMenu(i, playerInventory, getBlockPos());
    }

    private class ChargingStationHandler extends BaseItemStackHandler {
        ChargingStationHandler() {
            super(ChargingStationBlockEntity.this, INVENTORY_SIZE);
        }

        @Override
        public int getSlotLimit(int slot){
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return slot == CHARGE_INVENTORY_INDEX
                    && (itemStack.isEmpty() || itemStack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).isPresent());
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);

            ChargingStationBlockEntity teCS = ChargingStationBlockEntity.this;

            ItemStack newStack = getStackInSlot(slot);
            if (!ItemStack.isSame(chargingStackSynced, newStack)) {
                chargingStackSynced = new ItemStack(newStack.getItem());
            }

            if (teCS.nonNullLevel().isClientSide || slot != CHARGE_INVENTORY_INDEX) return;

            teCS.chargeableInventory = newStack.getItem() instanceof IChargeableContainerProvider ?
                    new ChargeableItemHandler(teCS) :
                    null;

            // if any other player has a gui open for the previous item, force a reopen of the charging station gui
            for (Player player : teCS.nonNullLevel().players()) {
                if (player instanceof ServerPlayer sp
                        && player.containerMenu instanceof ChargingStationUpgradeManagerMenu manager
                        && manager.te == te) {
                    NetworkHooks.openGui(sp, ChargingStationBlockEntity.this, getBlockPos());
                }
            }
        }
    }
}
