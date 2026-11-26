package com.nguyenquochuy.survivalarcade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SurvivalArcade implements ModInitializer {
	public static final String MOD_ID = "survivalarcade";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess, environment) -> ArcadeCommands.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(Store::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> Store.save());
		ServerTickEvents.END_SERVER_TICK.register(Games::tick);

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			Events.onDeath(entity, source);
			if (entity instanceof ServerPlayerEntity sp) {
				Games.onPlayerDeath(sp);
				return;
			}
			if (source.getAttacker() instanceof ServerPlayerEntity killer) {
				Quests.onKill(killer, entity);
			}
		});

		// gỡ sách khỏi túi đồ trước khi chết để không rơi ra đất; hồi sinh xong sẽ được cấp lại
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Armor::allowDamage);

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity sp) {
				if (!Armor.allowDeath(sp)) return false;
				ArcadeBook.strip(sp);
				ArcadeItems.strip(sp);
			}
			return true;
		});

		UseItemCallback.EVENT.register(ArcadeItems::onUse);

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayerEntity sp) {
				Quests.onMine(sp, state);
				ArcadeItems.onBreak(sp, world, pos, state);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Games.drop(handler.player.getUuid());
		});

		LOG.info("Survival Arcade loaded");
	}
}
