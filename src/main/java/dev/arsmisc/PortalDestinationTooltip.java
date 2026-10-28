package dev.arsmisc;

import com.hollingsworth.arsnouveau.common.block.tile.PortalTile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Extends Ars Nouveau's native crosshair tooltip without requiring an external HUD mod. */
public final class PortalDestinationTooltip {
    public static void append(PortalTile portal,List<Component> lines) {
        if (portal.dimID!=null && !portal.dimID.isBlank()) {
            var id=ResourceLocation.tryParse(portal.dimID);
            Component dimension;
            if(id!=null && id.getNamespace().equals("minecraft") && List.of("overworld","the_nether","the_end").contains(id.getPath()))
                dimension=Component.translatable("tooltip.ars_misc.portal.dimension."+id.getPath());
            else dimension=Component.literal(portal.dimID);
            lines.add(Component.translatable("tooltip.ars_misc.portal.destination",dimension));
        }
        if(portal.warpPos!=null) {
            var pos=portal.warpPos;
            lines.add(Component.translatable("tooltip.ars_misc.portal.coordinates",pos.getX(),pos.getY(),pos.getZ()));
        }
        if((portal.dimID==null || portal.dimID.isBlank()) && portal.warpPos==null
                && (portal.displayName==null || portal.displayName.isBlank()))
            lines.add(Component.translatable("tooltip.ars_misc.portal.unbound"));
    }
    private PortalDestinationTooltip() {}
}
