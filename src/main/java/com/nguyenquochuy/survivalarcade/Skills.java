package com.nguyenquochuy.survivalarcade;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/** Kỹ năng chủ động: dùng bằng sổ tay, lệnh /mg skill, hoặc 3 vật phẩm kỹ năng trên thanh nhanh. */
public final class Skills {
	public record Skill(String id, String name, int minLevel, int cooldown) {}

	public static final List<Skill> ALL = List.of(
		new Skill("dash", "Lướt nhanh", 1, 60),
		new Skill("heal", "Hồi máu", 3, 300),
		new Skill("shock", "Sóng xung kích", 5, 200)
	);

	/** Số bậc tối đa của cây kỹ năng. */
	public static final int MAX_TREE = 5;

	private Skills() {}

	private static Skill find(String id) {
		for (Skill s : ALL) if (s.id().equals(id)) return s;
		return null;
	}

	public static int treeLv(ServerPlayerEntity p, String id) {
		Store.PData d = Store.get(p);
		return switch (id) {
			case "dash" -> d.dashLv;
			case "heal" -> d.healLv;
			default -> d.shockLv;
		};
	}

	/** Nâng một bậc trong cây kỹ năng bằng điểm. Trả về thông báo kết quả. */
	public static String upgrade(ServerPlayerEntity p, String id) {
		Skill s = find(id);
		if (s == null) return "Kỹ năng không tồn tại.";
		Store.PData d = Store.get(p);
		int lv = treeLv(p, id);
		if (lv >= MAX_TREE) return s.name() + " đã đạt bậc tối đa.";
		int cost = 30 * (lv + 1);
		if (d.points < cost) return "Cần " + cost + " điểm để nâng " + s.name() + " (bạn có " + d.points + ").";
		d.points -= cost;
		switch (id) {
			case "dash" -> d.dashLv++;
			case "heal" -> d.healLv++;
			default -> d.shockLv++;
		}
		Fx.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.4f);
		Fx.burst(p, ParticleTypes.HAPPY_VILLAGER, 20);
		return "Đã nâng " + s.name() + " lên bậc " + (lv + 1) + " (-" + cost + " điểm).";
	}

	/** Dòng trạng thái ngắn cho thanh thông báo. */
	public static String status(ServerPlayerEntity p, String id) {
		Skill s = find(id);
		if (s == null) return "";
		int lv = Store.level(Store.get(p).xp);
		if (lv < s.minLevel()) return s.name() + ": cần cấp " + s.minLevel();
		int left = Cd.remaining(p, "skill:" + id);
		return s.name() + ": " + (left <= 0 ? "SẴN SÀNG" : left + "s");
	}

	/** Trả về thông báo lỗi, hoặc null nếu dùng thành công. */
	public static String use(ServerPlayerEntity p, String id) {
		Skill s = find(id);
		if (s == null) return "Kỹ năng không tồn tại.";
		if (!p.isAlive()) return "Bạn không thể dùng kỹ năng lúc này.";
		int lv = Store.level(Store.get(p).xp);
		if (lv < s.minLevel()) return "Cần cấp " + s.minLevel() + " để dùng " + s.name() + " (bạn đang cấp " + lv + ").";
		int tl = treeLv(p, id);
		int cd = Math.max(20, (int) (s.cooldown() * (1.0 - 0.08 * tl)));
		int left = Cd.use(p, "skill:" + id, cd);
		if (left > 0) return s.name() + " còn hồi chiêu " + left + " giây.";
		switch (id) {
			case "dash" -> dash(p, tl);
			case "heal" -> heal(p, lv, tl);
			default -> shock(p, lv, tl);
		}
		return null;
	}

	private static void dash(ServerPlayerEntity p, int tl) {
		Vec3d look = p.getRotationVector();
		double len = Math.hypot(look.x, look.z);
		double dx = len < 1e-3 ? 0 : look.x / len;
		double dz = len < 1e-3 ? 0 : look.z / len;
		double speed = 1.9 + 0.15 * tl;
		Fx.burst(p, ParticleTypes.CLOUD, 25);
		p.setVelocity(dx * speed, 0.35, dz * speed);
		p.velocityModified = true;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, false, false));
		Fx.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f);
		UUID id = p.getUuid();
		for (int i = 1; i <= 8; i++) {
			Fx.later(i, () -> {
				ServerPlayerEntity q = Fx.player(id);
				if (q == null) return;
				q.getEntityWorld().spawnParticles(ParticleTypes.END_ROD, q.getX(), q.getY() + 0.8, q.getZ(), 4, 0.2, 0.3, 0.2, 0.02);
			});
		}
	}

	private static void heal(ServerPlayerEntity p, int lv, int tl) {
		p.heal(6.0f + lv * 0.5f + tl * 1.5f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
		Fx.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.6f);
		Fx.helix(p, ParticleTypes.HEART, 24);
		Fx.burst(p, ParticleTypes.HAPPY_VILLAGER, 20);
	}

	private static void shock(ServerPlayerEntity p, int lv, int tl) {
		ServerWorld w = p.getEntityWorld();
		double x = p.getX(), y = p.getY(), z = p.getZ();
		float dmg = 8.0f + lv * 0.5f + tl * 2.0f;
		Box box = new Box(x - 7, y - 3, z - 7, x + 7, y + 3, z + 7);
		DamageSource src = w.getDamageSources().playerAttack(p);
		for (HostileEntity e : w.getEntitiesByClass(HostileEntity.class, box, h -> h.isAlive())) {
			double dx = e.getX() - x;
			double dz = e.getZ() - z;
			double len = Math.max(0.1, Math.hypot(dx, dz));
			e.damage(w, src, dmg);
			e.setVelocity(dx / len * 1.3, 0.55, dz / len * 1.3);
			e.velocityModified = true;
		}
		Fx.sound(p, SoundEvents.ENTITY_WITHER_SPAWN, 2.0f);
		w.spawnParticles(ParticleTypes.SONIC_BOOM, x, y + 1.0, z, 1, 0, 0, 0, 0);
		Fx.expandingRing(w, x, y + 0.2, z, 7, ParticleTypes.CRIT);
		Fx.expandingRing(w, x, y + 0.6, z, 7, ParticleTypes.END_ROD);
	}
}
