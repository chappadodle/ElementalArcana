package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A crypt's colours on its dark stone (see the Arcane Crypts spec): a trim block, a light block and
 * the colour of its candles, by element. The stone itself is the same deepslate for every crypt.
 */
record CryptPalette(BlockState trim, BlockState light, Block candle) {

    static CryptPalette of(Element element) {
        return switch (element) {
            case FIRE -> new CryptPalette(Blocks.RED_NETHER_BRICKS.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.ORANGE_CANDLE);
            case WATER -> new CryptPalette(Blocks.DARK_PRISMARINE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(), Blocks.BLUE_CANDLE);
            case ICE -> new CryptPalette(Blocks.PACKED_ICE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(), Blocks.LIGHT_BLUE_CANDLE);
            case WIND -> new CryptPalette(Blocks.CALCITE.defaultBlockState(), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), Blocks.CYAN_CANDLE);
            case EARTH -> new CryptPalette(Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.VERDANT_FROGLIGHT.defaultBlockState(), Blocks.BROWN_CANDLE);
            case CRYSTAL -> new CryptPalette(Blocks.AMETHYST_BLOCK.defaultBlockState(), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), Blocks.PURPLE_CANDLE);
            case LIGHTNING -> new CryptPalette(Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState(), Blocks.OCHRE_FROGLIGHT.defaultBlockState(),
                    Blocks.YELLOW_CANDLE);
            case RADIANCE -> new CryptPalette(Blocks.QUARTZ_BRICKS.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState(), Blocks.WHITE_CANDLE);
        };
    }

    /** {@code count} lit candles of the crypt's colour. */
    BlockState candles(int count) {
        return candle.defaultBlockState().setValue(CandleBlock.CANDLES, Math.clamp(count, 1, 4)).setValue(CandleBlock.LIT, true);
    }
}
