package com.teamwizardry.wizardry.common.block;

import com.teamwizardry.wizardry.Wizardry;
import com.teamwizardry.wizardry.api.block.IManaAcceptor;
import com.teamwizardry.wizardry.client.render.TilePedestalRenderer;
import com.teamwizardry.wizardry.common.tile.TilePedestal;
import com.teamwizardry.wizardry.init.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by Saad on 5/7/2016.
 */
public class BlockPedestal extends Block implements ITileEntityProvider, IManaAcceptor {

    public BlockPedestal() {
        super(Material.ROCK);
        setUnlocalizedName("pedestal");
        setRegistryName("pedestal");
        GameRegistry.register(this);
        GameRegistry.register(new ItemBlock(this), getRegistryName());
        GameRegistry.registerTileEntity(TilePedestal.class, "pedestal");
        setCreativeTab(Wizardry.tab);
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(TilePedestal.class, new TilePedestalRenderer());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TilePedestal te = getTE(world, pos);
            if (heldItem != null && player.isSneaking()) {
                if (heldItem.getItem() == ModItems.PEARL_MANA) {
                    NBTTagCompound compound = new NBTTagCompound();
                    compound.setInteger("link_x", pos.getX());
                    compound.setInteger("link_y", pos.getY());
                    compound.setInteger("link_z", pos.getZ());
                    heldItem.setTagCompound(compound);
                    return true;
                }
            }

            if (te.getStack() == null && heldItem != null) {
                te.setStack(heldItem);
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                player.openContainer.detectAndSendChanges();

            } else if (te.getStack() != null && heldItem == null) {
                ItemStack stack = te.getStack();
                te.setStack(null);
                if (!player.inventory.addItemStackToInventory(stack)) {
                    EntityItem entityItem = new EntityItem(world, pos.getX(), pos.getY() + 1, pos.getZ(), stack);
                    world.spawnEntityInWorld(entityItem);
                } else {
                    player.openContainer.detectAndSendChanges();
                }
            }
        }
        return true;
    }

    private TilePedestal getTE(World world, BlockPos pos) {
        return (TilePedestal) world.getTileEntity(pos);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TilePedestal();
    }

    @Override
    public boolean canRenderInLayer(BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState blockState) {
        return false;
    }
}
