package com.raven.client.features.mining;

import com.raven.client.features.Feature;
import com.raven.client.features.FeatureCategory;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

import org.lwjgl.opengl.GL11;

public class EfficientMinerHeatmap extends Feature {

    private Minecraft mc;
    private static final Set<Block> TARGET_BLOCKS = new HashSet<>();
    
    private Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }

    public EfficientMinerHeatmap() {
        super("Efficient Miner Heatmap", FeatureCategory.MINING);
        // Add valid block targets
        TARGET_BLOCKS.add(Blocks.coal_ore); // Replace with actual mithril, tungsten, umber blocks if custom
        TARGET_BLOCKS.add(Blocks.iron_ore);
        TARGET_BLOCKS.add(Blocks.diamond_ore);
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        Minecraft mc = getMc();
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        if (!(mc.thePlayer.getHeldItem() != null &&
              mc.thePlayer.getHeldItem().getItem() instanceof ItemPickaxe)) return;

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.getBlockPos() == null) return;

        BlockPos startPos = mop.getBlockPos();
        IBlockState state = mc.theWorld.getBlockState(startPos);
        if (!TARGET_BLOCKS.contains(state.getBlock())) return;

        List<BlockPos> cluster = findCluster(mc.theWorld, startPos, 20, 6);
        renderCluster(cluster);
        renderRayTraceTo(startPos);
    }

    private List<BlockPos> findCluster(World world, BlockPos origin, int maxBlocks, int radius) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        List<BlockPos> result = new ArrayList<>();

        IBlockState startState = world.getBlockState(origin);
        if (startState == null || !TARGET_BLOCKS.contains(startState.getBlock())) return result;

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos current = queue.poll();
            result.add(current);

            for (EnumFacing dir : EnumFacing.values()) {
                BlockPos next = current.offset(dir);
                if (!visited.contains(next) && origin.distanceSq(next) <= radius * radius) {
                    IBlockState state = world.getBlockState(next);
                    if (state != null && state.getBlock() == startState.getBlock()) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return result;
    }

    private void renderCluster(List<BlockPos> blocks) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = blocks.get(i);
            float r = 1.0f, g = i / (float) blocks.size(), b = 0.0f;
            drawBox(pos, r, g, b, 0.4f);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderRayTraceTo(BlockPos pos) {
        Minecraft mc = getMc();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        Vec3 playerPos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 targetPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        GL11.glColor4f(0f, 1f, 0f, 0.6f);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(playerPos.xCoord - mc.getRenderManager().viewerPosX,
                        playerPos.yCoord - mc.getRenderManager().viewerPosY,
                        playerPos.zCoord - mc.getRenderManager().viewerPosZ);
        GL11.glVertex3d(targetPos.xCoord - mc.getRenderManager().viewerPosX,
                        targetPos.yCoord - mc.getRenderManager().viewerPosY,
                        targetPos.zCoord - mc.getRenderManager().viewerPosZ);
        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawBox(BlockPos pos, float red, float green, float blue, float alpha) {
        Minecraft mc = getMc();
        double x = pos.getX() - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ;

        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x + 1, y, z);
        GL11.glVertex3d(x + 1, y, z);
        GL11.glVertex3d(x + 1, y + 1, z);
        GL11.glVertex3d(x + 1, y + 1, z);
        GL11.glVertex3d(x, y + 1, z);
        GL11.glVertex3d(x, y + 1, z);
        GL11.glVertex3d(x, y, z);
        GL11.glEnd();
    }
}