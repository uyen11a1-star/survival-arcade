package com.nguyenquochuy.survivalarcade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Store {
	public static class PData {
		public int points, xp, kills, mined, quests, bestWave, delivered, rushWins, surviveWins, forgeWins;
		public String name;
		public Set<String> ach = new HashSet<>();
		public String dailyDate = "";
		public String dailyId = "";
		public int dailyProgress;
		public boolean dailyDone;
		public String lastLogin = "";
		public int streak;
		public String weeklyKey = "";
		public String weeklyId = "";
		public int weeklyProgress;
		public boolean weeklyDone;
	}

	public record Weekly(String name, String cat, String catLabel) {}

	public static final Map<String, String> ACH = new LinkedHashMap<>();
	static {
		ACH.put("first_quest", "Thử thách đầu tiên");
		ACH.put("quests_10", "Thợ săn nhiệm vụ (10 thử thách ngày)");
		ACH.put("kills_100", "Sát thủ (100 kill)");
		ACH.put("wave_5", "Đấu sĩ (đạt đợt 5)");
		ACH.put("wave_10", "Huyền thoại đấu trường (đạt đợt 10)");
		ACH.put("survivor", "Kẻ sống sót");
		ACH.put("rusher", "Tay đua tài nguyên");
		ACH.put("smith", "Thợ rèn chớp nhoáng");
		ACH.put("courier", "Người giao hàng");
		ACH.put("far_courier", "Hành trình xa (giao hàng từ 300 block)");
		ACH.put("lv_5", "Cấp 5");
		ACH.put("lv_10", "Cấp 10");
		ACH.put("boss_slayer", "Kẻ diệt boss");
		ACH.put("bounty_hunter", "Thợ săn tiền thưởng");
		ACH.put("weekly_1", "Nhiệm vụ tuần đầu tiên");
		ACH.put("streak_7", "Chuỗi đăng nhập 7 ngày");
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Map<String, PData> data = new HashMap<>();
	private static Path file;
	public static MinecraftServer server;

	private Store() {}

	// ---------- lưu / tải ----------

	public static void load(MinecraftServer s) {
		server = s;
		file = s.getSavePath(WorldSavePath.ROOT).resolve("survival_arcade.json");
		if (Files.exists(file)) {
			try (Reader r = Files.newBufferedReader(file)) {
				Map<String, PData> m = GSON.fromJson(r, new TypeToken<Map<String, PData>>() {}.getType());
				if (m != null) data = m;
			} catch (Exception e) {
				SurvivalArcade.LOG.error("Không đọc được survival_arcade.json", e);
			}
		}
	}

	public static void save() {
		if (file == null) return;
		try (Writer w = Files.newBufferedWriter(file)) {
			GSON.toJson(data, w);
		} catch (Exception e) {
			SurvivalArcade.LOG.error("Không ghi được survival_arcade.json", e);
		}
	}

	public static PData get(ServerPlayerEntity p) {
		PData d = data.computeIfAbsent(p.getUuid().toString(), k -> new PData());
		d.name = p.getName().getString();
		return d;
	}

	/** Bảng xếp hạng theo tổng XP. */
	public static List<String> top(int n) {
		List<PData> l = new ArrayList<>();
		for (PData d : data.values()) if (d.xp > 0) l.add(d);
		l.sort((a, b) -> Integer.compare(b.xp, a.xp));
		List<String> out = new ArrayList<>();
		for (int i = 0; i < Math.min(n, l.size()); i++) {
			PData d = l.get(i);
			out.add((i + 1) + ". " + (d.name == null ? "???" : d.name) + " — cấp " + level(d.xp) + " (" + d.xp + " XP)");
		}
		return out;
	}

	// ---------- cấp độ ----------

	public static int level(int xp) {
		return Math.min(30, (int) Math.floor(Math.sqrt(xp / 50.0)) + 1);
	}

	/** XP tổng cần để đạt cấp l. */
	public static int xpFor(int l) {
		return 50 * (l - 1) * (l - 1);
	}

	// ---------- sự kiện tuần ----------

	public static Weekly weekly() {
		int w = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
		return switch (w % 4) {
			case 0 -> new Weekly("Tuần Chiến Đấu", "combat", "chiến đấu");
			case 1 -> new Weekly("Tuần Khai Thác", "mining", "khai thác");
			case 2 -> new Weekly("Tuần Phiêu Lưu", "adventure", "giao hàng");
			default -> new Weekly("Tuần Thợ Rèn", "craft", "rèn đồ");
		};
	}

	// ---------- điểm ----------

	/** Cộng điểm (và XP cấp độ). Trả về số điểm thực nhận sau khi nhân sự kiện tuần. */
	public static int addPoints(ServerPlayerEntity p, int base, String cat) {
		int amt = (cat != null && cat.equals(weekly().cat())) ? base * 2 : base;
		PData d = get(p);
		int before = level(d.xp);
		d.points += amt;
		d.xp += amt;
		int after = level(d.xp);
		if (after > before) {
			say(p, "Lên cấp " + after + "!", Formatting.GOLD);
			Fx.levelUp(p, after);
			if (after % 5 == 0 && server != null) {
				server.getPlayerManager().broadcast(Text.literal("✦ " + p.getName().getString() + " đã đạt cấp " + after + "!").formatted(Formatting.LIGHT_PURPLE), false);
			}
		}
		if (after >= 5) grant(p, "lv_5");
		if (after >= 10) grant(p, "lv_10");
		return amt;
	}

	public static void grant(ServerPlayerEntity p, String id) {
		PData d = get(p);
		if (!ACH.containsKey(id) || !d.ach.add(id)) return;
		d.points += 10;
		d.xp += 10;
		say(p, "Thành tựu: " + ACH.get(id) + " (+10 điểm)", Formatting.AQUA);
		Fx.achievement(p);
	}

	// ---------- hiệu ứng vĩnh viễn theo cấp ----------

	public static void applyPerks(ServerPlayerEntity p) {
		int lv = level(get(p).xp);
		if (lv >= 3) fx(p, StatusEffects.SPEED);
		if (lv >= 5) fx(p, StatusEffects.HASTE);
		if (lv >= 8) fx(p, StatusEffects.RESISTANCE);
		if (lv >= 12) fx(p, StatusEffects.NIGHT_VISION);
	}

	private static void fx(ServerPlayerEntity p, RegistryEntry<StatusEffect> e) {
		p.addStatusEffect(new StatusEffectInstance(e, 400, 0, true, false, false));
	}

	// ---------- tiện ích gửi tin ----------

	public static void say(ServerPlayerEntity p, String s, Formatting f) {
		p.sendMessage(Text.literal(s).formatted(f));
	}

	public static void bar(ServerPlayerEntity p, String s) {
		p.sendMessage(Text.literal(s), true);
	}
}
