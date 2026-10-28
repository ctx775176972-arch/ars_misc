package dev.arsmisc.item;

import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.perk.RepairingPerk;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.spell.SwordSpellDamage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import java.util.List;

/** Native scribing and spell resolution with the installed native crystal-sword model. */
public final class NetheriteEnchantersSwordItem extends SwordItem implements ICasterTool, software.bernie.geckolib.animatable.GeoItem {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cache=
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    public static final ResourceLocation SPELL_BONUS = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "netherite_sword_spell_bonus");
    public static ItemAttributeModifiers attributes() {
        var builder=ItemAttributeModifiers.builder();
        SwordItem.createAttributes(Tiers.NETHERITE,3,-2.4f).modifiers().forEach(entry ->
                builder.add(entry.attribute(),entry.modifier(),entry.slot()));
        return builder.add(ModRegistry.SPELL_DAMAGE_MULTIPLIER,new AttributeModifier(SPELL_BONUS,.15,
                AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();
    }
    public NetheriteEnchantersSwordItem() {
        super(Tiers.NETHERITE,new Properties().attributes(attributes()).fireResistant().rarity(Rarity.RARE)
                .component(DataComponentRegistry.SPELL_CASTER.get(),new SpellCaster()));
    }
    @Override public AbstractCaster<?> getSpellCaster(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SPELL_CASTER.get(),new SpellCaster());
    }
    @Override public boolean isScribedSpellValid(AbstractCaster<?> caster,Player player,InteractionHand hand,ItemStack stack,Spell spell) {
        return !spell.isEmpty() && spell.unsafeList().stream().noneMatch(part -> part instanceof AbstractCastMethod);
    }
    @Override public void scribeModifiedSpell(AbstractCaster<?> caster,Player player,InteractionHand hand,ItemStack stack,Spell.Mutable spell) {
        var parts=new java.util.ArrayList<AbstractSpellPart>();
        parts.add(MethodTouch.INSTANCE);parts.addAll(spell.recipe);spell.recipe=parts;
    }
    @Override public void sendInvalidMessage(Player player) {
        player.displayClientMessage(Component.translatable("ars_nouveau.sword.invalid"),true);
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        cast(stack,target,attacker);
        return super.hurtEnemy(stack,target,attacker);
    }
    public static void cast(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level) || attacker.getMainHandItem()!=stack
                || !(stack.getItem() instanceof NetheriteEnchantersSwordItem item)) return;
        var caster=item.getSpellCaster(stack);
        var spell=caster.modifySpellBeforeCasting(level,attacker,InteractionHand.MAIN_HAND,caster.getSpell());
        // Empty spells must reach the native validator so melee gives the same feedback as the native sword.
        if (!spell.isEmpty() && spell.getCastMethod()!=MethodTouch.INSTANCE) {
            if (attacker instanceof Player player) item.sendInvalidMessage(player);
            return;
        }
        var context=SpellContext.fromEntity(spell,attacker,stack);
        SwordSpellDamage.mark(context,attacker);
        SpellResolver resolver=attacker instanceof Player ? new SpellResolver(context) : new EntitySpellResolver(context);
        resolver.onCastOnEntity(stack,target,InteractionHand.MAIN_HAND);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        super.inventoryTick(stack,level,entity,slot,selected);
        if (entity instanceof Player player) RepairingPerk.attemptRepair(stack,player);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        getInformation(stack,context,lines,flag);
        lines.add(Component.translatable("tooltip.ars_misc.netherite_enchanters_sword"));
    }
    @Override public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {}
    @Override public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void createGeoRenderer(java.util.function.Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(dev.arsmisc.client.NetheriteSwordRenderer.createProvider());
    }
}
