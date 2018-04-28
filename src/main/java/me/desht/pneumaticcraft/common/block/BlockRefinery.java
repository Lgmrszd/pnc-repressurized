package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.common.recipes.RefineryRecipe;
import me.desht.pneumaticcraft.common.tileentity.TileEntityRefinery;
import me.desht.pneumaticcraft.proxy.CommonProxy.EnumGuiId;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockRefinery extends BlockPneumaticCraftModeled {

    BlockRefinery() {
        super(Material.IRON, "refinery");
    }

    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityRefinery.class;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float par7, float par8, float par9) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityRefinery) {
            TileEntityRefinery master = ((TileEntityRefinery) te).getMasterRefinery();
            return super.onBlockActivated(world, master.getPos(), state, player, hand, side, par7, par8, par9);
        }
        return false;
    }

    @Override
    public EnumGuiId getGuiID() {
        return EnumGuiId.REFINERY;
    }

    @Override
    public boolean isRotatable() {
        return true;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        int nRefineries = 0;
        int up = 1, down = 1;
        while (worldIn.getBlockState(pos.up(up++)).getBlock() instanceof BlockRefinery) {
            nRefineries++;
        }
        while (worldIn.getBlockState(pos.down(down++)).getBlock() instanceof BlockRefinery) {
            nRefineries++;
        }
        return nRefineries < RefineryRecipe.MAX_OUTPUTS && super.canPlaceBlockAt(worldIn, pos);
    }
}
