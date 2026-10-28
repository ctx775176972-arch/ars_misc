package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.PortalTile;
import dev.arsmisc.PortalDestinationTooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(PortalTile.class)
public abstract class PortalDestinationTooltipMixin {
    @Inject(method="getTooltip",at=@At("TAIL"))
    private void arsMisc$destination(List<Component> lines,CallbackInfo ci) {
        PortalDestinationTooltip.append((PortalTile)(Object)this,lines);
    }
}
