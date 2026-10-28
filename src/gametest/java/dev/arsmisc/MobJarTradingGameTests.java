package dev.arsmisc;

import com.hollingsworth.arsnouveau.common.block.MobJar;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

import java.util.UUID;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class MobJarTradingGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var profile = new GameProfile(UUID.randomUUID(), "jar-trade-test");
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void assertTrade(GameTestHelper h, EntityType<?> type) {
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        var block = (MobJar) BlockRegistry.MOB_JAR.get();
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var jar = (MobJarTile) h.getLevel().getBlockEntity(pos);
        var entity = type.create(h.getLevel());
        if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
            villager.setVillagerData(villager.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.FARMER));
        }
        h.assertTrue(entity instanceof Merchant && jar.setEntityData(entity), "Merchant could not be stored in a jar");
        var merchant = (Merchant) jar.getEntity();
        var player = player(h);
        player.setPos(pos.getCenter().add(0, 0, 2));
        var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
        block.useItemOn(ItemStack.EMPTY, h.getLevel().getBlockState(pos), h.getLevel(), pos,
                player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(player.containerMenu instanceof MerchantMenu && player.containerMenu.stillValid(player)
                && merchant.getTradingPlayer() == player, "Jar interaction invalidated the merchant's trading player: " + type);
        player.closeContainer();
        h.assertTrue(merchant.getTradingPlayer() == null, "Closing the menu retained its trading player");
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void ironWizardJarTradeStaysOpen(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("irons_spellbooks")) { h.succeed(); return; }
        for (var id : new String[]{"cryomancer", "pyromancer"}) {
            var key = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", id);
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(key), "Missing integration test merchant: " + key);
            assertTrade(h, BuiltInRegistries.ENTITY_TYPE.get(key));
        }
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void registeredVillagerJarBehaviorStillTrades(GameTestHelper h) {
        assertTrade(h, EntityType.VILLAGER);
        assertTrade(h, EntityType.WANDERING_TRADER);
        h.succeed();
    }
}
