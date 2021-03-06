package com.teamwizardry.wizardry.common.spell.module.shapes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import com.teamwizardry.wizardry.api.module.Module;
import com.teamwizardry.wizardry.api.module.attribute.Attribute;
import com.teamwizardry.wizardry.api.spell.ModuleType;
import com.teamwizardry.wizardry.common.spell.ProjectileEntity;

public class ModuleProjectile extends Module {
    public ModuleProjectile() {
    	setStaticIconIndex(0, 0);
        attributes.addAttribute(Attribute.SPEED);
        attributes.addAttribute(Attribute.PIERCE);
        attributes.addAttribute(Attribute.SCATTER);
        attributes.addAttribute(Attribute.PROJ_COUNT);
    }

    @Override
    public ModuleType getType() {
        return ModuleType.SHAPE;
    }

    @Override
    public String getDescription()
    {
    	return "Fires a projectile that targets the first entity hit.";
    }

    @Override
    public String getDisplayName() {
        return "Projectile";
    }

    @Override
    public NBTTagCompound getModuleData() {
    	NBTTagCompound compound = super.getModuleData();
    	compound.setDouble(SPEED, attributes.apply(Attribute.SPEED, 0.1));
    	compound.setInteger(PIERCE, (int) attributes.apply(Attribute.PIERCE, 0));
    	compound.setDouble(SCATTER, attributes.apply(Attribute.SCATTER, 0));
    	compound.setInteger(PROJ_COUNT, (int) attributes.apply(Attribute.PROJ_COUNT, 1));
    	compound.setDouble(MANA, attributes.apply(Attribute.MANA, 10));
    	compound.setDouble(BURNOUT, attributes.apply(Attribute.BURNOUT, 10));
        return compound;
    }

	@Override
	public boolean cast(EntityPlayer player, Entity caster, NBTTagCompound spell)
	{
		if (caster.worldObj.isRemote)
		{
//			double scatter = MathHelper.clamp_double(spell.getDouble(SCATTER), 0, 2);
//			int projCount = spell.getInteger(PROJ_COUNT);
		
//			double castSpread = 180 * scatter; //180 degrees, or half a circle
//			double anglePerProj = castSpread / (projCount - 1);
		
			float yaw = caster.rotationYaw /*- (90 * (float)castSpread)*/;
			float pitch = caster.rotationPitch;
//			for (int i = 0; i < projCount; i++)
			{
				ProjectileEntity proj = new ProjectileEntity(caster.worldObj, caster.posX, caster.posY + caster.getEyeHeight(), caster.posZ, player, spell);
				proj.setDirection(yaw, pitch);
//				yaw += anglePerProj;
				caster.worldObj.spawnEntityInWorld(proj);
			}
		}
		return true;
	}
}