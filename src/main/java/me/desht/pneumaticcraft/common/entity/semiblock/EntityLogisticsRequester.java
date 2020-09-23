package me.desht.pneumaticcraft.common.entity.semiblock;

import me.desht.pneumaticcraft.common.core.ModContainers;
import me.desht.pneumaticcraft.common.semiblock.IProvidingInventoryListener;
import me.desht.pneumaticcraft.common.semiblock.ISpecificRequester;
import me.desht.pneumaticcraft.common.thirdparty.ae2.AE2Integration;
import me.desht.pneumaticcraft.common.thirdparty.ae2.AE2RequesterIntegration;
import me.desht.pneumaticcraft.common.util.IOHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

public class EntityLogisticsRequester extends EntityLogisticsFrame implements ISpecificRequester, IProvidingInventoryListener {
    private static final DataParameter<Boolean> AE2_ENABLED = EntityDataManager.createKey(EntityLogisticsRequester.class, DataSerializers.BOOLEAN);

    private int minItems = 1;
    private int minFluid = 1;

    private AE2RequesterIntegration ae2requester = null;

    public EntityLogisticsRequester(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void registerData() {
        super.registerData();

        if (AE2Integration.isAvailable()) {
            getDataManager().register(AE2_ENABLED, false);
        }
    }

    @Override
    public int getColor() {
        return 0xFF0000FF;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    protected ContainerType<?> getContainerType() {
        return ModContainers.LOGISTICS_FRAME_REQUESTER.get();
    }

    public int getMinItemOrderSize() {
        return minItems;
    }

    public void setMinItemOrderSize(int minItems) {
        this.minItems = minItems;
    }

    public int getMinFluidOrderSize() {
        return minFluid;
    }

    public void setMinFluidOrderSize(int minFluid) {
        this.minFluid = minFluid;
    }

    @Override
    protected void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);

        setMinItemOrderSize(tag.getInt(NBT_MIN_ITEMS));
        setMinFluidOrderSize(tag.getInt(NBT_MIN_FLUID));
    }

    @Override
    public CompoundNBT serializeNBT(CompoundNBT tag) {
        tag = super.serializeNBT(tag);

        tag.putInt(NBT_MIN_ITEMS, getMinItemOrderSize());
        tag.putInt(NBT_MIN_FLUID, getMinFluidOrderSize());

        return tag;
    }

    @Override
    public boolean canFilterStack() {
        return true;
    }

    @Override
    public boolean supportsBlacklisting() {
        return false;
    }

    @Override
    public void writeToBuf(PacketBuffer payload) {
        super.writeToBuf(payload);

        payload.writeVarInt(minItems);
        payload.writeVarInt(minFluid);
    }

    @Override
    public void readFromBuf(PacketBuffer payload) {
        super.readFromBuf(payload);

        minItems = payload.readVarInt();
        minFluid = payload.readVarInt();
    }

    @Override
    public void tick() {
        super.tick();

        if (!world.isRemote && AE2Integration.isAvailable()) {
            getAE2integration().maybeCheckForInterface();
        }
    }

    @Override
    public void notify(TileEntityAndFace teAndFace) {
        if (AE2Integration.isAvailable()) {
            getAE2integration().maybeAddTE(teAndFace);
        }
    }

    @Override
    protected void onBroken() {
        super.onBroken();

        if (AE2Integration.isAvailable()) {
            getAE2integration().shutdown();
        }
    }

    @Override
    public void handleGUIButtonPress(String tag, boolean shiftHeld, PlayerEntity player) {
        super.handleGUIButtonPress(tag, shiftHeld, player);

        if (tag.equals("ae2") && AE2Integration.isAvailable()) {
            setAE2enabled(!isAE2enabled());
            getAE2integration().setEnabled(isAE2enabled());
        }
    }

    @Override
    public int amountRequested(ItemStack stack) {
        int totalRequestingAmount = getTotalRequestedAmount(stack);
        if (totalRequestingAmount > 0) {
            return IOHelper.getInventoryForTE(getCachedTileEntity(), getFacing()).map(itemHandler -> {
                int count = 0;
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack s = itemHandler.getStackInSlot(i);
                    // important that the request stack is passed first to matchOneItem()
                    // otherwise tag filters in the dest inventory will count as that item, messing up amounts
                    if (!s.isEmpty() && getItemFilterHandler().matchOneItem(stack, s)) {
                        count += s.getCount();
                    }
                }
                count += getIncomingItems(stack);
                return Math.max(0, Math.min(stack.getCount(), totalRequestingAmount - count));
            }).orElse(0);
        }
        return 0;
    }

    @Override
    public int amountRequested(FluidStack stack) {
        int totalRequestingAmount = getTotalRequestedAmount(stack);
        if (totalRequestingAmount > 0) {
            return getCachedTileEntity().getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, getFacing()).map(fluidHandler -> {
                int count = 0;
                for (int i = 0; i < fluidHandler.getTanks(); i++) {
                    FluidStack contents = fluidHandler.getFluidInTank(i);
                    if (contents.getFluid() == stack.getFluid()) {
                        count += contents.getAmount();
                    }
                }
                count += getIncomingFluid(stack.getFluid());
                return Math.max(0, Math.min(stack.getAmount(), totalRequestingAmount - count));
            }).orElse(0);
        }
        return 0;
    }

    private int getTotalRequestedAmount(ItemStack stack) {
        return getItemFilterHandler().getMatchedCount(stack);
    }

    private int getTotalRequestedAmount(FluidStack stack) {
        int requesting = 0;
        for (int i = 0; i < 9; i++) {
            FluidStack requestingStack = getFluidFilter(i);
            if (requestingStack != null && requestingStack.getFluid() == stack.getFluid()) {
                requesting += requestingStack.getAmount();
            }
        }
        return requesting;
    }

    private void setAE2enabled(boolean enabled) {
        getDataManager().set(AE2_ENABLED, enabled);
    }

    public boolean isAE2enabled() {
        return AE2Integration.isAvailable() && getDataManager().get(AE2_ENABLED);
    }

    public AE2RequesterIntegration getAE2integration() {
        if (ae2requester == null) {
            ae2requester = new AE2RequesterIntegration(this);
        }
        return ae2requester;
    }
}
