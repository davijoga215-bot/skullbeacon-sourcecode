package com.example.skullbeacon;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** Mostra na tela (HUD) quantos blocos faltam até a caveira. */
public final class SkullHud {
    private SkullHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(SkullHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        SkullBeaconConfig cfg = SkullBeaconConfig.get();
        Vec3d target = SkullBeaconClient.skullPos;
        String dimension = SkullBeaconClient.skullDimension;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!cfg.enabled || cfg.distanceMode != SkullBeaconConfig.DistanceMode.HUD || target == null || dimension == null
                || mc.world == null || mc.player == null || mc.options.hudHidden) {
            return;
        }

        Text text;
        if (SkullBeaconClient.dimensionId(mc.world).equals(dimension)) {
            int blocks = (int) Math.round(mc.player.getPos().distanceTo(target));
            text = Text.translatable("skullbeacon.hud.distance", blocks);
        } else {
            // A caveira está em outra dimensão: avisa qual
            text = Text.translatable("skullbeacon.hud.other_dimension",
                    SkullBeaconClient.dimensionText(dimension));
        }

        context.drawCenteredTextWithShadow(mc.textRenderer, text,
                context.getScaledWindowWidth() / 2, 8, 0xFFFFFF);
    }
}
