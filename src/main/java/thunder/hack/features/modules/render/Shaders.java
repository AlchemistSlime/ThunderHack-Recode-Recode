package thunder.hack.features.modules.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ShaderManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;

// Шейдеры для рук и существ: Default, Smoke, Gradient, Snow, Fade
public class Shaders extends Module {
    public Shaders() {
        super("Shaders", Category.RENDER);
    }

    private final Setting<SettingGroup> select = new Setting<>("Select", new SettingGroup(false, 0));
    public final Setting<Boolean> hands = new Setting<>("Hands", true).addToGroup(select);
    private final Setting<Boolean> players = new Setting<>("Players", false).addToGroup(select);
    private final Setting<Boolean> self = new Setting<>("Self", true, v -> players.getValue()).addToGroup(select);
    private final Setting<Boolean> friends = new Setting<>("Friends", false).addToGroup(select);
    private final Setting<Boolean> crystals = new Setting<>("Crystals", false).addToGroup(select);
    private final Setting<Boolean> creatures = new Setting<>("Creatures", false).addToGroup(select);
    private final Setting<Boolean> monsters = new Setting<>("Monsters", false).addToGroup(select);
    private final Setting<Boolean> ambients = new Setting<>("Ambients", false).addToGroup(select);
    private final Setting<Boolean> items = new Setting<>("Items", false).addToGroup(select);
    private final Setting<Boolean> others = new Setting<>("Others", false).addToGroup(select);

    public final Setting<ShaderManager.Shader> mode = new Setting<>("Mode", ShaderManager.Shader.Default);
    public final Setting<ShaderManager.Shader> handsMode = new Setting<>("HandsMode", ShaderManager.Shader.Default);

    public final Setting<Integer> maxRange = new Setting<>("MaxRange", 64, 16, 256);
    public final Setting<Float> factor = new Setting<>("GradientFactor", 2f, 0f, 20f, v -> mode.is(ShaderManager.Shader.Gradient) || handsMode.is(ShaderManager.Shader.Gradient));
    public final Setting<Float> gradient = new Setting<>("Gradient", 2f, 0f, 20f, v -> mode.is(ShaderManager.Shader.Gradient) || handsMode.is(ShaderManager.Shader.Gradient));
    public final Setting<Integer> alpha2 = new Setting<>("GradientAlpha", 170, 0, 255, v -> mode.is(ShaderManager.Shader.Gradient) || handsMode.is(ShaderManager.Shader.Gradient));
    public final Setting<Integer> lineWidth = new Setting<>("LineWidth", 2, 0, 20);
    public final Setting<Integer> quality = new Setting<>("Quality", 3, 0, 6);
    public final Setting<Integer> octaves = new Setting<>("SmokeOctaves", 10, 5, 30);
    public final Setting<Integer> fillAlpha = new Setting<>("FillAlpha", 170, 0, 255);
    public final Setting<Boolean> glow = new Setting<>("Glow", true);

    private final Setting<SettingGroup> colors = new Setting<>("Colors", new SettingGroup(false, 0));
    public final Setting<ColorSetting> outlineColor = new Setting<>("Outline", new ColorSetting(0x88FF1E1E)).addToGroup(colors);
    public final Setting<ColorSetting> outlineColor1 = new Setting<>("SmokeOutline", new ColorSetting(0x88FF7070), v -> mode.is(ShaderManager.Shader.Smoke) || handsMode.is(ShaderManager.Shader.Smoke)).addToGroup(colors);
    public final Setting<ColorSetting> outlineColor2 = new Setting<>("SmokeOutline2", new ColorSetting(0x88900000), v -> mode.is(ShaderManager.Shader.Smoke) || handsMode.is(ShaderManager.Shader.Smoke)).addToGroup(colors);
    public final Setting<ColorSetting> fillColor1 = new Setting<>("Fill", new ColorSetting(0x88FF1E1E)).addToGroup(colors);
    public final Setting<ColorSetting> fillColor2 = new Setting<>("Fill2", new ColorSetting(0x88900000), v -> mode.is(ShaderManager.Shader.Smoke) || handsMode.is(ShaderManager.Shader.Smoke) || mode.is(ShaderManager.Shader.Fade) || handsMode.is(ShaderManager.Shader.Fade)).addToGroup(colors);
    public final Setting<ColorSetting> fillColor3 = new Setting<>("Fill3", new ColorSetting(0x88FF7070), v -> mode.is(ShaderManager.Shader.Smoke) || handsMode.is(ShaderManager.Shader.Smoke)).addToGroup(colors);

    /** Выбрано ли хоть одно существо (иначе буфер для существ не нужен). */
    public boolean hasEntityTargets() {
        return players.getValue() || friends.getValue() || crystals.getValue() || creatures.getValue()
                || monsters.getValue() || ambients.getValue() || items.getValue() || others.getValue();
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null || mc.player == null)
            return false;

        if (mc.player.squaredDistanceTo(entity.getPos()) > maxRange.getPow2Value())
            return false;

        if (entity instanceof PlayerEntity) {
            if (entity == mc.player)
                return players.getValue() && self.getValue();
            if (Managers.FRIEND.isFriend((PlayerEntity) entity))
                return friends.getValue();
            return players.getValue();
        }

        if (entity instanceof EndCrystalEntity)
            return crystals.getValue();

        if (entity instanceof ItemEntity)
            return items.getValue();

        return switch (entity.getType().getSpawnGroup()) {
            case CREATURE, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> creatures.getValue();
            case MONSTER -> monsters.getValue();
            case AMBIENT, WATER_AMBIENT -> ambients.getValue();
            default -> others.getValue();
        };
    }
}
