package com.example.skullbeacon;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.IntConsumer;

/** Tela de configurações feita só com widgets do próprio Minecraft. */
public class SkullBeaconConfigScreen extends Screen {
    private final Screen parent;
    private final SkullBeaconConfig cfg = SkullBeaconConfig.get();

    public SkullBeaconConfigScreen(Screen parent) {
        super(Text.translatable("skullbeacon.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int colW = 150;
        int left = width / 2 - 155;
        int right = width / 2 + 5;
        int y = height / 6;
        int step = 24;

        // Linha 1
        addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.enabled)
                .build(left, y, colW, 20, Text.translatable("skullbeacon.config.enabled"),
                        (button, value) -> cfg.enabled = value));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.showDeathMessage)
                .build(right, y, colW, 20, Text.translatable("skullbeacon.config.death_message"),
                        (button, value) -> cfg.showDeathMessage = value));

        // Linha 2
        addDrawableChild(CyclingButtonWidget.<SkullBeaconConfig.DistanceMode>builder(
                        mode -> Text.translatable("skullbeacon.config.distance_mode."
                                + mode.name().toLowerCase(Locale.ROOT)))
                .values(SkullBeaconConfig.DistanceMode.values())
                .initially(cfg.distanceMode)
                .build(left, y + step, colW, 20, Text.translatable("skullbeacon.config.distance_mode"),
                        (button, value) -> cfg.distanceMode = value));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.seeThroughWalls)
                .build(right, y + step, colW, 20, Text.translatable("skullbeacon.config.see_through_walls"),
                        (button, value) -> cfg.seeThroughWalls = value));

        // Linha 3
        addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.bobbing)
                .build(left, y + step * 2, colW, 20, Text.translatable("skullbeacon.config.bobbing"),
                        (button, value) -> cfg.bobbing = value));
        addDrawableChild(new IntSlider(right, y + step * 2, colW, 20,
                SkullBeaconConfig.MIN_SKULL_SIZE, SkullBeaconConfig.MAX_SKULL_SIZE, cfg.skullSize,
                "skullbeacon.config.skull_size", value -> cfg.skullSize = value));

        // Linha 4
        addDrawableChild(new IntSlider(left, y + step * 3, 310, 20,
                SkullBeaconConfig.MIN_HIDE_DISTANCE, SkullBeaconConfig.MAX_HIDE_DISTANCE, cfg.hideDistance,
                "skullbeacon.config.hide_distance", value -> cfg.hideDistance = value));

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(width / 2 - 100, y + step * 4 + 8, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        SkullBeaconConfig.save();
    }

    /** Slider de números inteiros com texto traduzível ("... %s ..."). */
    private static final class IntSlider extends SliderWidget {
        private final int min;
        private final int max;
        private final String translationKey;
        private final IntConsumer setter;

        IntSlider(int x, int y, int width, int height, int min, int max, int initial,
                  String translationKey, IntConsumer setter) {
            super(x, y, width, height, Text.empty(), (initial - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.translationKey = translationKey;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable(translationKey, current()));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
