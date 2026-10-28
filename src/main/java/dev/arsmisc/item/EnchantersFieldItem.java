package dev.arsmisc.item;

import com.hollingsworth.arsnouveau.api.item.*;
import com.hollingsworth.arsnouveau.api.entity.ISummon;
import com.hollingsworth.arsnouveau.api.spell.*;
import dev.arsmisc.spell.FieldSpellResolver;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.menu.EnchantersFieldMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import java.util.*;

/** Direct targeted effect resolution; filters execute inside the stored spell. */
public final class EnchantersFieldItem extends ArsNouveauCurio implements ICasterTool {
    public static final String NEXT_UPKEEP = "ars_misc:field_next_upkeep";
    public static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID,"field_excluded"));
    public EnchantersFieldItem() { super(new Properties().stacksTo(1)); }
    private static net.minecraft.nbt.CompoundTag data(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
    }
    public static boolean enabled(ItemStack s) { var d=data(s); return !d.contains("field_enabled") || d.getBoolean("field_enabled"); }
    public static void setEnabled(ItemStack s,boolean v) { CustomData.update(DataComponents.CUSTOM_DATA,s,d->d.putBoolean("field_enabled",v)); }
    public static int interval(ItemStack s) { var d=data(s); return d.contains("field_interval") ? Math.clamp(d.getInt("field_interval"),1,3600) : 1; }
    public static void setInterval(ItemStack s,int v) { CustomData.update(DataComponents.CUSTOM_DATA,s,d->d.putInt("field_interval",Math.clamp(v,1,3600))); }
    public static final int DEFAULT_RADIUS = 80;
    public static final int MAX_RADIUS = 160;
    // Radius is stored in tenths of a block, independently of interaction attributes.
    public static int radiusSetting(ItemStack s) { var d=data(s);return !d.contains("field_radius") || d.getInt("field_radius")==0 ? DEFAULT_RADIUS : Math.clamp(d.getInt("field_radius"),1,MAX_RADIUS); }
    public static void setRadius(ItemStack s,int v) { CustomData.update(DataComponents.CUSTOM_DATA,s,d->d.putInt("field_radius",Math.clamp(v,1,MAX_RADIUS))); }
    public static double radius(Player p,ItemStack s) { return radiusSetting(s)/10.0; }
    public static int upkeepCost(ItemStack s) { return radiusSetting(s); }
    private static boolean payUpkeep(Player p,ItemStack s,long now) {
        long next=p.getPersistentData().getLong(NEXT_UPKEEP);
        if(next>now && next<=now+20)return true;
        var mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p);
        int cost=upkeepCost(s);
        if(mana==null || !Double.isFinite(mana.getCurrentMana()) || mana.getCurrentMana()<cost) {
            setEnabled(s,false);
            p.getInventory().setChanged();
            p.displayClientMessage(Component.translatable("item.ars_misc.enchanters_field.no_mana"),true);
            return false;
        }
        // A player-wide deadline prevents repeated ticks, toggles and item swaps from charging twice.
        mana.removeMana(cost);
        p.getPersistentData().putLong(NEXT_UPKEEP,now+20);
        return true;
    }
    public static Spell directSpell(Spell spell) {
        var parts=new ArrayList<AbstractSpellPart>(spell.unsafeList());
        if (!parts.isEmpty() && parts.getFirst() instanceof AbstractCastMethod) parts.removeFirst();
        return spell.setRecipe(parts);
    }
    @Override public AbstractCaster<?> getSpellCaster(ItemStack s) { return s.getOrDefault(DataComponentRegistry.SPELL_CASTER.get(),new SpellCaster()); }
    @Override public boolean isScribedSpellValid(AbstractCaster<?> c,Player p,InteractionHand h,ItemStack s,Spell spell) {
        return !spell.isEmpty() && spell.unsafeList().stream().anyMatch(x->!(x instanceof AbstractCastMethod));
    }
    @Override public void scribeModifiedSpell(AbstractCaster<?> c,Player p,InteractionHand h,ItemStack s,Spell.Mutable spell) {
        var parts=new ArrayList<AbstractSpellPart>(spell.recipe);
        if (!parts.isEmpty() && parts.getFirst() instanceof AbstractCastMethod) parts.removeFirst();
        spell.recipe=parts;
    }
    @Override public boolean canEquipFromUse(SlotContext c,ItemStack s) { return false; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand) {
        var s=p.getItemInHand(hand);
        if(p instanceof ServerPlayer server)p.openMenu(new SimpleMenuProvider((id,inv,who)->new EnchantersFieldMenu(id,inv,hand),s.getHoverName()),b->b.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(s,level.isClientSide);
    }
    public static boolean validTarget(Player p,LivingEntity e) {
        return e!=p && e.isAlive() && !e.isSpectator() && !(e instanceof Player)
            && !(e instanceof ISummon) && !(e instanceof OwnableEntity own && own.getOwnerUUID()!=null)
            && !e.getType().is(EXCLUDED);
    }
    private static double distanceToTarget(Player p, LivingEntity e) {
        var eye=p.getEyePosition();var box=e.getBoundingBox();
        return eye.distanceToSqr(new net.minecraft.world.phys.Vec3(
            Math.clamp(eye.x,box.minX,box.maxX),Math.clamp(eye.y,box.minY,box.maxY),Math.clamp(eye.z,box.minZ,box.maxZ)));
    }
    public List<LivingEntity> targets(Player p,ItemStack s) {
        double range=radius(p,s);
        return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),e->validTarget(p,e)
            && distanceToTarget(p,e)<=range*range)
            .stream().sorted(Comparator.<LivingEntity>comparingDouble(p::distanceToSqr).thenComparingInt(Entity::getId)).toList();
    }
    @Override public void curioTick(SlotContext slot,ItemStack s) {
        if(slot.cosmetic() || !(slot.entity() instanceof Player p) || p.level().isClientSide || !p.isAlive()
            || p.isSpectator() || p.tickCount%20!=0 || !enabled(s))return;
        long now=p.level().getGameTime();
        if(!payUpkeep(p,s,now))return;
        var d=data(s);long next=d.getLong("field_next");
        if(next>now && next<=now+72000L)return;
        var spell=directSpell(getSpellCaster(s).getSpell());if(spell.isEmpty())return;
        CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putLong("field_next",now+interval(s)*20L));
        for(var target:targets(p,s)) {
            if(!enabled(s) || !validTarget(p,target))continue;
            try {
                var resolver=new FieldSpellResolver(SpellContext.fromEntity(spell,p,s));
                resolver.withSilent(true);
                resolver.castOnTarget(p,target);
            } catch(RuntimeException ex) {
                setEnabled(s,false);ArsMiscMod.LOGGER.error("Paused Enchanter's Field after a spell error",ex);
                p.displayClientMessage(Component.translatable("item.ars_misc.enchanters_field.error"),true);break;
            }
        }
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f) {
        super.appendHoverText(s,c,lines,f);lines.add(Component.translatable("item.ars_misc.enchanters_field.help"));
        lines.add(Component.translatable("item.ars_misc.enchanters_field.upkeep",upkeepCost(s)));
        lines.add(Component.translatable(enabled(s)?"gui.ars_misc.watch.enabled":"gui.ars_misc.watch.paused"));getInformation(s,c,lines,f);
    }
}
