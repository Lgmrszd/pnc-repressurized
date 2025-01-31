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

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.block.AbstractPneumaticCraftBlock;
import me.desht.pneumaticcraft.common.core.ModBlockEntities;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.function.BiPredicate;

public class HeatPipeBlockEntity extends AbstractTickingBlockEntity implements CamouflageableBlockEntity, IHeatExchangingTE {
    private final IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance().getHeatRegistry().makeHeatExchangerLogic();
    private final LazyOptional<IHeatExchangerLogic> heatCap = LazyOptional.of(() -> heatExchanger);

    private BlockState camoState;

    public HeatPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_PIPE.get(), pos, state);
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return null;
    }

    @Override
    public LazyOptional<IHeatExchangerLogic> getHeatCap(Direction side) {
        return heatCap;
    }

    @Override
    public BiPredicate<LevelAccessor, BlockPos> heatExchangerBlockFilter() {
        // heat pipes don't connect to air or fluids
        return (world, pos) -> !world.isEmptyBlock(pos) && !(world.getBlockState(pos).getBlock() instanceof LiquidBlock);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (!nonNullLevel().isClientSide) {
            updateConnections();
        }
    }

    @Override
    public void onNeighborBlockUpdate(BlockPos fromPos) {
        super.onNeighborBlockUpdate(fromPos);

        updateConnections();
    }

    public void updateConnections() {
        BlockState state = getBlockState();
        boolean changed = false;
        for (Direction dir : DirectionUtil.VALUES) {
            BooleanProperty prop = AbstractPneumaticCraftBlock.connectionProperty(dir);
            boolean connected = heatExchanger.isSideConnected(dir);
            if (state.getValue(prop) != connected) {
                state = state.setValue(prop, connected);
                changed = true;
            }
        }
        if (changed) nonNullLevel().setBlockAndUpdate(worldPosition, state);
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
    public BlockState getCamouflage() {
        return camoState;
    }

    @Override
    public void setCamouflage(BlockState state) {
        camoState = state;
        CamouflageableBlockEntity.syncToClient(this);
    }

    @Override
    public IHeatExchangerLogic getHeatExchanger(Direction dir) {
        return heatExchanger;
    }
}
