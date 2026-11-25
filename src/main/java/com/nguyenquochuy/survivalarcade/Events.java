package com.nguyenquochuy.survivalarcade;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Sự kiện bất ngờ: Boss xuất hiện sau màn cảnh báo, và Bounty (quái có tên, phát sáng, thưởng lớn). */
public final class Events {
	private static final Random RNG = new Random();

	// ---- Boss ----
	private static UUID bossId;
	private static ServerWorld bossWorld;
	private static String bossName = "";
	private static int bossLevel;
	private static int bossEnd;
	private static int lastBoss = -9000; // cho phép boss đầu tiên xuất hiện sau ~2,5 phút
	private static boolean bossSpawning;
	private static ServerBossBar bossBar;

	// ---- Bounty ----
	private static UUID bountyId;
	private static ServerWorld bountyWorld;
	private static String bountyName = "";
	private static int bountyEnd;
	private static int lastBounty = -3000;
	private static final String[] BOUNTY_NAMES = {"Kẻ Săn Đêm", "Bóng Ma Đỏ", "Hung Thần", "Sát Thủ Vô Danh", "Lưỡi Hái Câm"};

	private Events() {}

	/** Gọi mỗi giây (20 tick). */
	public static void tick(MinecraftServer server, int t) {
		tickBoss(server, t);
		tickBounty(server, t);
	}

	private static void announce(String s, Formatting f) {
		Store.server.getPlayerManager().broadcast(Text.literal(s).formatted(f), false);
	}

	private static ServerPlayerEntity pickPlayer(MinecraftServer server) {
		List<ServerPlayerEntity> ok = new ArrayList<>();
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (!p.isAlive() || p.isSpectator() || Games.current(p) != null) continue;
			if (!Games.onSurface(p.getEntityWorld(), p)) continue;
			ok.add(p);
		}
		return ok.isEmpty() ? null : ok.get(RNG.nextInt(ok.size()));
	}

	private static void boost(MobEntity m, double hp, double scale) {
		EntityAttributeInstance a = m.getAttributeInstance(EntityAttributes.MAX_HEALTH);
		if (a != null) a.setBaseValue(hp);
		m.setHealth((float) hp);
		EntityAttributeInstance s = m.getAttributeInstance(EntityAttributes.SCALE);
		if (s != null) s.setBaseValue(scale);
	}

	// =====================================================================
	// Boss
	// =====================================================================

	private static void tickBoss(MinecraftServer server, int t) {
		if (bossId != null) {
			updateBoss(server);
			return;
		}
		if (bossSpawning || bountyId != null) return;
		if (t - lastBoss < 12000 || RNG.nextInt(240) != 0) return;
		ServerPlayerEntity p = pickPlayer(server);
		if (p != null) startBoss(p, t);
	}

	private static void startBoss(ServerPlayerEntity p, int t) {
		lastBoss = t;
		bossSpawning = true;
		UUID pid = p.getUuid();
		Fx.title(p, "CẢNH BÁO", "Có thứ gì đó đang tới gần...", Formatting.DARK_RED);
		Fx.sound(p, SoundEvents.ENTITY_WITHER_SPAWN, 0.6f);
		// vòng lửa linh hồn thu hẹp quanh người trong 2 giây
		for (int i = 0; i < 40; i += 2) {
			final int k = i;
			Fx.later(k, () -> {
				ServerPlayerEntity q = Fx.player(pid);
				if (q == null) return;
				double r = Math.max(0.8, 6.0 - k * 0.13);
				Fx.ringAt(q.getEntityWorld(), q.getX(), q.getY() + 0.2, q.getZ(), r, ParticleTypes.SOUL_FIRE_FLAME, 28);
			});
		}
		Fx.later(44, () -> {
			bossSpawning = false;
			ServerPlayerEntity q = Fx.player(pid);
			if (q != null && q.isAlive()) spawnBoss(q);
		});
	}

	private static void spawnBoss(ServerPlayerEntity q) {
		ServerWorld w = q.getEntityWorld();
		int lv = Store.level(Store.get(q).xp);
		EntityType<? extends MobEntity> type;
		String name;
		double scale;
		if (lv >= 10) {
			type = EntityType.RAVAGER;
			name = "Quái Thú Gầm Thét";
			scale = 1.4;
		} else if (lv >= 5) {
			type = EntityType.WITHER_SKELETON;
			name = "Kỵ Sĩ Xương Đen";
			scale = 2.2;
		} else {
			type = EntityType.ZOMBIE;
			name = "Zombie Khổng Lồ";
			scale = 2.5;
		}
		MobEntity m = Games.spawnAround(w, q, q.getX(), q.getZ(), 12, 16, type);
		if (m == null) return;
		boost(m, 120 + lv * 10, scale);
		m.setCustomName(Text.literal(name).formatted(Formatting.DARK_RED, Formatting.BOLD));
		m.setCustomNameVisible(true);
		m.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60 * 10, 1, false, false));
		m.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60 * 10, 0, false, false));

		bossId = m.getUuid();
		bossWorld = w;
		bossName = name;
		bossLevel = lv;
		bossEnd = Store.server.getTicks() + 20 * 60 * 10;
		bossBar = new ServerBossBar(Text.literal("BOSS: " + name), BossBar.Color.RED, BossBar.Style.PROGRESS);

		announce("⚠ " + name + " xuất hiện gần " + q.getName().getString() + "!", Formatting.RED);
		Fx.title(q, "BOSS XUẤT HIỆN", name, Formatting.DARK_RED);
		Fx.sound(q, SoundEvents.ENTITY_WITHER_SPAWN, 0.7f);
		Fx.expandingRing(w, m.getX(), m.getY() + 0.2, m.getZ(), 8, ParticleTypes.SOUL_FIRE_FLAME);
	}

	private static void updateBoss(MinecraftServer server) {
		Entity e = bossWorld.getEntity(bossId);
		boolean alive = e instanceof LivingEntity && e.isAlive();
		if (!alive || Store.server.getTicks() >= bossEnd) {
			if (alive) {
				e.discard();
				announce(bossName + " đã bỏ đi...", Formatting.GRAY);
			}
			endBoss();
			return;
		}
		LivingEntity le = (LivingEntity) e;
		bossBar.setPercent(Math.max(0f, Math.min(1f, le.getHealth() / le.getMaxHealth())));
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (p.getEntityWorld() == bossWorld && p.squaredDistanceTo(e) < 80 * 80) bossBar.addPlayer(p);
			else bossBar.removePlayer(p);
		}
		// hào quang lửa linh hồn quanh boss
		Fx.ringAt(bossWorld, e.getX(), e.getY() + 0.1, e.getZ(), 2.5, ParticleTypes.SOUL_FIRE_FLAME, 20);
	}

	private static void endBoss() {
		if (bossBar != null) bossBar.clearPlayers();
		bossBar = null;
		bossId = null;
		bossWorld = null;
	}

	private static void bossKilled(Entity boss) {
		String name = bossName;
		ServerWorld w = bossWorld;
		int lv = bossLevel;
		endBoss();
		int n = 0;
		for (ServerPlayerEntity p : Store.server.getPlayerManager().getPlayerList()) {
			if (p.getEntityWorld() != w || p.squaredDistanceTo(boss) > 60 * 60) continue;
			n++;
			int got = Store.addPoints(p, 80 + lv * 4, "combat");
			p.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 3));
			p.getInventory().offerOrDrop(new ItemStack(Items.GOLDEN_APPLE, 2));
			p.addExperience(200);
			Store.grant(p, "boss_slayer");
			Fx.win(p, "BOSS HẠ GỤC", name);
			Store.say(p, "Phần thưởng boss: +" + got + " điểm, 3 kim cương, 2 táo vàng, 200 XP", Formatting.GREEN);
		}
		announce("✦ " + name + " đã bị hạ gục bởi " + n + " người chơi!", Formatting.GOLD);
		Fx.expandingRing(w, boss.getX(), boss.getY() + 0.5, boss.getZ(), 6, ParticleTypes.FIREWORK);
	}

	// =====================================================================
	// Bounty
	// =====================================================================

	private static void tickBounty(MinecraftServer server, int t) {
		if (bountyId != null) {
			updateBounty();
			return;
		}
		if (bossId != null || bossSpawning) return;
		if (t - lastBounty < 6000 || RNG.nextInt(200) != 0) return;
		ServerPlayerEntity p = pickPlayer(server);
		if (p != null) startBounty(p, t);
	}

	private static void startBounty(ServerPlayerEntity p, int t) {
		lastBounty = t;
		ServerWorld w = p.getEntityWorld();
		int lv = Store.level(Store.get(p).xp);
		MobEntity m = Games.spawnAround(w, p, p.getX(), p.getZ(), 25, 40, Games.pick(2 + lv / 2));
		if (m == null) return;
		String name = BOUNTY_NAMES[RNG.nextInt(BOUNTY_NAMES.length)];
		boost(m, 40 + lv * 4, 1.3);
		m.setCustomName(Text.literal("★ " + name).formatted(Formatting.GOLD));
		m.setCustomNameVisible(true);
		m.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 6000, 0, false, false));
		m.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 0, false, false));
		m.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0, false, false));

		bountyId = m.getUuid();
		bountyWorld = w;
		bountyName = name;
		bountyEnd = Store.server.getTicks() + 6000;

		double dx = m.getX() - p.getX();
		double dz = m.getZ() - p.getZ();
		String where = Games.Delivery.dir(dx, dz);
		announce("★ Bounty: " + name + " đang lẩn khuất ở hướng " + where + " cách " + (int) Math.hypot(dx, dz)
			+ " block quanh " + p.getName().getString() + "! Hạ nó để nhận thưởng.", Formatting.GOLD);
		Fx.title(p, "BOUNTY", name + " — hướng " + where, Formatting.GOLD);
		Fx.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f);
	}

	private static void updateBounty() {
		Entity e = bountyWorld.getEntity(bountyId);
		if (e == null || !e.isAlive()) {
			endBounty();
			return;
		}
		if (Store.server.getTicks() >= bountyEnd) {
			e.discard();
			announce(bountyName + " đã biến mất...", Formatting.GRAY);
			endBounty();
			return;
		}
		Fx.ringAt(bountyWorld, e.getX(), e.getY() + 0.1, e.getZ(), 1.5, ParticleTypes.FLAME, 12);
	}

	private static void endBounty() {
		bountyId = null;
		bountyWorld = null;
	}

	private static void bountyKilled(Entity victim, DamageSource source) {
		String name = bountyName;
		endBounty();
		if (source.getAttacker() instanceof ServerPlayerEntity k) {
			int got = Store.addPoints(k, 35, "combat");
			k.getInventory().offerOrDrop(new ItemStack(Items.EMERALD, 6));
			k.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 1));
			k.addExperience(80);
			Store.grant(k, "bounty_hunter");
			Fx.win(k, "BOUNTY HOÀN THÀNH", name);
			Store.say(k, "Thưởng bounty: +" + got + " điểm, 6 ngọc lục bảo, 1 kim cương, 80 XP", Formatting.GREEN);
			announce("★ " + k.getName().getString() + " đã hạ " + name + "!", Formatting.GOLD);
		}
	}

	// =====================================================================
	// Gọi từ sự kiện tử vong
	// =====================================================================

	public static void onDeath(Entity entity, DamageSource source) {
		UUID id = entity.getUuid();
		if (bossId != null && id.equals(bossId)) {
			bossKilled(entity);
		} else if (bountyId != null && id.equals(bountyId)) {
			bountyKilled(entity, source);
		}
	}
}
