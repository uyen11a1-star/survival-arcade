package com.nguyenquochuy.survivalarcade;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Hiệu ứng nhìn/nghe: title lớn, âm thanh, hạt, và hoạt ảnh theo từng tick. */
public final class Fx {
	private record Task(int when, Runnable r) {}

	private static final List<Task> TASKS = new ArrayList<>();
	private static int now = 0;

	private Fx() {}

	// ---------- bộ lập lịch hoạt ảnh ----------

	/** Gọi mỗi tick từ Games.tick. */
	public static void tick() {
		now++;
		if (TASKS.isEmpty()) return;
		List<Task> due = new ArrayList<>();
		for (Task t : TASKS) {
			if (t.when() <= now) due.add(t);
		}
		TASKS.removeAll(due);
		for (Task t : due) {
			try {
				t.r().run();
			} catch (Exception e) {
				SurvivalArcade.LOG.error("Fx task bị lỗi", e);
			}
		}
	}

	public static void later(int delayTicks, Runnable r) {
		TASKS.add(new Task(now + Math.max(0, delayTicks), r));
	}

	static ServerPlayerEntity player(UUID id) {
		return Store.server == null ? null : Store.server.getPlayerManager().getPlayer(id);
	}

	// ---------- cơ bản ----------

	public static void title(ServerPlayerEntity p, String title, String sub, Formatting color) {
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 50, 15));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub).formatted(Formatting.WHITE)));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(color, Formatting.BOLD)));
	}

	public static void sound(ServerPlayerEntity p, SoundEvent s, float pitch) {
		p.playSoundToPlayer(s, SoundCategory.MASTER, 1.0f, pitch);
	}

	public static void burst(ServerPlayerEntity p, ParticleEffect type, int count) {
		ServerWorld w = p.getEntityWorld();
		w.spawnParticles(type, p.getX(), p.getY() + 1.0, p.getZ(), count, 0.5, 0.7, 0.5, 0.1);
	}

	// ---------- hoạt ảnh hạt ----------

	/** Xoắn ốc đôi bay lên quanh người chơi trong `steps` tick. */
	public static void helix(ServerPlayerEntity p, ParticleEffect type, int steps) {
		UUID id = p.getUuid();
		for (int i = 0; i < steps; i++) {
			final int k = i;
			later(k, () -> {
				ServerPlayerEntity q = player(id);
				if (q == null || !q.isAlive()) return;
				ServerWorld w = q.getEntityWorld();
				double a = k * 0.55;
				double y = q.getY() + k * 0.07;
				double r = 1.0 - k * 0.005;
				w.spawnParticles(type, q.getX() + Math.cos(a) * r, y, q.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
				w.spawnParticles(type, q.getX() + Math.cos(a + Math.PI) * r, y, q.getZ() + Math.sin(a + Math.PI) * r, 1, 0, 0, 0, 0);
			});
		}
	}

	public static void ringAt(ServerWorld w, double x, double y, double z, double radius, ParticleEffect type, int points) {
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			w.spawnParticles(type, x + Math.cos(a) * radius, y, z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	/** Vòng sóng lan ra từ tâm, mỗi tick rộng thêm 1 block. */
	public static void expandingRing(ServerWorld w, double x, double y, double z, double maxR, ParticleEffect type) {
		for (int r = 1; r <= (int) maxR; r++) {
			final int rr = r;
			later(rr, () -> ringAt(w, x, y, z, rr, type, 10 + rr * 6));
		}
	}

	// ---------- hiệu ứng theo sự kiện ----------

	public static void win(ServerPlayerEntity p, String title, String sub) {
		title(p, title, sub, Formatting.GOLD);
		sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
		burst(p, ParticleTypes.TOTEM_OF_UNDYING, 40);
		helix(p, ParticleTypes.END_ROD, 30);
	}

	public static void lose(ServerPlayerEntity p, String sub) {
		title(p, "THẤT BẠI", sub, Formatting.RED);
		sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1.0f);
	}

	public static void levelUp(ServerPlayerEntity p, int lvl) {
		String sub = switch (lvl) {
			case 3 -> "Mở khóa buff Tốc độ và kỹ năng Hồi máu";
			case 5 -> "Mở khóa buff Đào nhanh và Sóng xung kích";
			case 8 -> "Mở khóa buff Giáp";
			case 12 -> "Mở khóa buff Nhìn đêm";
			default -> "Chúc mừng!";
		};
		title(p, "LÊN CẤP " + lvl, sub, Formatting.GOLD);
		sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f);
		burst(p, ParticleTypes.TOTEM_OF_UNDYING, 30);
		helix(p, ParticleTypes.END_ROD, 36);
		later(10, () -> {
			ServerPlayerEntity q = player(p.getUuid());
			if (q != null) helix(q, ParticleTypes.ENCHANT, 26);
		});
	}

	public static void achievement(ServerPlayerEntity p) {
		sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f);
		burst(p, ParticleTypes.HAPPY_VILLAGER, 25);
		helix(p, ParticleTypes.HAPPY_VILLAGER, 24);
	}

	public static void waveStart(ServerPlayerEntity p, int wave, int count) {
		title(p, "ĐỢT " + wave, count + " quái đang kéo đến", Formatting.RED);
		sound(p, SoundEvents.ENTITY_WITHER_SPAWN, 1.5f);
	}

	public static void waveClear(ServerPlayerEntity p, int wave) {
		sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f);
		burst(p, ParticleTypes.HAPPY_VILLAGER, 20);
	}

	public static void loginReward(ServerPlayerEntity p, boolean big) {
		sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, big ? 0.8f : 1.2f);
		helix(p, ParticleTypes.END_ROD, 34);
		if (big) {
			burst(p, ParticleTypes.TOTEM_OF_UNDYING, 60);
			later(8, () -> {
				ServerPlayerEntity q = player(p.getUuid());
				if (q != null) helix(q, ParticleTypes.ENCHANT, 30);
			});
		}
	}
}
