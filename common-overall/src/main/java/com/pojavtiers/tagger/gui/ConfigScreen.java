package com.pojavtiers.tagger.gui;

import com.pojavtiers.tagger.PojavTierManager;
import com.pojavtiers.tagger.config.PojavConfig;
import com.pojavtiers.tagger.demo.DemoProfile;
import com.pojavtiers.tagger.search.PlayerSearchScreen;
import com.pojavtiers.tagger.util.CompatUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A self-contained vanilla-widget config screen (no Cloth Config dependency).
 * <p>
 * This class lives in the version-agnostic {@code common} source set and is
 * compiled once per Minecraft-version module. The only things in it that
 * actually differ between Minecraft versions (font-styling API, keybinding
 * construction, and the texture-draw call signature) are routed through
 * {@link CompatUtil}, which each version module supplies its own real,
 * directly-compiled (non-reflection) implementation of. See CompatUtil's
 * class comment in each version module for details.
 */
public class ConfigScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("PojavTierTagger/ConfigScreen");
    private static final Integer[] REFRESH_INTERVALS = {5, 10, 15, 30, 60};
    private static final Identifier MANNEQUIN_TEXTURE =
            Identifier.of("pojavtiertagger", "textures/mannequin/default.png");
    private static final int MANNEQUIN_W = 64;
    private static final int MANNEQUIN_H = 154; // 64 * (432/180), the texture's native aspect ratio

    private final Screen parent;
    private final PojavConfig cfg = PojavConfig.get();

    private ButtonWidget secondaryGamemodeButton;

    // Laid out in init(), read back in render() so the mannequin preview and
    // its caption line up with wherever the button rows ended.
    private int mannequinTextY;
    private int mannequinImageY;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Pojav Tier Tagger Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int colW = 150;
        int gap = 4;
        int rowH = 20;
        int leftX = this.width / 2 - colW - gap / 2;
        int rightX = this.width / 2 + gap / 2;
        int y = 40;

        // Search bar, at the very top of the screen. Not a real text field -
        // it's a button styled to look like one, that opens the dedicated
        // PlayerSearchScreen sub-screen. Everything below shifts down by one
        // row's worth of space so this never overlaps or misaligns any
        // existing row.
        final int searchY = y;
        int searchW = colW * 2 + gap;
        safeAdd("playerSearch", () -> ButtonWidget.builder(
                        Text.literal("\uD83D\uDD0D Search player..."),
                        b -> {
                            if (this.client != null) this.client.setScreen(new PlayerSearchScreen(this));
                        })
                .dimensions(this.width / 2 - searchW / 2, searchY, searchW, rowH)
                .build());
        y += rowH + gap + 4;

        // Every row below is wrapped in safeAdd() so that if ONE widget fails to
        // build on a given Minecraft version, it just skips that single button and
        // logs why, instead of throwing out of init() and leaving the whole screen
        // with no buttons at all.

        final int row1Y = y;
        safeAdd("showOverallTier", () -> CyclingButtonWidget.onOffBuilder(cfg.showOverallTier)
                .build(leftX, row1Y, colW, rowH, Text.translatable("pojavtiertagger.config.showOverall"),
                        (b, v) -> cfg.showOverallTier = v));
        safeAdd("showRegion", () -> CyclingButtonWidget.onOffBuilder(cfg.showRegion)
                .build(rightX, row1Y, colW, rowH, Text.translatable("pojavtiertagger.config.showRegion"),
                        (b, v) -> cfg.showRegion = v));
        y += rowH + gap;

        final int row2Y = y;
        safeAdd("showInChat", () -> CyclingButtonWidget.onOffBuilder(cfg.showInChat)
                .build(leftX, row2Y, colW, rowH, Text.translatable("pojavtiertagger.config.showInChat"),
                        (b, v) -> cfg.showInChat = v));
        safeAdd("useBrackets", () -> CyclingButtonWidget.onOffBuilder(cfg.useBrackets)
                .build(rightX, row2Y, colW, rowH, Text.translatable("pojavtiertagger.config.bracket"),
                        (b, v) -> cfg.useBrackets = v));
        y += rowH + gap;

        final int row3Y = y;
        safeAdd("gamemode", () -> ButtonWidget.builder(gamemodeLabel(), b -> {
                    cfg.gamemode = cfg.gamemode.next();
                    b.setMessage(gamemodeLabel());
                }).dimensions(leftX, row3Y, colW, rowH).build());
        safeAdd("highestMode", () -> ButtonWidget.builder(highestModeLabel(), b -> {
                    cfg.highestMode = nextHighestMode(cfg.highestMode);
                    b.setMessage(highestModeLabel());
                }).dimensions(rightX, row3Y, colW, rowH).build());
        y += rowH + gap;

        final int row4Y = y;
        safeAdd("secondaryEnabled", () -> ButtonWidget.builder(secondaryEnabledLabel(), b -> {
                    cfg.secondaryTierEnabled = !cfg.secondaryTierEnabled;
                    b.setMessage(secondaryEnabledLabel());
                    if (secondaryGamemodeButton != null) secondaryGamemodeButton.active = cfg.secondaryTierEnabled;
                }).dimensions(leftX, row4Y, colW, rowH).build());
        safeAdd("secondaryGamemode", () -> {
            secondaryGamemodeButton = ButtonWidget.builder(secondaryGamemodeLabel(), b -> {
                        cfg.secondaryGamemode = cfg.secondaryGamemode.next();
                        b.setMessage(secondaryGamemodeLabel());
                    }).dimensions(rightX, row4Y, colW, rowH).build();
            secondaryGamemodeButton.active = cfg.secondaryTierEnabled;
            return secondaryGamemodeButton;
        });
        y += rowH + gap;

        final int refreshY = y;
        safeAdd("refreshInterval", () -> ButtonWidget.builder(refreshIntervalLabel(), b -> {
                    cfg.refreshIntervalMinutes = nextInterval(cfg.refreshIntervalMinutes);
                    b.setMessage(refreshIntervalLabel());
                }).dimensions(leftX, refreshY, colW, rowH).build());
        safeAdd("refreshNowButton", () -> ButtonWidget.builder(
                        Text.literal("Refresh now (" + PojavTierManager.size() + " loaded)"),
                        b -> PojavTierManager.refreshNow(true))
                .dimensions(rightX, refreshY, colW, rowH).build());
        y += rowH + gap + 10;

        // Mannequin preview: an offline demo player (__BlazeNYX) showing exactly
        // what the currently-configured tier badge(s) will look like, live-updating
        // as the buttons above are clicked.
        mannequinTextY = y;
        mannequinImageY = y + 14;
        int doneY = mannequinImageY + MANNEQUIN_H + 10;

        final int finalDoneY = doneY;
        safeAdd("done", () -> ButtonWidget.builder(Text.translatable("pojavtiertagger.config.done"), b -> close())
                .dimensions(this.width / 2 - 100, finalDoneY, 200, rowH).build());
    }

    /** Builds and adds a widget, catching and logging any failure instead of aborting the whole screen init. */
    private void safeAdd(String rowName, java.util.function.Supplier<net.minecraft.client.gui.widget.ClickableWidget> factory) {
        try {
            addDrawableChild(factory.get());
        } catch (Throwable t) {
            LOGGER.warn("Could not build config screen widget '{}' on this Minecraft version - skipping it. "
                    + "Other buttons will still work.", rowName, t);
        }
    }

    private Text gamemodeLabel() {
        return Text.literal("Gamemode: " + cfg.gamemode.displayName());
    }

    private Text secondaryEnabledLabel() {
        return Text.literal("2nd Tier: " + (cfg.secondaryTierEnabled ? "Enabled" : "Disabled"));
    }

    private Text secondaryGamemodeLabel() {
        return Text.literal("2nd Gamemode: " + cfg.secondaryGamemode.displayName());
    }

    private Text refreshIntervalLabel() {
        return Text.literal(nearest(cfg.refreshIntervalMinutes, REFRESH_INTERVALS) + " min");
    }

    private static PojavConfig.HighestMode nextHighestMode(PojavConfig.HighestMode m) {
        PojavConfig.HighestMode[] all = PojavConfig.HighestMode.values();
        return all[(m.ordinal() + 1) % all.length];
    }

    private static int nextInterval(int current) {
        Integer near = nearest(current, REFRESH_INTERVALS);
        int idx = java.util.Arrays.asList(REFRESH_INTERVALS).indexOf(near);
        return REFRESH_INTERVALS[(idx + 1) % REFRESH_INTERVALS.length];
    }

    private static String label(PojavConfig.HighestMode m) {
        return switch (m) {
            case NEVER -> "No rank: Show nothing";
            case IF_NONE -> "No rank: Show highest";
        };
    }

    private Text highestModeLabel() {
        return Text.literal(label(cfg.highestMode));
    }

    private static Integer nearest(int value, Integer[] options) {
        Integer best = options[0];
        int diff = Integer.MAX_VALUE;
        for (Integer o : options) {
            int d = Math.abs(o - value);
            if (d < diff) {
                diff = d;
                best = o;
            }
        }
        return best;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

        // Mannequin: same combined-tag builder the real nametag/tab/chat use, fed
        // the bundled offline demo profile instead of a live lookup - so this
        // preview always matches gamemode/highest-mode/second-tier settings exactly.
        Text tag = PojavTierManager.buildCombinedTag(DemoProfile.get(),
                Text.literal(DemoProfile.NAME).styled(s -> s.withColor(0xFFFFFF)));
        context.drawCenteredTextWithShadow(this.textRenderer, tag, this.width / 2, mannequinTextY, 0xFFFFFF);

        int imgX = this.width / 2 - MANNEQUIN_W / 2;
        CompatUtil.drawTexture(context, MANNEQUIN_TEXTURE, imgX, mannequinImageY, MANNEQUIN_W, MANNEQUIN_H);
    }

    @Override
    public void close() {
        cfg.save();
        PojavTierManager.clearCache();
        PojavTierManager.refreshNow();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
