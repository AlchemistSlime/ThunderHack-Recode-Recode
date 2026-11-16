package thunder.hack.gui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.api.IAddon;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.UiKit;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.ThunderUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static thunder.hack.features.modules.Module.mc;

public class MainMenuScreen extends Screen {
    private final List<MainMenuButton> buttons = new ArrayList<>();
    public boolean confirm = false;
    public static int ticksActive;
    private long openedAt = System.currentTimeMillis();

    protected MainMenuScreen() {
        super(Text.of("THMainMenuScreen"));
        INSTANCE = this;

        buttons.add(new MainMenuButton(-100, -80, 200, 28, I18n.translate("menu.singleplayer").toUpperCase(Locale.ROOT), () -> mc.setScreen(new SelectWorldScreen(this))));
        buttons.add(new MainMenuButton(-100, -46, 200, 28, I18n.translate("menu.multiplayer").toUpperCase(Locale.ROOT), () -> mc.setScreen(new MultiplayerScreen(this))));
        buttons.add(new MainMenuButton(-100, -12, 200, 28, I18n.translate("menu.options")
                .toUpperCase(Locale.ROOT)
                .replace(".", ""), () -> mc.setScreen(new OptionsScreen(this, mc.options))));
        buttons.add(new MainMenuButton(-100, 22, 200, 28, "CLICKGUI", () -> ModuleManager.clickGui.setGui()));
        buttons.add(new MainMenuButton(-100, 56, 200, 28, I18n.translate("menu.quit").toUpperCase(Locale.ROOT), mc::scheduleStop));
    }

    private static MainMenuScreen INSTANCE = new MainMenuScreen();

    public static MainMenuScreen getInstance() {
        ticksActive = 0;

        if (INSTANCE == null) {
            INSTANCE = new MainMenuScreen();
        }
        return INSTANCE;
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        ticksActive++;

        if (ticksActive > 400) {
            ticksActive = 0;
        }
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        float halfOfWidth = mc.getWindow().getScaledWidth() / 2f;
        float halfOfHeight = mc.getWindow().getScaledHeight() / 2f;

        float mainX = halfOfWidth - 120f;
        float mainY = halfOfHeight - 92f;
        float mainWidth = 240f;
        float mainHeight = 184f;

        long since = System.currentTimeMillis() - openedAt;
        float cardA = UiKit.easeOutCubic(since / 500f);
        float logoA = UiKit.easeOutCubic((since - 60) / 600f);
        double t = System.currentTimeMillis() / 1000.0;

        UiKit.menuBackdrop(context.getMatrices(), halfOfWidth * 2f, halfOfHeight * 2f);

        // свечение под карточкой
        UiKit.glow(context.getMatrices(), mainX + 10, mainY + 10, mainWidth - 20, mainHeight - 20, 30, UiKit.alpha(UiKit.accent(0), 0.35f * cardA));

        float cardY = mainY + (1f - cardA) * 14f;
        UiKit.glass(context.getMatrices(), mainX, cardY, mainWidth, mainHeight, 14f, cardA);
        UiKit.accentLine(context.getMatrices(), mainX + 22, cardY + 1f, mainWidth - 44, 1.5f, 0.9f * cardA);

        for (int i = 0; i < buttons.size(); i++) {
            float a = UiKit.easeOutCubic((since - 140 - i * 70L) / 420f);
            buttons.get(i).onRender(context, mouseX, mouseY, a);
        }

        // логотип
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * 1.6));
        UiKit.glow(context.getMatrices(), halfOfWidth - 70, halfOfHeight - 124, 140, 22, 26, UiKit.alpha(UiKit.accent(90), (0.25f + 0.25f * pulse) * logoA));

        FontRenderers.thglitchBig.drawCenteredString(context.getMatrices(), "THUNDERHACK", (int) halfOfWidth, (int) (halfOfHeight - 132),
                new Color(255, 255, 255, (int) (235 * logoA)).getRGB());
        // версия и авторы
        FontRenderers.sf_medium.drawString(context.getMatrices(), "ThunderHack Recode Recode v" + ThunderHack.VERSION, 10, halfOfHeight * 2 - 22, Render2DEngine.applyOpacity(-1, 0.45f));
        FontRenderers.sf_medium.drawString(context.getMatrices(), "Authors: Alchemist Slime, Pan4ur, 06ED", 10, halfOfHeight * 2 - 12, Render2DEngine.applyOpacity(-1, 0.3f));

        int totalAddonsLoaded = Managers.ADDON.getTotalAddons();
        String addonsText = "Addons Loaded: " + totalAddonsLoaded;
        int screenWidth = mc.getWindow().getScaledWidth();
        int textWidth = (int) FontRenderers.sf_bold.getStringWidth(addonsText);
        int textX = screenWidth - textWidth - 5;
        FontRenderers.sf_bold.drawString(context.getMatrices(), addonsText, textX, 5, Color.WHITE.getRGB());

        int offset = 0;
        for (IAddon addon : Managers.ADDON.getAddons()) {
            // for (String addon : Arrays.asList("Addon", "Addon2", "Addon3", "Addon4", "Addon5")) {
            textWidth = (int) FontRenderers.sf_bold.getStringWidth(addon.getName() + " |");
            textX = screenWidth - textWidth - 5;
            FontRenderers.sf_bold.drawString(context.getMatrices(), addon.getName() + Formatting.WHITE + " |", textX, 13 + offset, Color.GRAY.getRGB());
            offset += 9;
        }
    }

    private static @NotNull String getPrefix(@NotNull String change) {
        String prefix = "";
        if (change.contains("[+]")) {
            change = change.replace("[+] ", "");
            prefix = Formatting.GREEN + "[+] " + Formatting.RESET;
        } else if (change.contains("[-]")) {
            change = change.replace("[-] ", "");
            prefix = Formatting.RED + "[-] " + Formatting.RESET;
        } else if (change.contains("[/]")) {
            change = change.replace("[/] ", "");
            prefix = Formatting.LIGHT_PURPLE + "[/] " + Formatting.RESET;
        } else if (change.contains("[*]")) {
            change = change.replace("[*] ", "");
            prefix = Formatting.GOLD + "[*] " + Formatting.RESET;
        }
        return prefix + change;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float halfOfWidth = mc.getWindow().getScaledWidth() / 2f;
        float halfOfHeight = mc.getWindow().getScaledHeight() / 2f;
        buttons.forEach(b -> b.onClick((int) mouseX, (int) mouseY));

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
