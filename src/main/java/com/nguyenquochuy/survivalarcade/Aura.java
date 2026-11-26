package com.nguyenquochuy.survivalarcade;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Hào quang quanh người: tự động theo cấp, cấp càng cao càng nhiều lớp và càng đẹp. */
public final class Aura {
	private Aura() {}

	/** Gọi mỗi 3 tick cho từng người chơi. */
	public static void tick(ServerPlayerEntity p, int t) {
		if (Armor.pieces(p, "power") >= 4) Armor.halo(p, t);
		int lv = Store.level(Store.get(p).xp);
		if (lv < 2) return;
		ServerWorld w = p.getEntityWorld();
		double x = p.getX(), y = p.getY(), z = p.getZ();

		// vệt hạt dưới chân, đổi kiểu theo cấp
		ParticleEffect trail;
		if (lv >= 30) trail = ParticleTypes.TOTEM_OF_UNDYING;
		else if (lv >= 20) trail = ParticleTypes.SOUL_FIRE_FLAME;
		else if (lv >= 15) trail = ParticleTypes.ENCHANT;
		else if (lv >= 10) trail = ParticleTypes.FLAME;
		else if (lv >= 5) trail = ParticleTypes.END_ROD;
		else trail = ParticleTypes.HAPPY_VILLAGER;
		w.spawnParticles(trail, x, y + 0.1, z, 1 + lv / 10, 0.25, 0.02, 0.25, 0.01);

		// các vòng hạt xoay quanh người (cấp 5: 1 vòng, 10: 2 vòng, 20: 3 vòng)
		int orbits = lv >= 20 ? 3 : lv >= 10 ? 2 : lv >= 5 ? 1 : 0;
		double a = t * 0.3;
		for (int i = 0; i < orbits; i++) {
			double ang = a + i * 2 * Math.PI / orbits;
			double oy = y + 1.0 + Math.sin(a * 0.5 + i) * 0.6;
			w.spawnParticles(ParticleTypes.END_ROD, x + Math.cos(ang) * 0.9, oy, z + Math.sin(ang) * 0.9, 1, 0, 0, 0, 0);
		}

		// vương miện chữ phù phía trên đầu (cấp 15+)
		if (lv >= 15) {
			w.spawnParticles(ParticleTypes.ENCHANT, x, y + 2.2, z, 2, 0.3, 0.1, 0.3, 0.1);
		}
		// pháo hoa nhỏ lấp lánh (cấp 30)
		if (lv >= 30 && t % 9 == 0) {
			w.spawnParticles(ParticleTypes.FIREWORK, x, y + 1.0, z, 3, 0.6, 0.8, 0.6, 0.02);
		}
	}
}
