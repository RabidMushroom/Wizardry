package com.teamwizardry.wizardry.common.spell;

import com.teamwizardry.librarianlib.api.util.misc.Color;
import com.teamwizardry.librarianlib.math.Raycast;
import com.teamwizardry.wizardry.Wizardry;
import com.teamwizardry.wizardry.api.item.IColorable;
import com.teamwizardry.wizardry.api.module.Module;
import com.teamwizardry.wizardry.api.spell.SpellEntity;
import com.teamwizardry.wizardry.api.spell.event.SpellCastEvent;
import com.teamwizardry.wizardry.client.fx.particle.SparkleFX;
import com.teamwizardry.wizardry.client.fx.particle.trails.SparkleTrailHelix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants.NBT;

public class ProjectileEntity extends SpellEntity {
    private NBTTagList modules;
    private EntityPlayer player;
    private int ticker = 0;
    private Color trailColor;

    public ProjectileEntity(World world, double posX, double posY, double posZ, EntityPlayer caster, NBTTagCompound spell) {
        super(world, posX, posY, posZ, spell);
        this.setSize(0.1F, 0.1F);
        this.isImmuneToFire = true;
        this.player = caster;
        modules = spell.getTagList(Module.MODULES, NBT.TAG_COMPOUND);

        if (caster.getHeldItemMainhand() != null) {
            ItemStack stack = caster.getHeldItemMainhand();
            if (stack.getItem() instanceof IColorable) {
                IColorable colorable = (IColorable) stack.getItem();
                trailColor = colorable.getColor(stack);
            }
        }
    }

    @Override
    public float getEyeHeight() {
        return 0;
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();

        ticker++;
        for (int i = 0; i < 2; i++) {
            double theta = i * Math.toRadians((360.0 / 2) + ticker);
            Vec3d origin = new Vec3d(posX + 0.5 * Math.cos(theta), posY, posZ + 0.5 * Math.sin(theta));

            // TODO: Add motion so they dont just move in the same space
            SparkleTrailHelix helix = Wizardry.proxy.spawnParticleSparkleTrailHelix(worldObj, origin, getPositionVector(), 0.5, theta, 50, false);
            helix.setColor((int) trailColor.r - 30, (int) trailColor.g - 30, (int) trailColor.b - 30);
            //helix.addContinuousMotion(new Vec3d(-motionX * 10, -motionY * 10, -motionZ * 10));

            SparkleFX fizz = Wizardry.proxy.spawnParticleSparkle(worldObj, posX, posY, posZ, 0.5F, 0.5F, 20, true);
            fizz.randomizeSizes();
            fizz.setColor((int) trailColor.r, (int) trailColor.g, (int) trailColor.b);
            fizz.randomDirection(0.1, 0.1, 0.1);
            fizz.jitter(10, 0.1, 0.1, 0.1);
        }

        RayTraceResult cast = Raycast.cast(this, new Vec3d(motionX, motionY, motionZ), Math.min(spell.getDouble(Module.SPEED), 1));

        if (cast != null) {
            if (cast.typeOfHit == RayTraceResult.Type.BLOCK) {
                for (int i = 0; i < modules.tagCount(); i++) {
                    BlockPos pos = cast.getBlockPos();
                    SpellEntity entity = new SpellEntity(worldObj, pos.getX(), pos.getY(), pos.getZ());
                    SpellCastEvent event = new SpellCastEvent(modules.getCompoundTagAt(i), entity, player);
                    MinecraftForge.EVENT_BUS.post(event);
                }
                this.setDead();
            } else if (cast.typeOfHit == RayTraceResult.Type.ENTITY && cast.entityHit != player) {
                for (int i = 0; i < modules.tagCount(); i++) {
                    SpellCastEvent event = new SpellCastEvent(modules.getCompoundTagAt(i), cast.entityHit, player);
                    MinecraftForge.EVENT_BUS.post(event);
                }
                this.setDead();
            }
        }

        posX += motionX * 4;
        posY += motionY * 4;
        posZ += motionZ * 4;
        setPosition(posX, posY, posZ);
    }

    public void setDirection(float yaw, float pitch) {
        double speed = spell.getDouble(Module.SPEED) / 10;
        Vec3d dir = this.getVectorForRotation(pitch, yaw);
        this.setVelocity(dir.xCoord * speed, dir.yCoord * speed, dir.zCoord * speed);
    }
}
