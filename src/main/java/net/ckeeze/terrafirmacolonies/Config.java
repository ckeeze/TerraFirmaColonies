package net.ckeeze.terrafirmacolonies;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Config {
    static final Pair<Config, ForgeConfigSpec> instance = new ForgeConfigSpec.Builder().configure(Config::new);

    public final ConfigValue<List<? extends String>> stylepackBlockReplacement;

    public Config(ForgeConfigSpec.Builder builder) {
        stylepackBlockReplacement = builder.defineListAllowEmpty("stylepack_block_replacement", List.of(
            "tfc:dead_torch=>tfc:torch",
            "tfc:dead_wall_torch=>tfc:wall_torch"
        ), e -> e instanceof String s && s.contains("=>"));
    }

    private static Map<Block, Replacer> stylepackBlockReplacementCache;

    public static Map<Block, Replacer> getStylepackBlockReplacements() {
        if (stylepackBlockReplacementCache == null) {
            stylepackBlockReplacementCache = new HashMap<>();
            RegistryLookup<Block> blockLookup = BuiltInRegistries.BLOCK.asLookup();
            List<? extends String> rawEntries = instance.getLeft().stylepackBlockReplacement.get();
            for (String e : rawEntries) {
                String[] split = e.split("=>");
                if (split.length == 2) {
                    try {
                        BlockState to = BlockStateParser.parseForBlock(blockLookup, split[1], true).blockState();
                        if (split[0].contains("[")) {
                            BlockState from = BlockStateParser.parseForBlock(blockLookup, split[0], true).blockState();
                            stylepackBlockReplacementCache.put(from.getBlock(), new OneToOneReplacer(from, to));
                        } else {
                            Block from = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(split[0]));
                            stylepackBlockReplacementCache.put(from, new AnyToOneReplacer(to));
                        }
                    } catch (CommandSyntaxException exception) {
                        System.out.println("failed to parse blockstate pair from config: " + e);
                        exception.printStackTrace();
                    }
                }
            }
        }
        return stylepackBlockReplacementCache;

    }

    public interface Replacer extends Function<BlockState, BlockState> {
    }

    public static class AnyToOneReplacer implements Replacer {

        public final BlockState to;

        public AnyToOneReplacer(BlockState to) {
            this.to = to;
        }

        @Override
        public BlockState apply(BlockState prev) {
            return to;
        }
    }

    public static class OneToOneReplacer implements Replacer {
        public final BlockState from;
        public final BlockState to;

        public OneToOneReplacer(BlockState from, BlockState blockState) {
            this.from = from;
            to = blockState;
        }

        @Override
        public BlockState apply(BlockState prev) {
            if (prev == this.from)
                return to;
            else
                return prev;
        }
    }


}
