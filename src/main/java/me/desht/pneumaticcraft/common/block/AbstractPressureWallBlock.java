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

package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.common.advancements.AdvancementTriggers;
import me.desht.pneumaticcraft.common.block.PressureChamberWallBlock.WallState;
import me.desht.pneumaticcraft.common.block.entity.PressureChamberValveBlockEntity;
import me.desht.pneumaticcraft.common.block.entity.PressureChamberWallBlockEntity;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import static me.desht.pneumaticcraft.common.block.PressureChamberValveBlock.FORMED;

public abstract class AbstractPressureWallBlock extends AbstractPneumaticCraftBlock implements IBlockPressureChamber, PneumaticCraftEntityBlock {
    public static final EnumProperty<WallState> WALL_STATE = EnumProperty.create("wall_state", WallState.class);

    AbstractPressureWallBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PressureChamberWallBlockEntity(pPos, pState);
    }

    @Override
    public void setPlacedBy(Level par1World, BlockPos pos, BlockState state, LivingEntity par5EntityLiving, ItemStack iStack) {
        super.setPlacedBy(par1World, pos, state, par5EntityLiving, iStack);
        if (!par1World.isClientSide && PressureChamberValveBlockEntity.checkIfProperlyFormed(par1World, pos)) {
            AdvancementTriggers.PRESSURE_CHAMBER.trigger((ServerPlayer) par5EntityLiving);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult brtr) {
        if (world.isClientSide) {
            return isFormed(state) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        // forward activation to the pressure chamber valve, which will open the GUI
        return PneumaticCraftUtils.getTileEntityAt(world, pos, PressureChamberWallBlockEntity.class).map(te -> {
            PressureChamberValveBlockEntity valve = te.getPrimaryValve();
            if (valve != null) {
                NetworkHooks.openGui((ServerPlayer) player, valve, valve.getBlockPos());
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }).orElse(InteractionResult.FAIL);
    }

    private boolean isFormed(BlockState state) {
        if (state.hasProperty(WALL_STATE)) {
            return state.getValue(WALL_STATE) != WallState.NONE;
        } else if (state.hasProperty(FORMED)) {
            return state.getValue(FORMED);
        } else {
            return false;
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !world.isClientSide) {
            PneumaticCraftUtils.getTileEntityAt(world, pos, PressureChamberWallBlockEntity.class)
                    .ifPresent(PressureChamberWallBlockEntity::onBlockBreak);
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
