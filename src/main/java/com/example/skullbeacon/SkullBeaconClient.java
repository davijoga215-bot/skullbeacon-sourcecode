package com.example.skullbeacon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public class SkullBeaconClient implements ClientModInitializer {
    public static final String MOD_ID = "skullbeacon";

    /** Local da última morte (null = nenhuma caveira ativa). */
    public static Vec3d skullPos = null;
    /** Id da dimensão da caveira, por exemplo "minecraft:overworld". */
    public static String skullDimension = null;

    private boolean wasDead = false;
    private String currentKey = null; // identifica o mundo/servidor atual
    private KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        SkullBeaconConfig.load();

        // Tecla (sem atalho por padrão) em Controles > Skull Beacon
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skullbeacon.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.skullbeacon"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // A caveira continua salva em disco; aqui só limpamos o estado em memória
            skullPos = null;
            skullDimension = null;
            wasDead = false;
            currentKey = null;
        });
        SkullRenderer.register();
        SkullHud.register();
    }

    private void onTick(MinecraftClient client) {
        while (openConfigKey.wasPressed()) {
            client.setScreen(new SkullBeaconConfigScreen(client.currentScreen));
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            wasDead = false;
            return;
        }

        // Ao entrar em um mundo/servidor, carrega a caveira salva dele
        if (currentKey == null) {
            String key = worldKey(client);
            if (key != null) {
                currentKey = key;
                SkullStore.Entry saved = SkullStore.get(key);
                if (saved != null) {
                    skullPos = new Vec3d(saved.x, saved.y, saved.z);
                    skullDimension = saved.dimension;
                } else {
                    skullPos = null;
                    skullDimension = null;
                }
            }
        }

        boolean dead = player.isDead();
        if (dead && !wasDead) {
            onDeath(client, player);
        }
        wasDead = dead;

        // Some quando o jogador chega perto da caveira
        if (!dead && skullPos != null
                && dimensionId(client.world).equals(skullDimension)) {
            double hide = SkullBeaconConfig.get().hideDistance;
            if (player.getPos().squaredDistanceTo(skullPos) < hide * hide) {
                skullPos = null;
                skullDimension = null;
                if (currentKey != null) SkullStore.remove(currentKey);
            }
        }
    }

    private void onDeath(MinecraftClient client, ClientPlayerEntity player) {
        if (!SkullBeaconConfig.get().enabled) return;

        Vec3d pos = player.getPos();
        int x = MathHelper.floor(pos.x);
        int y = MathHelper.floor(pos.y);
        int z = MathHelper.floor(pos.z);
        String dim = dimensionId(client.world);
        Text dimText = dimensionText(dim);

        GameMode mode = client.interactionManager != null
                ? client.interactionManager.getCurrentGameMode()
                : GameMode.SURVIVAL;
        boolean creative = mode == GameMode.CREATIVE;

        Text message;
        if (creative) {
            // Somente no Criativo: mensagem clicável que executa /tp @s X Y Z
            Style style = Style.EMPTY
                    .withColor(Formatting.AQUA)
                    .withUnderline(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/tp @s " + x + " " + y + " " + z))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Text.translatable("skullbeacon.message.hover")));
            message = Text.translatable("skullbeacon.message.teleport", dimText, x, y, z)
                    .setStyle(style);
        } else {
            // Survival (inclusive OP): apenas informativa, sem teleporte
            message = Text.translatable("skullbeacon.message.info", dimText, x, y, z)
                    .formatted(Formatting.RED);
        }
        if (SkullBeaconConfig.get().showDeathMessage) {
            client.inGameHud.getChatHud().addMessage(message);
        }

        // Caveira apenas em Sobrevivência (Survival/Adventure)
        if (mode.isSurvivalLike()) {
            skullPos = pos;
            skullDimension = dim;
            if (currentKey != null) SkullStore.put(currentKey, dim, pos);
        }
    }

    static String dimensionId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    static Text dimensionText(String id) {
        return switch (id) {
            case "minecraft:overworld" -> Text.translatable("skullbeacon.dimension.overworld");
            case "minecraft:the_nether" -> Text.translatable("skullbeacon.dimension.the_nether");
            case "minecraft:the_end" -> Text.translatable("skullbeacon.dimension.the_end");
            default -> Text.literal(id); // dimensões de outros mods
        };
    }

    /** "sp:<pasta do mundo>" no singleplayer, "mp:<endereço>" em servidores. */
    private static String worldKey(MinecraftClient client) {
        IntegratedServer server = client.getServer();
        if (server != null) {
            Path name = server.getSavePath(WorldSavePath.ROOT).normalize().getFileName();
            return name != null ? "sp:" + name : null;
        }
        ServerInfo info = client.getCurrentServerEntry();
        return info != null ? "mp:" + info.address : null;
    }
}
