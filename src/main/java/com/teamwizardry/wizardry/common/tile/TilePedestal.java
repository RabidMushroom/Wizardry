package com.teamwizardry.wizardry.common.tile;

import com.teamwizardry.wizardry.Wizardry;
import com.teamwizardry.wizardry.client.fx.particle.SparkleFX;
import com.teamwizardry.wizardry.init.ModBlocks;
import com.teamwizardry.wizardry.init.ModItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Created by Saad on 5/7/2016.
 */
public class TilePedestal extends TileEntity implements ITickable {

    private ItemStack stack;
    private List<Vec3d> points;
    private BlockPos linkedBlock;
    private int queue = 0;

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
        markDirty();
        if (worldObj != null) {
            IBlockState state = worldObj.getBlockState(getPos());
            worldObj.notifyBlockUpdate(getPos(), state, state, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("stack")) stack = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("stack"));
        else stack = null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (stack != null) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            stack.writeToNBT(tagCompound);
            compound.setTag("stack", tagCompound);
        }
        return compound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbtTag = new NBTTagCompound();
        this.writeToNBT(nbtTag);
        return new SPacketUpdateTileEntity(getPos(), 1, nbtTag);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void update() {
        if (stack != null && linkedBlock == null) {
            if (stack.getItem() == ModItems.PEARL_MANA) {
                NBTTagCompound compound = stack.getTagCompound();
                if (compound != null) {
                    if (compound.hasKey("link_x") && compound.hasKey("link_y") && compound.hasKey("link_z")) {
                        BlockPos pos = new BlockPos(compound.getInteger("link_x"), compound.getInteger("link_y"), compound.getInteger("link_z"));
                        if (worldObj.getBlockState(pos).getBlock() == ModBlocks.PEDESTAL) { // TODO: instanceOf IManaAccepter
                            if (pos != getPos()) linkedBlock = pos;
                            worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
                        }
                    }
                }
            }
        }

        if (stack != null && linkedBlock != null) {
            if (stack.getItem() == ModItems.PEARL_MANA) {

                if (queue < points.size()) {
                    Vec3d location = points.get(queue);
                    SparkleFX fizz = Wizardry.proxy.spawnParticleSparkle(worldObj, location.xCoord, location.yCoord, location.zCoord, 0.8F, 0.5F, 30, false);
                    fizz.setMotion(0, 0, 0);
                    fizz.jitter(20, 0.05, 0.05, 0.05);
                    queue++;
                } else {
                    queue = 0;
                    points.clear();
                }
            }
        }
    }

    public List<Vec3d> getPoints() {
        return points;
    }

    public void setPoints(List<Vec3d> points) {
        this.points = points;
    }

    public void setLinkedBlock(BlockPos linkedBlock) {
        this.linkedBlock = linkedBlock;
    }
}
