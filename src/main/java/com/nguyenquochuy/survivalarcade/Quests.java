package com.nguyenquochuy.survivalarcade;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Quests {
	public record Q(String id, String text, int target, int pts, String cat, String rewardText, Supplier<ItemStack> reward) {}

	public static final List<Q> ALL = List.of(
		new Q("kill_hostile", "Hạ 15 quái vật", 15, 25, "combat", "1 táo vàng", () -> new ItemStack(Items.GOLDEN_APPLE, 1)),
		new Q("kill_zombie", "Hạ 10 zombie", 10, 20, "combat", "8 bò nướng", () -> new ItemStack(Items.COOKED_BEEF, 8)),
		new Q("kill_skeleton", "Hạ 8 skeleton", 8, 25, "combat", "32 mũi tên", () -> new ItemStack(Items.ARROW, 32)),
		new Q("mine_stone", "Đào 64 khối đá", 64, 20, "mining", "3 thỏi sắt", () -> new ItemStack(Items.IRON_INGOT, 3)),
		new Q("mine_logs", "Chặt 32 khúc gỗ", 32, 20, "mining", "6 cà rốt vàng", () -> new ItemStack(Items.GOLDEN_CARROT, 6)),
		new Q("mine_coal", "Đào 16 quặng than", 16, 25, "mining", "4 thỏi sắt", () -> new ItemStack(Items.IRON_INGOT, 4)),
		new Q("mine_iron", "Đào 8 quặng sắt", 8, 35, "mining", "1 kim cương", () -> new ItemStack(Items.DIAMOND, 1))
	);

	private Quests() {}

	public static Q find(String id) {
		for (Q q : ALL) if (q.id().equals(id)) return q;
		return null;
	}

	/** Đảm bảo người chơi có thử thách của hôm nay (cố định theo ngày + UUID, không reroll được bằng relog). */
	public static void ensure(Store.PData d, UUID id) {
		String today = LocalDate.now().toString();
		if (today.equals(d.dailyDate) && find(d.dailyId) != null) return;
		Random r = new Random(id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ today.hashCode());
		Q q = ALL.get(r.nextInt(ALL.size()));
		d.dailyDate = today;
		d.dailyId = q.id();
		d.dailyProgress = 0;
		d.dailyDone = false;
	}

	public static String describe(ServerPlayerEntity p) {
		Store.PData d = Store.get(p);
		ensure(d, p.getUuid());
		Q q = find(d.dailyId);
		if (d.dailyDone) return q.text() + " — ĐÃ XONG";
		return q.text() + " (" + d.dailyProgress + "/" + q.target() + ") — thưởng " + q.pts() + " điểm + " + q.rewardText();
	}

	static boolean matchKill(String id, Entity e) {
		EntityType<?> t = e.getType();
		return switch (id) {
			case "kill_hostile" -> e instanceof HostileEntity;
			case "kill_zombie" -> t == EntityType.ZOMBIE;
			case "kill_skeleton" -> t == EntityType.SKELETON;
			default -> false;
		};
	}

	static boolean matchMine(String id, BlockState s) {
		return switch (id) {
			case "mine_stone" -> s.isIn(BlockTags.BASE_STONE_OVERWORLD);
			case "mine_logs" -> s.isIn(BlockTags.LOGS);
			case "mine_coal" -> s.isOf(Blocks.COAL_ORE) || s.isOf(Blocks.DEEPSLATE_COAL_ORE);
			case "mine_iron" -> s.isOf(Blocks.IRON_ORE) || s.isOf(Blocks.DEEPSLATE_IRON_ORE);
			default -> false;
		};
	}

	public static void onKill(ServerPlayerEntity p, Entity victim) {
		Store.PData d = Store.get(p);
		d.kills++;
		if (d.kills >= 100) Store.grant(p, "kills_100");
		progress(p, id -> matchKill(id, victim));
		weekly(p, id -> matchKill(id, victim));
	}

	public static void onMine(ServerPlayerEntity p, BlockState state) {
		Store.get(p).mined++;
		progress(p, id -> matchMine(id, state));
		weekly(p, id -> matchMine(id, state));
	}

	private static void progress(ServerPlayerEntity p, Predicate<String> match) {
		Store.PData d = Store.get(p);
		ensure(d, p.getUuid());
		if (d.dailyDone || !match.test(d.dailyId)) return;
		Q q = find(d.dailyId);
		d.dailyProgress++;
		if (d.dailyProgress < q.target()) {
			Store.bar(p, "Thử thách ngày: " + d.dailyProgress + "/" + q.target());
			return;
		}
		d.dailyDone = true;
		d.quests++;
		int got = Store.addPoints(p, q.pts(), q.cat());
		p.getInventory().offerOrDrop(q.reward().get());
		p.addExperience(30);
		Store.say(p, "Xong thử thách ngày: " + q.text() + "! +" + got + " điểm, +" + q.rewardText() + ", +30 XP", Formatting.GREEN);
		Fx.win(p, "HOÀN THÀNH", q.text());
		Store.grant(p, "first_quest");
		if (d.quests >= 10) Store.grant(p, "quests_10");
	}

	// =====================================================================
	// Nhiệm vụ tuần
	// =====================================================================

	public static final List<Q> WEEKLY = List.of(
		new Q("kill_hostile", "Hạ 150 quái vật", 150, 150, "combat", "3 kim cương", () -> new ItemStack(Items.DIAMOND, 3)),
		new Q("kill_skeleton", "Hạ 40 skeleton", 40, 140, "combat", "4 táo vàng", () -> new ItemStack(Items.GOLDEN_APPLE, 4)),
		new Q("mine_stone", "Đào 400 khối đá", 400, 130, "mining", "12 thỏi sắt", () -> new ItemStack(Items.IRON_INGOT, 12)),
		new Q("mine_iron", "Đào 60 quặng sắt", 60, 170, "mining", "4 kim cương", () -> new ItemStack(Items.DIAMOND, 4)),
		new Q("mine_logs", "Chặt 200 khúc gỗ", 200, 120, "mining", "16 cà rốt vàng", () -> new ItemStack(Items.GOLDEN_CARROT, 16)),
		new Q("arena_wave", "Vượt qua đợt 8 ở Đấu trường", 8, 160, "combat", "3 kim cương", () -> new ItemStack(Items.DIAMOND, 3))
	);

	private static String weekKey() {
		LocalDate d = LocalDate.now();
		return d.get(IsoFields.WEEK_BASED_YEAR) + "-W" + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
	}

	private static Q findWeekly(String id) {
		for (Q q : WEEKLY) if (q.id().equals(id)) return q;
		return null;
	}

	public static void ensureWeekly(Store.PData d, UUID id) {
		String key = weekKey();
		if (key.equals(d.weeklyKey) && findWeekly(d.weeklyId) != null) return;
		Random r = new Random(id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ (key.hashCode() * 31L));
		Q q = WEEKLY.get(r.nextInt(WEEKLY.size()));
		d.weeklyKey = key;
		d.weeklyId = q.id();
		d.weeklyProgress = 0;
		d.weeklyDone = false;
	}

	public static String describeWeekly(ServerPlayerEntity p) {
		Store.PData d = Store.get(p);
		ensureWeekly(d, p.getUuid());
		Q q = findWeekly(d.weeklyId);
		if (d.weeklyDone) return q.text() + " — ĐÃ XONG";
		return q.text() + " (" + d.weeklyProgress + "/" + q.target() + ") — thưởng " + q.pts() + " điểm + " + q.rewardText();
	}

	private static void weekly(ServerPlayerEntity p, Predicate<String> match) {
		Store.PData d = Store.get(p);
		ensureWeekly(d, p.getUuid());
		if (d.weeklyDone || "arena_wave".equals(d.weeklyId) || !match.test(d.weeklyId)) return;
		d.weeklyProgress++;
		checkWeekly(p, d);
	}

	/** Gọi khi vượt qua một đợt ở Đấu trường. */
	public static void arenaWave(ServerPlayerEntity p, int cleared) {
		Store.PData d = Store.get(p);
		ensureWeekly(d, p.getUuid());
		if (d.weeklyDone || !"arena_wave".equals(d.weeklyId)) return;
		d.weeklyProgress = Math.max(d.weeklyProgress, cleared);
		checkWeekly(p, d);
	}

	private static void checkWeekly(ServerPlayerEntity p, Store.PData d) {
		Q q = findWeekly(d.weeklyId);
		if (d.weeklyProgress < q.target()) {
			if (d.weeklyProgress % 10 == 0) Store.bar(p, "Nhiệm vụ tuần: " + d.weeklyProgress + "/" + q.target());
			return;
		}
		d.weeklyDone = true;
		int got = Store.addPoints(p, q.pts(), q.cat());
		p.getInventory().offerOrDrop(q.reward().get());
		p.addExperience(100);
		Store.say(p, "Xong nhiệm vụ tuần: " + q.text() + "! +" + got + " điểm, +" + q.rewardText() + ", +100 XP", Formatting.GREEN);
		Fx.win(p, "NHIỆM VỤ TUẦN", q.text());
		Store.grant(p, "weekly_1");
	}

	// =====================================================================
	// Quà đăng nhập hằng ngày + chuỗi ngày
	// =====================================================================

	/** Gọi mỗi giây cho từng người chơi; chỉ phát quà một lần mỗi ngày. */
	public static void login(ServerPlayerEntity p) {
		if (p.age < 100) return; // đợi vào thế giới xong hẳn
		Store.PData d = Store.get(p);
		LocalDate today = LocalDate.now();
		if (today.toString().equals(d.lastLogin)) return;
		d.streak = today.minusDays(1).toString().equals(d.lastLogin) ? d.streak + 1 : 1;
		d.lastLogin = today.toString();
		int got = Store.addPoints(p, 10 + 5 * Math.min(d.streak, 7), null);
		boolean big = d.streak % 7 == 0;
		String gift;
		if (big) {
			p.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 3));
			p.getInventory().offerOrDrop(new ItemStack(Items.GOLDEN_APPLE, 2));
			Store.grant(p, "streak_7");
			gift = "3 kim cương, 2 táo vàng";
		} else {
			int beef = Math.min(16, 4 + 2 * d.streak);
			p.getInventory().offerOrDrop(new ItemStack(Items.COOKED_BEEF, beef));
			gift = beef + " bò nướng";
		}
		Fx.title(p, big ? "CHUỖI " + d.streak + " NGÀY!" : "QUÀ ĐĂNG NHẬP",
			"Chuỗi " + d.streak + " ngày  |  +" + got + " điểm", big ? Formatting.LIGHT_PURPLE : Formatting.GOLD);
		Fx.loginReward(p, big);
		Store.say(p, "Quà đăng nhập (chuỗi " + d.streak + " ngày): +" + got + " điểm, " + gift, Formatting.GOLD);
	}
}
