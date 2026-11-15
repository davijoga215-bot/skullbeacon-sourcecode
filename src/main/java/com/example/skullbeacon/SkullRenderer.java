package com.example.skullbeacon;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class SkullRenderer {
    private static final Identifier TEXTURE =
            Identifier.of(SkullBeaconClient.MOD_ID, "textures/misc/skull.png");

    private SkullRenderer() {}

    public static void register() {
        WorldRenderEvents.LAST.register(SkullRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        SkullBeaconConfig cfg = SkullBeaconConfig.get();
        Vec3d target = SkullBeaconClient.skullPos;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!cfg.enabled || target == null || mc.world == null || mc.player == null) return;
        if (!SkullBeaconClient.dimensionId(mc.world).equals(SkullBeaconClient.skullDimension)) return;

        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();

        // Balanço suave (sobe e desce), só se estiver ligado nas configurações
        float time = mc.world.getTime() + ctx.tickCounter().getTickDelta(false);
        double bob = cfg.bobbing ? Math.sin(time * 0.1) * 0.15 : 0.0;

        double dx = target.x - cam.x;
        double dy = target.y + 1.2 + bob - cam.y;
        double dz = target.z - cam.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Cresce um pouco com a distância para continuar visível de longe,
        // multiplicado pelo tamanho escolhido nas configurações
        float half = (float) MathHelper.clamp(0.35 + dist * 0.02, 0.35, 2.0)
                * (cfg.skullSize / 100f);

        MatrixStack matrices = ctx.matrixStack();
        matrices.push();
        matrices.translate(dx, dy, dz);
        matrices.multiply(camera.getRotation()); // billboard: sempre de frente para a câmera
        Matrix4f m = matrices.peek().getPositionMatrix();

        if (cfg.seeThroughWalls) {
            RenderSystem.disableDepthTest(); // visível através de paredes
        } else {
            RenderSystem.enableDepthTest();  // paredes escondem a caveira
        }
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram); // sem lightmap = brilho próprio
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, -half, -half, 0).texture(0f, 1f).color(255, 255, 255, 255);
        buf.vertex(m,  half, -half, 0).texture(1f, 1f).color(255, 255, 255, 255);
        buf.vertex(m,  half,  half, 0).texture(1f, 0f).color(255, 255, 255, 255);
        buf.vertex(m, -half,  half, 0).texture(0f, 0f).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Distância junto da caveira (somente se escolhido nas configurações)
        if (cfg.distanceMode == SkullBeaconConfig.DistanceMode.SKULL) {
            int blocks = (int) Math.round(mc.player.getPos().distanceTo(target));
            Text label = Text.translatable("skullbeacon.distance", blocks);

            float ts = 0.025f * (half / 0.4f); // o texto acompanha o tamanho da caveira
            matrices.push();
            matrices.scale(ts, -ts, ts);
            Matrix4f tm = matrices.peek().getPositionMatrix();

            float x = -mc.textRenderer.getWidth(label) / 2f;
            float y = half / ts + 2f;
            VertexConsumerProvider.Immediate consumers =
                    mc.getBufferBuilders().getEntityVertexConsumers();
            TextRenderer.TextLayerType layer = cfg.seeThroughWalls
                    ? TextRenderer.TextLayerType.SEE_THROUGH
                    : TextRenderer.TextLayerType.NORMAL;
            mc.textRenderer.draw(label, x, y, 0xFFFFFFFF, false, tm, consumers,
                    layer, 0x60000000, 0xF000F0);
            consumers.draw();
            matrices.pop();
        }

        // Restaura o estado do OpenGL
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        matrices.pop();
    }
}
