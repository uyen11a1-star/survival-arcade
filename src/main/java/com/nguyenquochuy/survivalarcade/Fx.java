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

/** Hiệu ứng nhìn/nghe: title lớn giữa màn hình, âm thanh, hạt. */
public final class Fx {
	private Fx() {}

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

	public static void win(ServerPlayerEntity p, String title, String sub) {
		title(p, title, sub, Formatting.GOLD);
		sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
		burst(p, ParticleTypes.TOTEM_OF_UNDYING, 40);
	}

	public static void lose(ServerPlayerEntity p, String sub) {
		title(p, "THẤT BẠI", sub, Formatting.RED);
		sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1.0f);
	}

	public static void levelUp(ServerPlayerEntity p, int lvl) {
		String sub = switch (lvl) {
			case 3 -> "Mở khóa buff Tốc độ";
			case 5 -> "Mở khóa buff Đào nhanh";
			case 8 -> "Mở khóa buff Giáp";
			case 12 -> "Mở khóa buff Nhìn đêm";
			default -> "Chúc mừng!";
		};
		title(p, "LÊN CẤP " + lvl, sub, Formatting.GOLD);
		sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f);
		burst(p, ParticleTypes.TOTEM_OF_UNDYING, 30);
	}

	public static void achievement(ServerPlayerEntity p) {
		sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f);
		burst(p, ParticleTypes.HAPPY_VILLAGER, 25);
	}

	public static void waveStart(ServerPlayerEntity p, int wave, int count) {
		title(p, "ĐỢT " + wave, count + " quái đang kéo đến", Formatting.RED);
		sound(p, SoundEvents.ENTITY_WITHER_SPAWN, 1.5f);
	}

	public static void waveClear(ServerPlayerEntity p, int wave) {
		sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f);
		burst(p, ParticleTypes.HAPPY_VILLAGER, 20);
	}
}
