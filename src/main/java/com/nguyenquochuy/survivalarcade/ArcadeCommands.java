package com.nguyenquochuy.survivalarcade;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

public final class ArcadeCommands {
	private ArcadeCommands() {}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("mg")
			.executes(c -> status(c.getSource()))
			.then(CommandManager.literal("help").executes(c -> help(c.getSource())))
			.then(CommandManager.literal("daily").executes(c -> daily(c.getSource())))
			.then(CommandManager.literal("arena").executes(c -> start(c.getSource(), "arena", 0)))
			.then(CommandManager.literal("survive")
				.executes(c -> start(c.getSource(), "survive", 5))
				.then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 10))
					.executes(c -> start(c.getSource(), "survive", IntegerArgumentType.getInteger(c, "minutes")))))
			.then(CommandManager.literal("rush").executes(c -> start(c.getSource(), "rush", 0)))
			.then(CommandManager.literal("forge").executes(c -> start(c.getSource(), "forge", 0)))
			.then(CommandManager.literal("delivery").executes(c -> start(c.getSource(), "delivery", 0)))
			.then(CommandManager.literal("stop").executes(c -> stop(c.getSource())))
			.then(CommandManager.literal("shop").executes(c -> shop(c.getSource())))
			.then(CommandManager.literal("buy")
				.then(CommandManager.argument("item", StringArgumentType.word())
					.executes(c -> buy(c.getSource(), StringArgumentType.getString(c, "item")))))
			.then(CommandManager.literal("weekly").executes(c -> weekly(c.getSource())))
			.then(CommandManager.literal("skill")
				.then(CommandManager.literal("dash").executes(c -> skill(c.getSource(), "dash")))
				.then(CommandManager.literal("heal").executes(c -> skill(c.getSource(), "heal")))
				.then(CommandManager.literal("shock").executes(c -> skill(c.getSource(), "shock"))))
			.then(CommandManager.literal("book").executes(c -> book(c.getSource())))
			.then(CommandManager.literal("top").executes(c -> top(c.getSource())))
			.then(CommandManager.literal("ach").executes(c -> ach(c.getSource()))));
	}

	private static int status(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		Store.PData d = Store.get(p);
		int lv = Store.level(d.xp);
		String next = lv >= 30 ? "cấp tối đa" : d.xp + "/" + Store.xpFor(lv + 1) + " XP";
		Store.Weekly w = Store.weekly();
		String cur = Games.current(p);
		Store.say(p, "=== Survival Arcade ===", Formatting.GOLD);
		Store.say(p, "Cấp " + lv + " (" + next + ")  |  Điểm: " + d.points, Formatting.WHITE);
		Store.say(p, "Sự kiện tuần: " + w.name() + " — điểm từ minigame " + w.catLabel() + " nhân đôi!", Formatting.LIGHT_PURPLE);
		Store.say(p, "Thử thách ngày: " + Quests.describe(p), Formatting.YELLOW);
		Store.say(p, "Nhiệm vụ tuần: " + Quests.describeWeekly(p), Formatting.YELLOW);
		if (d.streak > 0) Store.say(p, "Chuỗi đăng nhập: " + d.streak + " ngày", Formatting.AQUA);
		if (cur != null) Store.say(p, "Đang chơi: " + cur, Formatting.GREEN);
		Store.say(p, "Gõ /mg help để xem toàn bộ lệnh.", Formatting.GRAY);
		return 1;
	}

	private static int help(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		String[] lines = {
			"/mg — trạng thái, cấp, điểm, sự kiện tuần",
			"/mg daily — thử thách ngày",
			"/mg weekly — nhiệm vụ tuần",
			"/mg skill dash|heal|shock — kỹ năng chủ động",
			"/mg arena — đấu trường (các đợt quái tăng dần)",
			"/mg survive [phút] — sống sót 1–10 phút",
			"/mg rush — gom tài nguyên trong 5 phút",
			"/mg forge — chế đúng món trong thời gian giới hạn",
			"/mg delivery — giao hàng tới tọa độ xa",
			"/mg stop — dừng minigame đang chơi",
			"/mg shop — mở menu cửa hàng điểm (hoặc /mg buy <món>)",
			"/mg top — bảng xếp hạng",
			"/mg book — lấy lại Sổ tay Arcade (bấm chữ để chơi)",
			"/mg ach — thành tựu"
		};
		for (String l : lines) Store.say(p, l, Formatting.AQUA);
		return 1;
	}

	private static int daily(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		Store.say(p, "Thử thách ngày: " + Quests.describe(p), Formatting.YELLOW);
		return 1;
	}

	private static int start(ServerCommandSource src, String kind, int arg) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		String err = Games.begin(p, kind, arg);
		if (err != null) {
			src.sendError(Text.literal(err));
			return 0;
		}
		return 1;
	}

	private static int stop(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		if (!Games.stop(p)) {
			src.sendError(Text.literal("Bạn không đang chơi minigame nào."));
			return 0;
		}
		return 1;
	}

	private static int shop(ServerCommandSource src) throws CommandSyntaxException {
		ShopMenu.open(src.getPlayerOrThrow());
		return 1;
	}

	private static int top(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		java.util.List<String> rows = Store.top(10);
		Store.say(p, "=== Bảng xếp hạng ===", Formatting.GOLD);
		if (rows.isEmpty()) Store.say(p, "Chưa có ai trên bảng.", Formatting.GRAY);
		for (String r : rows) Store.say(p, r, Formatting.WHITE);
		return 1;
	}

	private static int buy(ServerCommandSource src, String item) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		Store.say(p, Shop.buy(p, item), Formatting.YELLOW);
		return 1;
	}

	private static int weekly(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		Store.say(p, "Nhiệm vụ tuần: " + Quests.describeWeekly(p), Formatting.YELLOW);
		return 1;
	}

	private static int skill(ServerCommandSource src, String id) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		String err = Skills.use(p, id);
		if (err != null) {
			src.sendError(Text.literal(err));
			return 0;
		}
		return 1;
	}

	private static int book(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		ArcadeBook.ensure(p);
		Store.say(p, "Sổ tay Arcade đã ở trong túi đồ — cầm lên và bấm chuột phải để mở.", Formatting.AQUA);
		return 1;
	}

	private static int ach(ServerCommandSource src) throws CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		Store.PData d = Store.get(p);
		Store.say(p, "=== Thành tựu (" + d.ach.size() + "/" + Store.ACH.size() + ") ===", Formatting.GOLD);
		for (Map.Entry<String, String> e : Store.ACH.entrySet()) {
			boolean has = d.ach.contains(e.getKey());
			Store.say(p, (has ? "✔ " : "✘ ") + e.getValue(), has ? Formatting.GREEN : Formatting.GRAY);
		}
		return 1;
	}
}
