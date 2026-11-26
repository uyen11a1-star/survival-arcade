package com.nguyenquochuy.survivalarcade;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

/** Hồi chiêu dùng chung cho kỹ năng, vũ khí và bùa. */
public final class Cd {
	private static final Map<String, Integer> READY = new HashMap<>();

	private Cd() {}

	private static String key(ServerPlayerEntity p, String k) {
		return p.getUuid() + ":" + k;
	}

	/** Trả về số giây còn lại nếu đang hồi chiêu; nếu dùng được thì đặt hồi chiêu mới và trả về 0. */
	public static int use(ServerPlayerEntity p, String k, int ticks) {
		int now = Store.server.getTicks();
		int ready = READY.getOrDefault(key(p, k), 0);
		if (now < ready) return (ready - now) / 20 + 1;
		READY.put(key(p, k), now + ticks);
		return 0;
	}

	/** Số giây còn lại (0 nếu đã sẵn sàng). */
	public static int remaining(ServerPlayerEntity p, String k) {
		int now = Store.server.getTicks();
		int ready = READY.getOrDefault(key(p, k), 0);
		return now < ready ? (ready - now) / 20 + 1 : 0;
	}

	public static void clear(ServerPlayerEntity p, String k) {
		READY.remove(key(p, k));
	}
}
