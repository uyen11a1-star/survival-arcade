package com.nguyenquochuy.survivalarcade;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Khả năng của vũ khí độc quyền. Sức mạnh và hồi chiêu thay đổi theo bậc nâng cấp (+0 đến +5). */
public final class Weapons {
	private static final Random RNG = new Random();

	static final String[] MODES = {"Thiên Thạch", "Bão Sét", "Hố Đen", "Hỏa Phụng", "Ngưng Đọng"};
	private static final Formatting[] MODE_COLORS = {
		Formatting.RED, Formatting.AQUA, Formatting.DARK_PURPLE, Formatting.GOLD, Formatting.BLUE};
	private static final int[] MODE_CD = {360, 400, 500, 440, 600};
	private static final Map<UUID, Integer> MODE = new HashMap<>();

	private Weapons() {}

	public static String use(ServerPlayerEntity p, String id, int tier) {
		return switch (id) {
			case "thunder_blade" -> thunder(p, tier);
			case "blink_staff" -> blink(p, tier);
			case "power_wand" -> wand(p, tier);
			default -> null;
		};
	}

	public static String hud(ServerPlayerEntity p, String id) {
		return switch (id) {
			case "thunder_blade" -> "Kiếm Sấm Sét: " + cdText(p, "thunder");
			case "blink_staff" -> "Gậy Dịch Chuyển: " + cdText(p, "blink");
			case "power_wand" -> {
				int m = MODE.getOrDefault(p.getUuid(), 0);
				yield "Gậy Quyền Năng [" + MODES[m] + "]: " + cdText(p, "wand:" + m) + "   (ngồi + chuột phải: đổi kỹ năng)";
			}
			default -> null;
		};
	}

	private static String cdText(ServerPlayerEntity p, String key) {
		int s = Cd.remaining(p, key);
		return s <= 0 ? "SẴN SÀNG" : s + "s";
	}

	private static int cd(ServerPlayerEntity p, int base, int tier) {
		return Math.max(20, (int) (base * (1.0 - 0.05 * tier) * Armor.cdFactor(p)));
	}

	// =====================================================================
	// Tiện ích dùng chung
	// =====================================================================

	static List<HostileEntity> hostiles(ServerWorld w, Vec3d c, double r) {
		Box b = new Box(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
		return new ArrayList<>(w.getEntitiesByClass(HostileEntity.class, b,
			e -> e.isAlive() && e.squaredDistanceTo(c.x, c.y, c.z) <= r * r));
	}

	/** Gây sát thương cho quái thù địch quanh một điểm (chỉ quái, không đụng người chơi hay dân làng). */
	static void hit(ServerWorld w, UUID owner, Vec3d c, double radius, float dmg, double push, float igniteSec) {
		ServerPlayerEntity src = Fx.player(owner);
		if (src == null) return;
		DamageSource ds = w.getDamageSources().playerAttack(src);
		for (HostileEntity e : hostiles(w, c, radius)) {
			double dx = e.getX() - c.x;
			double dz = e.getZ() - c.z;
			double len = Math.max(0.1, Math.hypot(dx, dz));
			e.damage(w, ds, dmg);
			if (igniteSec > 0) e.setOnFireFor(igniteSec);
			if (push > 0) {
				e.setVelocity(dx / len * push, 0.4 + push * 0.2, dz / len * push);
				e.velocityModified = true;
			}
		}
	}

	/** Sét chỉ có hiệu ứng nhìn và âm thanh, không đốt cháy khối và không gây sát thương tự động. */
	static void bolt(ServerWorld w, double x, double y, double z) {
		BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
		EntityType.LIGHTNING_BOLT.spawn(w, e -> e.setCosmetic(true), pos, SpawnReason.EVENT, false, false);
	}

	/** Điểm ngắm: quái đang nhìn tới (trong khoảng ~20 độ), nếu không có thì điểm chạm đất theo hướng nhìn. */
	private static Vec3d aim(ServerWorld w, ServerPlayerEntity p, double range) {
		Vec3d eye = p.getEyePos();
		Vec3d look = p.getRotationVector();
		HostileEntity best = null;
		double bestDot = 0.93;
		for (HostileEntity e : hostiles(w, eye, range)) {
			Vec3d d = new Vec3d(e.getX() - eye.x, e.getEyeY() - eye.y, e.getZ() - eye.z);
			double len = d.length();
			if (len < 1.0) continue;
			double dot = (d.x * look.x + d.y * look.y + d.z * look.z) / len;
			if (dot > bestDot) {
				bestDot = dot;
				best = e;
			}
		}
		if (best != null) return new Vec3d(best.getX(), best.getY(), best.getZ());
		Vec3d pos = eye;
		for (double s = 1; s <= range; s += 1) {
			Vec3d c = eye.add(look.multiply(s));
			BlockPos bp = new BlockPos((int) Math.floor(c.x), (int) Math.floor(c.y), (int) Math.floor(c.z));
			if (!w.getBlockState(bp).isAir()) break;
			pos = c;
		}
		BlockPos g = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
		for (int i = 0; i < 40 && w.getBlockState(g.down()).isAir(); i++) g = g.down();
		return new Vec3d(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
	}

	// =====================================================================
	// Kiếm Sấm Sét
	// =====================================================================

	private static String thunder(ServerPlayerEntity p, int tier) {
		int left = Cd.use(p, "thunder", cd(p, 100, tier));
		if (left > 0) return "Kiếm Sấm Sét còn hồi chiêu " + left + " giây.";
		ServerWorld w = p.getEntityWorld();
		double mult = (1.0 + 0.15 * tier) * Armor.powerMult(p);
		UUID id = p.getUuid();
		Vec3d t = aim(w, p, 24);
		bolt(w, t.x, t.y, t.z);
		hit(w, id, t, 4.0, (float) (16 * mult), 0.7, 0);
		Fx.expandingRing(w, t.x, t.y + 0.2, t.z, 5, ParticleTypes.ELECTRIC_SPARK);
		// sét lan sang tối đa 3 quái ở gần
		List<HostileEntity> near = hostiles(w, t, 9);
		near.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(t.x, t.y, t.z)));
		int n = 0;
		for (HostileEntity e : near) {
			if (n >= 3) break;
			if (e.squaredDistanceTo(t.x, t.y, t.z) < 4) continue;
			n++;
			Vec3d pos = new Vec3d(e.getX(), e.getY(), e.getZ());
			Fx.later(3 * n, () -> {
				bolt(w, pos.x, pos.y, pos.z);
				hit(w, id, pos, 3.0, (float) (10 * mult), 0.5, 0);
				Fx.ringAt(w, pos.x, pos.y + 0.2, pos.z, 2.5, ParticleTypes.ELECTRIC_SPARK, 16);
			});
		}
		return null;
	}

	// =====================================================================
	// Gậy Dịch Chuyển
	// =====================================================================

	private static String blink(ServerPlayerEntity p, int tier) {
		int left = Cd.use(p, "blink", cd(p, 60, tier));
		if (left > 0) return "Gậy Dịch Chuyển còn hồi chiêu " + left + " giây.";
		ServerWorld w = p.getEntityWorld();
		Vec3d look = p.getRotationVector();
		double max = 14 + 2 * tier;
		double x = p.getX(), y = p.getY(), z = p.getZ();
		double lx = x, ly = y, lz = z;
		for (double s = 0.5; s <= max; s += 0.5) {
			double cx = x + look.x * s;
			double cy = y + look.y * s;
			double cz = z + look.z * s;
			BlockPos bp = new BlockPos((int) Math.floor(cx), (int) Math.floor(cy), (int) Math.floor(cz));
			if (!w.getBlockState(bp).isAir() || !w.getBlockState(bp.up()).isAir()) break;
			lx = cx;
			ly = cy;
			lz = cz;
		}
		if (Math.abs(lx - x) + Math.abs(ly - y) + Math.abs(lz - z) < 1.5) {
			Cd.clear(p, "blink");
			return "Phía trước bị chắn, không thể dịch chuyển.";
		}
		Fx.burst(p, ParticleTypes.PORTAL, 40);
		double dist = Math.sqrt((lx - x) * (lx - x) + (ly - y) * (ly - y) + (lz - z) * (lz - z));
		for (double s = 0; s <= dist; s += 1.0) {
			w.spawnParticles(ParticleTypes.END_ROD, x + look.x * s, y + 1.0 + look.y * s, z + look.z * s, 1, 0.05, 0.05, 0.05, 0.0);
		}
		p.requestTeleport(lx, ly, lz);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false));
		Fx.burst(p, ParticleTypes.PORTAL, 40);
		Fx.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f);
		return null;
	}

	// =====================================================================
	// Gậy Quyền Năng: 5 kỹ năng, ngồi + chuột phải để đổi
	// =====================================================================

	private static String wand(ServerPlayerEntity p, int tier) {
		int mode = MODE.getOrDefault(p.getUuid(), 0);
		if (p.isSneaking()) {
			mode = (mode + 1) % MODES.length;
			MODE.put(p.getUuid(), mode);
			Store.bar(p, "Gậy Quyền Năng: " + MODES[mode]);
			Fx.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.6f);
			return null;
		}
		int left = Cd.use(p, "wand:" + mode, cd(p, MODE_CD[mode], tier));
		if (left > 0) return MODES[mode] + " còn hồi chiêu " + left + " giây.";
		double mult = (1.0 + 0.15 * tier) * Armor.powerMult(p);
		Fx.title(p, MODES[mode].toUpperCase(), "Gậy Quyền Năng", MODE_COLORS[mode]);
		switch (mode) {
			case 0 -> meteor(p, mult);
			case 1 -> storm(p, mult);
			case 2 -> blackHole(p, mult);
			case 3 -> phoenix(p, mult);
			default -> freeze(p, mult);
		}
		return null;
	}

	/** Thiên Thạch: cột sáng cảnh báo, vòng lửa thu hẹp, thiên thạch rơi và nổ lớn. */
	private static void meteor(ServerPlayerEntity p, double mult) {
		ServerWorld w = p.getEntityWorld();
		UUID id = p.getUuid();
		Vec3d t = aim(w, p, 30);
		for (int i = 0; i < 30; i += 2) {
			final int k = i;
			Fx.later(k, () -> {
				Fx.ringAt(w, t.x, t.y + 0.2, t.z, Math.max(1.0, 7.0 - k * 0.2), ParticleTypes.FLAME, 24);
				for (int h = 0; h < 40; h += 4) {
					w.spawnParticles(ParticleTypes.END_ROD, t.x, t.y + h, t.z, 1, 0, 0, 0, 0);
				}
			});
		}
		for (int i = 0; i < 12; i++) {
			final int k = i;
			Fx.later(18 + k, () -> {
				double y = t.y + 40 - k * 3.3;
				w.spawnParticles(ParticleTypes.FLAME, t.x, y, t.z, 25, 0.8, 0.8, 0.8, 0.02);
				w.spawnParticles(ParticleTypes.LARGE_SMOKE, t.x, y, t.z, 8, 0.6, 0.6, 0.6, 0.01);
			});
		}
		Fx.later(32, () -> {
			hit(w, id, t, 7.0, (float) (40 * mult), 1.4, 5.0f);
			w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, t.x, t.y + 1.0, t.z, 1, 0, 0, 0, 0);
			w.spawnParticles(ParticleTypes.LAVA, t.x, t.y + 1.0, t.z, 40, 2.0, 1.0, 2.0, 0.1);
			w.spawnParticles(ParticleTypes.FLAME, t.x, t.y + 0.5, t.z, 80, 2.5, 0.8, 2.5, 0.15);
			Fx.expandingRing(w, t.x, t.y + 0.3, t.z, 9, ParticleTypes.FLAME);
			ServerPlayerEntity q = Fx.player(id);
			if (q != null) Fx.sound(q, SoundEvents.ENTITY_WITHER_SPAWN, 0.5f);
		});
	}

	/** Bão Sét: 10 tia sét giáng liên tiếp xuống quái quanh người chơi. */
	private static void storm(ServerPlayerEntity p, double mult) {
		UUID id = p.getUuid();
		for (int i = 0; i < 10; i++) {
			Fx.later(i * 4, () -> {
				ServerPlayerEntity q = Fx.player(id);
				if (q == null || !q.isAlive()) return;
				ServerWorld w = q.getEntityWorld();
				Vec3d c = new Vec3d(q.getX(), q.getY(), q.getZ());
				List<HostileEntity> hs = hostiles(w, c, 20);
				Vec3d tgt;
				if (!hs.isEmpty()) {
					HostileEntity h = hs.get(RNG.nextInt(hs.size()));
					tgt = new Vec3d(h.getX(), h.getY(), h.getZ());
				} else {
					double a = RNG.nextDouble() * Math.PI * 2;
					double r = 4 + RNG.nextDouble() * 8;
					tgt = new Vec3d(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r);
				}
				bolt(w, tgt.x, tgt.y, tgt.z);
				hit(w, id, tgt, 3.5, (float) (14 * mult), 0.6, 0);
				Fx.ringAt(w, tgt.x, tgt.y + 0.2, tgt.z, 3.0, ParticleTypes.ELECTRIC_SPARK, 20);
			});
		}
	}

	/** Hố Đen: hút quái vào tâm xoáy trong 3 giây rồi nổ tung. */
	private static void blackHole(ServerPlayerEntity p, double mult) {
		ServerWorld w = p.getEntityWorld();
		UUID id = p.getUuid();
		Vec3d c = aim(w, p, 20);
		for (int i = 0; i < 60; i += 2) {
			final int k = i;
			Fx.later(k, () -> {
				for (int s = 0; s < 3; s++) {
					double a = k * 0.4 + s * 2.1;
					double r = 5.0 - k * 0.07;
					w.spawnParticles(ParticleTypes.PORTAL, c.x + Math.cos(a) * r, c.y + 1.2 + Math.sin(k * 0.3 + s) * 0.8,
						c.z + Math.sin(a) * r, 2, 0.05, 0.05, 0.05, 0.1);
				}
				w.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1.2, c.z, 3, 0.3, 0.3, 0.3, 0.01);
				for (HostileEntity e : hostiles(w, c, 12)) {
					Vec3d d = new Vec3d(c.x - e.getX(), c.y + 1.0 - e.getY(), c.z - e.getZ());
					double len = Math.max(0.5, d.length());
					e.setVelocity(d.x / len * 0.6, d.y / len * 0.3 + 0.05, d.z / len * 0.6);
					e.velocityModified = true;
				}
			});
		}
		Fx.later(62, () -> {
			hit(w, id, c, 7.0, (float) (28 * mult), 1.2, 0);
			w.spawnParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 1.2, c.z, 1, 0, 0, 0, 0);
			Fx.expandingRing(w, c.x, c.y + 0.3, c.z, 8, ParticleTypes.PORTAL);
			Fx.expandingRing(w, c.x, c.y + 0.9, c.z, 8, ParticleTypes.END_ROD);
			ServerPlayerEntity q = Fx.player(id);
			if (q != null) Fx.sound(q, SoundEvents.ENTITY_WITHER_SPAWN, 0.7f);
		});
	}

	/** Hỏa Phụng: sóng lửa lan rộng 12 block, thiêu đốt quái, hồi máu cho người dùng. */
	private static void phoenix(ServerPlayerEntity p, double mult) {
		ServerWorld w = p.getEntityWorld();
		UUID id = p.getUuid();
		Vec3d c = new Vec3d(p.getX(), p.getY(), p.getZ());
		p.heal(8.0f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
		Fx.helix(p, ParticleTypes.FLAME, 36);
		Fx.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f);
		Fx.expandingRing(w, c.x, c.y + 0.2, c.z, 12, ParticleTypes.FLAME);
		Fx.later(3, () -> Fx.expandingRing(w, c.x, c.y + 0.6, c.z, 12, ParticleTypes.SOUL_FIRE_FLAME));
		Fx.later(5, () -> hit(w, id, c, 12.0, (float) (24 * mult), 1.6, 6.0f));
	}

	/** Ngưng Đọng: đóng băng thời gian, quái trong 18 block đứng yên, người dùng được tăng tốc. */
	private static void freeze(ServerPlayerEntity p, double mult) {
		ServerWorld w = p.getEntityWorld();
		Vec3d c = new Vec3d(p.getX(), p.getY(), p.getZ());
		int dur = 100 + (int) ((mult - 1.0) * 130);
		List<UUID> ids = new ArrayList<>();
		for (HostileEntity e : hostiles(w, c, 18)) {
			e.setAiDisabled(true);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur + 20, 6, false, false));
			ids.add(e.getUuid());
		}
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur + 20, 2, false, false));
		Fx.expandingRing(w, c.x, c.y + 0.2, c.z, 18, ParticleTypes.SNOWFLAKE);
		Fx.expandingRing(w, c.x, c.y + 1.0, c.z, 18, ParticleTypes.ENCHANT);
		Fx.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f);
		for (int i = 0; i < dur; i += 10) {
			Fx.later(i, () -> {
				for (UUID u : ids) {
					Entity e = w.getEntity(u);
					if (e != null && e.isAlive()) {
						w.spawnParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + 1.0, e.getZ(), 6, 0.4, 0.6, 0.4, 0.02);
					}
				}
			});
		}
		Fx.later(dur, () -> {
			for (UUID u : ids) {
				Entity e = w.getEntity(u);
				if (e instanceof MobEntity m) m.setAiDisabled(false);
			}
		});
	}
}
