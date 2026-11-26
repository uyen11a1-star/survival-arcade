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
import java.util.List;
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

	public static void halo(ServerPlayerEntity p, int t) {
		ServerWorld w = p.getEntityWorld();
		double x = p.getX(), y = p.getY() + 2.45, z = p.getZ();
		double a0 = t * 0.12;
		for (int i = 0; i < 12; i++) {
			double a = a0 + i * Math.PI / 6;
			w.spawnParticles(ParticleTypes.WAX_ON, x + Math.cos(a) * 0.6, y, z + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
		}
		if (t % 6 == 0) w.spawnParticles(ParticleTypes.END_ROD, x, y + 0.15, z, 2, 0.3, 0.05, 0.3, 0.0);
	}
}
