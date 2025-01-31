package me.desht.pneumaticcraft.datagen;

import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.common.core.ModLootModifiers;
import me.desht.pneumaticcraft.common.loot.PNCDungeonLootModifier;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;
import org.apache.commons.lang3.Validate;

public class ModGLMProvider extends GlobalLootModifierProvider {
    public ModGLMProvider(DataGenerator gen) {
        super(gen, Names.MOD_ID);
    }

    @Override
    protected void start() {
        add("dungeon_loot", ModLootModifiers.DUNGEON_LOOT.get(), new PNCDungeonLootModifier(new LootItemCondition[]{
                getList(new String[]{
                        "simple_dungeon", "jungle_temple", "abandoned_mineshaft", "bastion_treasure",
                        "desert_pyramid", "end_city_treasure", "ruined_portal", "pillager_outpost",
                        "nether_bridge", "stronghold_corridor", "stronghold_crossing", "stronghold_library",
                        "woodland_mansion", "underwater_ruin_big", "underwater_ruin_small", "shipwreck_treasure"
                })
        }));
    }

    private LootItemCondition getList(String[] chests) {
        Validate.isTrue(chests.length > 0);
        LootItemCondition.Builder condition = null;
        for (String s : chests) {
            LootTableIdCondition.Builder b = LootTableIdCondition.builder(new ResourceLocation("chests/" + s));
            condition = condition == null ? b : condition.or(b);
        }
        return condition.build();
    }
}
