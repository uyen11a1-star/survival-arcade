package com.nguyenquochuy.survivalarcade;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Hiệu ứng bộ giáp độc quyền: 2 món cho hiệu ứng nhỏ, đủ 4 món mở hiệu ứng bộ.
 * Mỗi món được nhận diện bằng id dạng "thunder_helmet", "void_chest", "power_legs"...
 */
public final class Armor {
	private static final Random RNG = new Random();
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] SUFFIX = {"helmet", "chest", "legs", "boots"};

	private Armor() {}

	/** Số món của bộ `set` đang mặc đúng ô (0 đến 4). */
	public static int pieces(ServerPlayerEntity p, String set) {
		int n = 0;
		for (int i = 0; i < 4; i++) {
			String id = ArcadeItems.id(p.getEquippedStack(SLOTS[i]));
			if ((set + "_" + SUFFIX[i]).equals(id)) n++;
		}
		return n;
	}

	// ---------- hệ số của bộ Quyền Năng ----------

	public static double cdFactor(ServerPlayerEntity p) {
		int n = pieces(p, "power");
		return n >= 4 ? 0.70 : n >= 2 ? 0.85 : 1.0;
	}

	public static double powerMult(ServerPlayerEntity p) {
		return pieces(p, "power") >= 4 ? 1.25 : 1.0;
	}

	// ---------- hiệu ứng thường trực, gọi mỗi giây ----------

	private static void fx(ServerPlayerEntity p, RegistryEntry<StatusEffect> e, int amp, int dur) {
		p.addStatusEffect(new StatusEffectInstance(e, dur, amp, true, false, true));
	}

	private static boolean isNight(ServerPlayerEntity p) {
		long t = p.getEntityWorld().getTimeOfDay() % 24000L;
		return t >= 13000L && t <= 23000L;
	}

	public static void tick(ServerPlayerEntity p) {
		if (!p.isAlive()) return;
		int th = pieces(p, "thunder");
		int vo = pieces(p, "void");
		int ph = pieces(p, "phoenix");
		int gu = pieces(p, "guardian");
		int sh = pieces(p, "shadow");
		int po = pieces(p, "power");

		if (th >= 2) fx(p, StatusEffects.SPEED, th >= 4 ? 1 : 0, 80);
		if (th >= 4) {
			fx(p, StatusEffects.HASTE, 0, 80);
			zap(p);
		}
		if (vo >= 2) fx(p, StatusEffects.JUMP_BOOST, 1, 80);
		if (ph >= 2) fx(p, StatusEffects.FIRE_RESISTANCE, 0, 80);
		if (ph >= 4 && p.getHealth() <= p.getMaxHealth() * 0.5f) fx(p, StatusEffects.REGENERATION, 1, 60);
		if (gu >= 2) fx(p, StatusEffects.RESISTANCE, gu >= 4 ? 1 : 0, 80);
		if (gu >= 4 && p.getAbsorptionAmount() <= 0.0f && Cd.use(p, "guardian", 400) == 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 1, true, false, true));
			Fx.burst(p, ParticleTypes.END_ROD, 20);
		}
		if (sh >= 2) fx(p, StatusEffects.NIGHT_VISION, 0, 300);
		if (sh >= 4) {
			if (p.isSneaking()) fx(p, StatusEffects.INVISIBILITY, 0, 60);
			if (isNight(p)) fx(p, StatusEffects.SPEED, 1, 80);
		}
		if (po >= 2) fx(p, StatusEffects.RESISTANCE, 0, 80);
		if (po >= 4) {
			fx(p, StatusEffects.STRENGTH, 0, 80);
			fx(p, StatusEffects.REGENERATION, 0, 60);
		}
	}

	/** Bộ Sấm Sét (đủ 4 món): cứ vài giây tự phóng sét đánh tối đa 2 quái ở gần. */
	private static void zap(ServerPlayerEntity p) {
		ServerWorld w = p.getEntityWorld();
		Vec3d c = new Vec3d(p.getX(), p.getY(), p.getZ());
		List<HostileEntity> hs = Weapons.hostiles(w, c, 7);
		if (hs.isEmpty() || Cd.use(p, "thunder_set", 120) > 0) return;
		hs.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(c.x, c.y, c.z)));
		UUID id = p.getUuid();
		for (int i = 0; i < Math.min(2, hs.size()); i++) {
			HostileEntity e = hs.get(i);
			Vec3d pos = new Vec3d(e.getX(), e.getY(), e.getZ());
			Weapons.bolt(w, pos.x, pos.y, pos.z);
			Weapons.hit(w, id, pos, 2.5, 8.0f, 0.5, 0);
		}
	}

	// ---------- né đòn (Hư Không) ----------

	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayerEntity sp)) return true;
		if (source.getAttacker() == null || pieces(sp, "void") < 4 || RNG.nextFloat() >= 0.20f) return true;
		Fx.burst(sp, ParticleTypes.PORTAL, 30);
		Fx.sound(sp, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f);
		Store.bar(sp, "NÉ ĐÒN!");
		return false;
	}

	// ---------- cứu khi sắp chết (Phượng Hoàng, Hư Không) ----------

	/** Trả về false nếu cái chết bị hủy. */
	public static boolean allowDeath(ServerPlayerEntity sp) {
		ServerWorld w = sp.getEntityWorld();
		Vec3d c = new Vec3d(sp.getX(), sp.getY(), sp.getZ());
		if (pieces(sp, "phoenix") >= 4 && Cd.use(sp, "phoenix", 6000) == 0) {
			sp.setHealth(sp.getMaxHealth());
			sp.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0));
			sp.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 2));
			Weapons.hit(w, sp.getUuid(), c, 9.0, 30.0f, 1.8, 8.0f);
			Fx.title(sp, "PHƯỢNG HOÀNG TÁI SINH", "Bạn hồi sinh trong biển lửa!", Formatting.GOLD);
			Fx.sound(sp, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f);
			Fx.burst(sp, ParticleTypes.FLAME, 80);
			Fx.helix(sp, ParticleTypes.FLAME, 36);
			Fx.expandingRing(w, c.x, c.y + 0.3, c.z, 9, ParticleTypes.FLAME);
			Fx.expandingRing(w, c.x, c.y + 0.7, c.z, 9, ParticleTypes.SOUL_FIRE_FLAME);
			return false;
		}
		if (pieces(sp, "void") >= 4 && Cd.use(sp, "voidescape", 4000) == 0) {
			sp.setHealth(10.0f);
			sp.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 4));
			sp.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 80, 0));
			sp.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 2));
			Fx.title(sp, "HƯ KHÔNG HỘ THÂN", "Bạn đã thoát chết!", Formatting.DARK_PURPLE);
			Fx.sound(sp, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f);
			Fx.burst(sp, ParticleTypes.PORTAL, 60);
			Fx.helix(sp, ParticleTypes.PORTAL, 30);
			Fx.expandingRing(w, c.x, c.y + 0.3, c.z, 8, ParticleTypes.PORTAL);
			return false;
		}
		return true;
	}

	// ---------- vòng hào quang trên đầu (đủ bộ Quyền Năng) ----------

	private static final Map<UUID, Integer> FLARE = new HashMap<>();

	public static void halo(ServerPlayerEntity p, int t) {
		ServerWorld w = p.getEntityWorld();
		double x = p.getX(), y = p.getY() + 2.55, z = p.getZ();
		boolean flare = FLARE.getOrDefault(p.getUuid(), 0) > Store.server.getTicks();
		double a0 = t * 0.10;
		double r = flare ? 1.05 : 0.75;
		int n = flare ? 28 : 18;
		for (int i = 0; i < n; i++) {
			double a = a0 + i * 2 * Math.PI / n;
			w.spawnParticles(i % 2 == 0 ? ParticleTypes.WAX_ON : ParticleTypes.END_ROD,
				x + Math.cos(a) * r, y, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
		}
		for (int i = 0; i < 10; i++) { // vòng trong xoay ngược
			double a = -a0 * 1.6 + i * Math.PI / 5;
			w.spawnParticles(ParticleTypes.FLAME, x + Math.cos(a) * (r - 0.25), y - 0.05, z + Math.sin(a) * (r - 0.25), 1, 0, 0, 0, 0);
		}
		for (int i = 0; i < 8; i++) { // tia vương miện
			double a = i * Math.PI / 4;
			w.spawnParticles(ParticleTypes.END_ROD, x + Math.cos(a) * r, y + 0.2, z + Math.sin(a) * r, 1, 0, 0.12, 0, 0);
		}
		if (flare) w.spawnParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 3, 0.2, 0.6, 0.2, 0.02);
	}

	// =====================================================================
	// Tuyệt chiêu riêng của từng bộ (đủ 4 món), dùng bằng vật phẩm tuyệt chiêu
	// =====================================================================

	private static final String[] SET_IDS = {"thunder", "void", "phoenix", "guardian", "shadow", "power"};

	/** Bộ giáp đang mặc đủ 4 món, hoặc null. */
	public static String fullSet(ServerPlayerEntity p) {
		for (String s : SET_IDS) if (pieces(p, s) == 4) return s;
		return null;
	}

	private static int skillCd(String set) {
		return switch (set) {
			case "thunder" -> 400;
			case "void" -> 600;
			case "phoenix" -> 900;
			case "guardian" -> 700;
			case "shadow" -> 500;
			default -> 1800;
		};
	}

	private static String skillName(String set) {
		return switch (set) {
			case "thunder" -> "Bão Điện";
			case "void" -> "Nuốt Chửng";
			case "phoenix" -> "Giáng Lâm";
			case "guardian" -> "Pháo Đài";
			case "shadow" -> "Biến Mất";
			default -> "Thức Tỉnh";
		};
	}

	public static String skillStatus(ServerPlayerEntity p, String set) {
		int left = Cd.remaining(p, "askill:" + set);
		return "Tuyệt chiêu " + skillName(set) + ": " + (left <= 0 ? "SẴN SÀNG" : left + "s");
	}

	/** Trả về thông báo lỗi, hoặc null nếu dùng thành công. */
	public static String useSkill(ServerPlayerEntity p, String set) {
		if (!p.isAlive() || pieces(p, set) < 4) return "Cần mặc đủ 4 món của bộ giáp này.";
		int left = Cd.use(p, "askill:" + set, skillCd(set));
		if (left > 0) return "Tuyệt chiêu " + skillName(set) + " còn hồi chiêu " + left + " giây.";
		switch (set) {
			case "thunder" -> stormDash(p);
			case "void" -> Weapons.blackHoleAt(p, new Vec3d(p.getX(), p.getY(), p.getZ()), 1.2);
			case "phoenix" -> phoenixDescend(p);
			case "guardian" -> fortress(p);
			case "shadow" -> {
				String err = vanish(p);
				if (err != null) {
					Cd.clear(p, "askill:shadow");
					return err;
				}
			}
			default -> awaken(p);
		}
		return null;
	}

	/** Sấm Sét: lướt xuyên qua quái, sét giật dọc đường đi. */
	private static void stormDash(ServerPlayerEntity p) {
		UUID id = p.getUuid();
		ServerWorld w0 = p.getEntityWorld();
		Vec3d look = p.getRotationVector();
		double len = Math.hypot(look.x, look.z);
		double dx = len < 1e-3 ? 0 : look.x / len;
		double dz = len < 1e-3 ? 0 : look.z / len;
		Fx.title(p, "BÃO ĐIỆN", "Tuyệt chiêu bộ Sấm Sét", Formatting.AQUA);
		Fx.sound(p, SoundEvents.ENTITY_WITHER_SPAWN, 1.8f);
		Weapons.bolt(w0, p.getX(), p.getY(), p.getZ());
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 3));
		for (int i = 0; i < 8; i++) {
			final int k = i;
			Fx.later(k, () -> {
				ServerPlayerEntity q = Fx.player(id);
				if (q == null || !q.isAlive()) return;
				ServerWorld ww = q.getEntityWorld();
				q.setVelocity(dx * 2.2, 0.08, dz * 2.2);
				q.velocityModified = true;
				Vec3d pos = new Vec3d(q.getX(), q.getY(), q.getZ());
				Weapons.hit(ww, id, pos, 3.0, 12.0f, 1.0, 0);
				ww.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.0, pos.z, 12, 0.4, 0.5, 0.4, 0.1);
				if (k % 3 == 0) Weapons.bolt(ww, pos.x, pos.y, pos.z);
			});
		}
	}

	/** Phượng Hoàng: cánh lửa sau lưng và mưa cầu lửa giáng xuống quái trong 5 giây. */
	private static void phoenixDescend(ServerPlayerEntity p) {
		UUID id = p.getUuid();
		Fx.title(p, "GIÁNG LÂM", "Tuyệt chiêu bộ Phượng Hoàng", Formatting.GOLD);
		Fx.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 160, 0));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
		for (int i = 0; i < 100; i += 2) {
			final int k = i;
			Fx.later(k, () -> {
				ServerPlayerEntity q = Fx.player(id);
				if (q == null || !q.isAlive()) return;
				ServerWorld w = q.getEntityWorld();
				wings(w, q, k);
				if (k % 6 != 0) return;
				List<HostileEntity> hs = Weapons.hostiles(w, new Vec3d(q.getX(), q.getY(), q.getZ()), 14);
				if (hs.isEmpty()) return;
				HostileEntity h = hs.get(RNG.nextInt(hs.size()));
				Vec3d t = new Vec3d(h.getX(), h.getY(), h.getZ());
				for (int s = 0; s < 6; s++) {
					w.spawnParticles(ParticleTypes.FLAME, t.x, t.y + 12 - s * 2, t.z, 8, 0.3, 0.3, 0.3, 0.02);
				}
				Weapons.hit(w, id, t, 3.0, 14.0f, 0.8, 5.0f);
				w.spawnParticles(ParticleTypes.LAVA, t.x, t.y + 0.5, t.z, 12, 0.6, 0.3, 0.6, 0.1);
				w.spawnParticles(ParticleTypes.FLAME, t.x, t.y + 0.5, t.z, 30, 1.0, 0.4, 1.0, 0.1);
			});
		}
	}

	private static void wings(ServerWorld w, ServerPlayerEntity q, int k) {
		double yaw = Math.toRadians(q.getYaw());
		double bx = Math.sin(yaw), bz = -Math.cos(yaw);   // hướng ra sau lưng
		double rx = -Math.cos(yaw), rz = -Math.sin(yaw);  // hướng sang phải
		double flap = Math.sin(k * 0.25) * 0.35;
		for (int side = -1; side <= 1; side += 2) {
			for (int j = 0; j < 9; j++) {
				double out = 0.35 + j * 0.32;
				double up = 1.45 + Math.sin(j * 0.55) * 0.55 + flap * (j / 8.0);
				double back = 0.25 + j * 0.06;
				w.spawnParticles(ParticleTypes.FLAME,
					q.getX() + rx * side * out + bx * back, q.getY() + up, q.getZ() + rz * side * out + bz * back,
					1, 0.02, 0.02, 0.02, 0.0);
			}
		}
	}

	/** Hộ Vệ: bất khả xâm phạm 5 giây trong vòm khiên, kết thúc bằng sóng đẩy lùi. */
	private static void fortress(ServerPlayerEntity p) {
		UUID id = p.getUuid();
		Fx.title(p, "PHÁO ĐÀI", "Tuyệt chiêu bộ Hộ Vệ", Formatting.WHITE);
		Fx.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 4));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 3));
		for (int i = 0; i < 100; i += 4) {
			Fx.later(i, () -> {
				ServerPlayerEntity q = Fx.player(id);
				if (q == null || !q.isAlive()) return;
				ServerWorld w = q.getEntityWorld();
				for (double h = 0.2; h <= 1.9; h += 0.85) {
					Fx.ringAt(w, q.getX(), q.getY() + h, q.getZ(), 2.8, ParticleTypes.END_ROD, 14);
				}
			});
		}
		Fx.later(100, () -> {
			ServerPlayerEntity q = Fx.player(id);
			if (q == null || !q.isAlive()) return;
			ServerWorld w = q.getEntityWorld();
			Vec3d c = new Vec3d(q.getX(), q.getY(), q.getZ());
			Weapons.hit(w, id, c, 10.0, 22.0f, 2.2, 0);
			Fx.expandingRing(w, c.x, c.y + 0.3, c.z, 10, ParticleTypes.END_ROD);
			Fx.expandingRing(w, c.x, c.y + 0.8, c.z, 10, ParticleTypes.CRIT);
			Fx.sound(q, SoundEvents.ENTITY_WITHER_SPAWN, 1.4f);
		});
	}

	/** Bóng Đêm: tàng hình, chớp ra sau lưng quái gần nhất và đâm một nhát chí mạng. */
	private static String vanish(ServerPlayerEntity p) {
		ServerWorld w = p.getEntityWorld();
		Vec3d c = new Vec3d(p.getX(), p.getY(), p.getZ());
		List<HostileEntity> hs = Weapons.hostiles(w, c, 16);
		if (hs.isEmpty()) return "Không có quái nào trong 16 block để ám sát.";
		hs.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(c.x, c.y, c.z)));
		HostileEntity e = hs.get(0);
		Vec3d ep = new Vec3d(e.getX(), e.getY(), e.getZ());
		double dx = ep.x - c.x, dz = ep.z - c.z;
		double len = Math.max(0.1, Math.hypot(dx, dz));
		double tx = ep.x + dx / len * 1.8;
		double tz = ep.z + dz / len * 1.8;
		UUID id = p.getUuid();
		Fx.title(p, "BIẾN MẤT", "Tuyệt chiêu bộ Bóng Đêm", Formatting.DARK_GRAY);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 140, 0));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 140, 2));
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1.0, c.z, 40, 0.4, 0.8, 0.4, 0.05);
		p.requestTeleport(tx, ep.y, tz);
		Fx.later(2, () -> {
			ServerPlayerEntity q = Fx.player(id);
			if (q == null) return;
			ServerWorld ww = q.getEntityWorld();
			Weapons.hit(ww, id, ep, 3.0, 28.0f, 0.4, 0);
			ww.spawnParticles(ParticleTypes.CRIT, ep.x, ep.y + 1.0, ep.z, 40, 0.5, 0.6, 0.5, 0.3);
			ww.spawnParticles(ParticleTypes.SONIC_BOOM, ep.x, ep.y + 1.0, ep.z, 1, 0, 0, 0, 0);
			Fx.sound(q, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f);
		});
		return null;
	}

	/** Quyền Năng: thức tỉnh 15 giây, hồi mọi chiêu, vòng hào quang bùng sáng và bão sét. */
	private static void awaken(ServerPlayerEntity p) {
		ServerWorld w = p.getEntityWorld();
		Vec3d c = new Vec3d(p.getX(), p.getY(), p.getZ());
		FLARE.put(p.getUuid(), Store.server.getTicks() + 300);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 1));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 1));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 1));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 300, 1));
		for (int i = 0; i < 5; i++) Cd.clear(p, "wand:" + i);
		Cd.clear(p, "thunder");
		Cd.clear(p, "blink");
		Cd.clear(p, "skill:dash");
		Cd.clear(p, "skill:heal");
		Cd.clear(p, "skill:shock");
		Fx.title(p, "THỨC TỈNH QUYỀN NĂNG", "Mọi chiêu thức đã được làm mới!", Formatting.YELLOW);
		Fx.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f);
		Fx.sound(p, SoundEvents.ENTITY_WITHER_SPAWN, 1.2f);
		Fx.burst(p, ParticleTypes.TOTEM_OF_UNDYING, 80);
		Fx.helix(p, ParticleTypes.END_ROD, 40);
		Fx.expandingRing(w, c.x, c.y + 0.3, c.z, 14, ParticleTypes.WAX_ON);
		Fx.later(3, () -> Fx.expandingRing(w, c.x, c.y + 1.0, c.z, 14, ParticleTypes.END_ROD));
		Weapons.storm(p, 1.5);
	}
}
