package me.cg360.mod.bridging.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.cg360.mod.bridging.BridgingMod;
import me.cg360.mod.bridging.raytrace.BridgingStateTracker;
import me.cg360.mod.bridging.raytrace.PathTraversalHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

public class Render {

    public static void blocksInViewPath(PoseStack poseStack, VertexConsumer vertexConsumer, Camera camera) {
        LocalPlayer player = Minecraft.getInstance().player;

        if(player == null)
            return;

        List<BlockPos> path = PathTraversalHandler.getViewBlockPath(player);

        if(path.isEmpty())
            return;

        for(BlockPos pos: path)
            Render.cubeTrace(poseStack, vertexConsumer, camera, pos);
    }

    public static void cubeHighlight(PoseStack poseStack, VertexConsumer vertices, Camera camera, BlockPos pos) {
        Render.cubeOutline(poseStack, vertices, camera, pos, 0x260099FF);
    }

    public static void cubeTrace(PoseStack poseStack, VertexConsumer vertices, Camera camera, BlockPos pos) {
        Render.cubeOutline(poseStack, vertices, camera, pos, 0x16333333);
    }

    public static void cubeTermination(PoseStack poseStack, VertexConsumer vertices, Camera camera, BlockPos pos) {
        Render.cubeOutline(poseStack, vertices, camera, pos, 0x7FFF0000);
    }

    public static void cubeOutline(PoseStack poseStack, VertexConsumer consumer, Camera camera, BlockPos pos, int argbColor) {
        PoseStack.Pose pose = poseStack.last();
        Vec3 camPos = camera.getPosition();

        double x = pos.getX() - camPos.x();
        double y = pos.getY() - camPos.y();
        double z = pos.getZ() - camPos.z();

        Shapes.block().forAllEdges((startX, startY, startZ, endX, endY, endZ) -> {
            float dx = (float)(endX - startX);
            float dy = (float)(endY - startY);
            float dz = (float)(endZ - startZ);
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);

            consumer.addVertex(pose.pose(), (float)(startX + x), (float)(startY + y), (float)(startZ + z))
                    .setColor(argbColor)
                    .setNormal(pose, dx /= length, dy /= length, dz /= length);

            consumer.addVertex(pose.pose(), (float)(endX + x), (float)(endY + y), (float)(endZ + z))
                    .setColor(argbColor)
                    .setNormal(pose, dx, dy, dz);
        });
    }


    public static void currentNonBridgingOutline(PoseStack poseStack, Camera camera, VertexConsumer vertices) {
        HitResult hit = Minecraft.getInstance().hitResult;

        // Skip non-placement hits.
        if(hit == null || hit.getType() != HitResult.Type.BLOCK)
            return;

        BlockHitResult blockHitResult = (BlockHitResult) hit;
        BlockPos hitBlock = blockHitResult.getBlockPos();
        Direction hitSide = blockHitResult.getDirection();

        BlockPos placeTarget = hitBlock.relative(hitSide);
        Player player = Minecraft.getInstance().player;

        if(player == null)
            return;

        if(!GameSupport.isHoldingPlaceable(player))
            return;

        // Avoid boxes beneath player feet - other entities should be fiiiine
        // Calculating collisions for every entity every tick just sounds messy.
        AABB placeDeadzone = new AABB(placeTarget);
        if (player.getBoundingBox().intersects(placeDeadzone))
            return;

        int outlineColour = BridgingMod.getConfig().getOutlineColour();
        Render.cubeOutline(poseStack, vertices, camera, placeTarget, outlineColour);
    }

    public static void currentBridgingOutline(PoseStack poseStack, Camera camera, VertexConsumer vertices) {
        Tuple<BlockPos, Direction> lastTarget = BridgingStateTracker.getLastTickTarget();

        if(lastTarget == null)
            return;

        int outlineColour = BridgingMod.getConfig().getOutlineColour();

        Render.cubeOutline(poseStack, vertices, camera, lastTarget.getA(), outlineColour);
    }

}
