package dev.arsmisc;

import com.hollingsworth.arsnouveau.common.block.tile.PortalTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.gametest.*;
import java.util.ArrayList;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class PortalDestinationGameTests {
    private static PortalTile portal() {return new PortalTile(BlockPos.ZERO,BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());}
    private static String key(Component line) {return ((TranslatableContents)line.getContents()).getKey();}
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void nativeTooltipRetainsNameAndReadsSyncedDestination(GameTestHelper h) {
        var original=portal();original.displayName="Home";original.dimID="minecraft:the_nether";
        original.warpPos=new BlockPos(-123,72,456);
        var synced=portal();synced.loadWithComponents(original.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        var lines=new ArrayList<Component>();synced.getTooltip(lines);
        h.assertTrue(lines.size()==3&&lines.getFirst().getString().equals("Home"),"Native portal name was lost or destination tooltip not injected");
        var dimension=(TranslatableContents)lines.get(1).getContents();
        h.assertTrue(dimension.getKey().equals("tooltip.ars_misc.portal.destination")
                &&key((Component)dimension.getArgs()[0]).equals("tooltip.ars_misc.portal.dimension.the_nether"),"Wrong dimension tooltip");
        var coordinates=(TranslatableContents)lines.get(2).getContents();
        h.assertTrue(coordinates.getKey().equals("tooltip.ars_misc.portal.coordinates")
                &&java.util.Arrays.equals(coordinates.getArgs(),new Object[]{-123,72,456}),"Synced coordinates were lost");
        h.assertTrue(!BlockRegistry.PORTAL_BLOCK.get().defaultBlockState().getShape(h.getLevel(),BlockPos.ZERO).isEmpty(),"Portal has no outline for native crosshair targeting");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void unboundAndModdedDestinationsRemainReadable(GameTestHelper h) {
        var portal=portal();var lines=new ArrayList<Component>();portal.getTooltip(lines);
        h.assertTrue(key(lines.getLast()).equals("tooltip.ars_misc.portal.unbound"),"Unbound portal showed fabricated destination");
        portal.dimID="example:moon";portal.warpPos=new BlockPos(1,90,-2);lines.clear();portal.getTooltip(lines);
        var dimension=(TranslatableContents)lines.getFirst().getContents();
        h.assertTrue(((Component)dimension.getArgs()[0]).getString().equals("example:moon"),"Unknown dimension ID was not preserved");
        h.assertTrue(lines.size()==2,"Missing name prevented dimension and coordinates from being shown");h.succeed();
    }
}
