package com.teamwizardry.wizardry.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.teamwizardry.librarianlib.api.gui.GuiTickHandler;
import com.teamwizardry.librarianlib.math.Box;
import com.teamwizardry.librarianlib.math.Matrix4;
import com.teamwizardry.librarianlib.ragdoll.cloth.Cloth;
import com.teamwizardry.librarianlib.ragdoll.cloth.Link;
import com.teamwizardry.librarianlib.ragdoll.cloth.PointMass3D;
import com.teamwizardry.wizardry.Wizardry;

public class CapeHandler {

	public static final CapeHandler INSTANCE = new CapeHandler();
	
	WeakHashMap<EntityLivingBase, Cloth> cloths = new WeakHashMap<>();
	WeakHashMap<EntityLivingBase, List<Box>> models = new WeakHashMap<>();
	
	private CapeHandler() {
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void tick(ClientTickEvent event) {
		if(event.phase != Phase.END)
			return;
		
		List<EntityLivingBase> keysToRemove = new ArrayList<>();
		
		for (Entry<EntityLivingBase, Cloth> e : cloths.entrySet()) {
			EntityLivingBase entity = e.getKey();
			if(entity.isDead) {
				keysToRemove.add(entity);
				continue;
			}
			
			if(e.getValue().masses != null && e.getValue().masses[0] != null) {
				
				Vec3d[] shoulderPoints = new Vec3d[] {
						new Vec3d( 0.4, 1.5,  0),
						new Vec3d( 0.25, 1.5, -0.25),
						new Vec3d( 0,    1.5, -0.25),
						new Vec3d(-0.25, 1.5, -0.25),
						new Vec3d(-0.4, 1.5,  0)
				};
				
				Matrix4 matrix = new Matrix4();
				matrix.translate(entity.getPositionVector());
				matrix.rotate(Math.toRadians( entity.renderYawOffset), new Vec3d(0, -1, 0));
				
				for (int i = 0; i < shoulderPoints.length; i++) {
					shoulderPoints[i] = matrix.apply(shoulderPoints[i]);
				}
				
				for (int i = 0; i < e.getValue().masses[0].length && i < shoulderPoints.length; i++) {
					e.getValue().masses[0][i].origPos = e.getValue().masses[0][i].pos;
					e.getValue().masses[0][i].pos = shoulderPoints[i];
				}
				
				List<Box> boxes = models.get(entity);
				if(boxes == null)
					boxes = ImmutableList.of();
				
				e.getValue().tick(entity, boxes);
			}
		}
		
		for (EntityLivingBase entity : keysToRemove) {
			cloths.remove(entity);
			models.remove(entity);
		}
	}
	
	List<ModelRenderer> rendererStack = new ArrayList<>();
	
	public List<Box> getBoxes(Vec3d modelPos, ModelBase model, double yaw) {
		if(!(model instanceof ModelBiped))
			return ImmutableList.of();
		List<Box> boxes = new ArrayList<>();
		
		rendererStack.clear();
		
		ModelBiped biped = (ModelBiped) model;
		
		Vec3d scale = new Vec3d(-1, 1, 1), transform = new Vec3d(0, -22.5, 0), rotate = new Vec3d(0, Math.toRadians(yaw), 0);
		
		processModel(modelPos, biped.bipedHead, boxes, scale, transform, rotate);
		
		scale = new Vec3d(-1, 1, 1);
		transform = new Vec3d(0, -22.5, 0);
		Vec3d armTransform = new Vec3d(2, 0, 0);
		
		processModel(modelPos, biped.bipedBody, boxes, scale, transform, rotate);
		processModel(modelPos, biped.bipedLeftArm, boxes, scale, transform.add(armTransform), rotate);
		processModel(modelPos, biped.bipedRightArm, boxes, scale, transform.subtract(armTransform), rotate);
		processModel(modelPos, biped.bipedLeftLeg, boxes, scale, transform, rotate);
		processModel(modelPos, biped.bipedRightLeg, boxes, scale, transform, rotate);
		
		return boxes;
	}
	
	public void processModel(Vec3d modelPos, ModelRenderer renderer, List<Box> boxes, Vec3d scale, Vec3d transform, Vec3d rotate) {
		rendererStack.add(renderer);
		Matrix4 matrix = new Matrix4(), inverse = new Matrix4();
		
		for (ModelRenderer render : rendererStack) {
			matrix.translate(new Vec3d(
					render.offsetX,
					render.offsetY,
					render.offsetZ
				));
			matrix.rotate(-render.rotateAngleX, new Vec3d(1, 0, 0));
			matrix.rotate(-render.rotateAngleY, new Vec3d(0, 1, 0));
			matrix.rotate(-render.rotateAngleZ, new Vec3d(0, 0, 1));
			matrix.translate(new Vec3d(
					render.rotationPointX,
					render.rotationPointY,
					render.rotationPointZ
				));
		}
		
		matrix.rotate(-rotate.zCoord, new Vec3d(0, 0, 1));
		matrix.rotate(-rotate.yCoord, new Vec3d(0, 1, 0));
		matrix.rotate(-rotate.xCoord, new Vec3d(1, 0, 0));
		matrix.translate(transform);
		matrix.scale(new Vec3d(scale.xCoord, scale.yCoord, scale.zCoord));
		matrix.scale(new Vec3d(16.0, 16.0, 16.0));
		matrix.translate(modelPos.scale(-1));
		
		inverse.translate(modelPos);
		inverse.scale(new Vec3d(1.0/16.0, 1.0/16.0, 1.0/16.0));
		inverse.scale(new Vec3d(1/scale.xCoord, 1/scale.yCoord, 1/scale.zCoord));
		inverse.translate(transform.scale(-1));
		inverse.rotate(rotate.xCoord, new Vec3d(1, 0, 0));
		inverse.rotate(rotate.yCoord, new Vec3d(0, 1, 0));
		inverse.rotate(rotate.zCoord, new Vec3d(0, 0, 1));
		
		for (ModelRenderer render : Lists.reverse(rendererStack)) {
			inverse.translate(new Vec3d(
					-(render.rotationPointX),
					-(render.rotationPointY),
					-(render.rotationPointZ)
				));
			inverse.rotate(render.rotateAngleZ, new Vec3d(0, 0, 1));
			inverse.rotate(render.rotateAngleY, new Vec3d(0, 1, 0));
			inverse.rotate(render.rotateAngleX, new Vec3d(1, 0, 0));
			inverse.translate(new Vec3d(
					-(render.offsetX),
					-(render.offsetY),
					-(render.offsetZ)
				));
		}
		
		
		addBoxes(renderer, matrix, inverse, boxes);
		if(renderer.childModels != null) {
			for (ModelRenderer child : renderer.childModels) {
				processModel(modelPos, child, boxes, scale, transform, rotate);
			}
		}
		
		rendererStack.remove(rendererStack.size()-1);
	}
	
	public void addBoxes(ModelRenderer renderer, Matrix4 matrix, Matrix4 inverse, List<Box> boxes) {
		if(renderer.cubeList != null) {
			for (ModelBox box : renderer.cubeList) {
				boxes.add( new Box(matrix, inverse, box.posX1, -box.posY2, box.posZ1, box.posX2, -box.posY1, box.posZ2) );
			}
		}
	}
	
	@SubscribeEvent
	public void damage(ItemTossEvent event) {
		if(event.getEntityItem().getEntityItem().getItem() == Items.BEEF) {
			cloths.clear();
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	@SubscribeEvent
	public void drawPlayer(RenderLivingEvent.Post event) {
		if(!( event.getEntity() instanceof EntityVillager || event.getEntity() instanceof EntityPlayer))
			return;
		
		float partialTicks = GuiTickHandler.getPartialTicks();
		
		models.put(event.getEntity(), ImmutableList.of());//getBoxes(event.getEntity().getPositionVector(), event.getRenderer().getMainModel(), event.getEntity().renderYawOffset));
		
		if(!cloths.containsKey(event.getEntity())) {
			Vec3d[] shoulderPoints = new Vec3d[] {
					new Vec3d( 0.4, 1.5,  0),
					new Vec3d( 0.25, 1.5, -0.25),
					new Vec3d( 0,    1.5, -0.25),
					new Vec3d(-0.25, 1.5, -0.25),
					new Vec3d(-0.4, 1.5,  0)
			};
			
			Matrix4 matrix = new Matrix4();
			matrix.translate(event.getEntity().getPositionVector());
			matrix.rotate(Math.toRadians( event.getEntity().renderYawOffset), new Vec3d(0, -1, 0));
			
			for (int i = 0; i < shoulderPoints.length; i++) {
				shoulderPoints[i] = matrix.apply(shoulderPoints[i]);
			}
			
			cloths.put(event.getEntity(), new Cloth(
					shoulderPoints,
					10,
					new Vec3d(0, 0.2, 0)
			) );
		}
		
		
		Cloth c = cloths.get(event.getEntity());
		
		Tessellator tess = Tessellator.getInstance();
		VertexBuffer vb = tess.getBuffer();
		
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		GlStateManager.translate(event.getX(), event.getY(), event.getZ());
        GlStateManager.translate(
        		-(event.getEntity().posX - event.getEntity().lastTickPosX) * partialTicks,
        		-(event.getEntity().posY - event.getEntity().lastTickPosY) * partialTicks,
        		-(event.getEntity().posZ - event.getEntity().lastTickPosZ) * partialTicks
        		);
		GlStateManager.translate(-event.getEntity().lastTickPosX, -event.getEntity().lastTickPosY, -event.getEntity().lastTickPosZ);
		
		
		GlStateManager.disableTexture2D();
		GlStateManager.glLineWidth(1f);
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.color(1, 1, 1, 0.75f);
		
		vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		for (Link link : c.links) {
			vecPos(vb, link.a.origPos, link.a.pos, partialTicks).endVertex();
			vecPos(vb, link.b.origPos, link.b.pos, partialTicks).endVertex();
		}
		tess.draw();

		Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(Wizardry.MODID, "textures/cape.png"));
		GlStateManager.enableTexture2D();
		
		vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		
		for (int x = 0; x < c.masses.length-1; x++) {
            for (int y = 0; y < c.masses[x].length-1; y++) {
            	float minU = (float)x/(float)(c.masses.length-1);
            	float minV = (float)y/(float)(c.masses[x].length-1);
            	float maxU = (float)(x+1)/(float)(c.masses.length-1);
            	float maxV = (float)(y+1)/(float)(c.masses[x].length-1);
            	
            	PointMass3D mass = c.masses[x][y];
            	vecPos(vb, mass.origPos, mass.pos, partialTicks).tex(minU, minV).endVertex();
            	
            	mass = c.masses[x+1][y];
            	vecPos(vb, mass.origPos, mass.pos, partialTicks).tex(maxU, minV).endVertex();
            	
            	mass = c.masses[x+1][y+1];
            	vecPos(vb, mass.origPos, mass.pos, partialTicks).tex(maxU, maxV).endVertex();
            	
            	mass = c.masses[x][y+1];
            	vecPos(vb, mass.origPos, mass.pos, partialTicks).tex(minU, maxV).endVertex();
            }
		}
		tess.draw();
		
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
	
	VertexBuffer vecPos(VertexBuffer vb, Vec3d lastTick, Vec3d pos, float partialTicks) {
		if(lastTick == null)
			lastTick = pos;
		if(pos == null)
			pos = lastTick;
		return vb.pos(
				lastTick.xCoord + (pos.xCoord - lastTick.xCoord) * partialTicks,
				lastTick.yCoord + (pos.yCoord - lastTick.yCoord) * partialTicks,
				lastTick.zCoord + (pos.zCoord - lastTick.zCoord) * partialTicks
			);
	}
}
