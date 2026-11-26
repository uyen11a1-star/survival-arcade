package com.nguyenquochuy.survivalarcade;

import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dữ liệu công thức (khớp với data/survivalarcade/recipe/*.json) để vẽ trong sổ tay. Tạo tự động. */
public final class RecipeData {
	public record Cell(String atlas, String sprite, String label) {}
	public record R(String id, String title, Formatting color, List<String> pattern, Map<Character, Cell> key, Cell result, String desc) {}

	private RecipeData() {}

	private static Map<Character, Cell> keys(Object... kv) {
		Map<Character, Cell> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) m.put((Character) kv[i], (Cell) kv[i + 1]);
		return m;
	}

	public static final List<R> ALL = List.of(
		new R("thunder_blade", "Kiếm Sấm Sét", Formatting.AQUA, List.of("L L", " S ", "LDL"),
			keys('L', new Cell("minecraft:blocks", "minecraft:block/lightning_rod", "Cột thu lôi"), 'S', new Cell("minecraft:items", "minecraft:item/netherite_sword", "Kiếm netherite"), 'D', new Cell("minecraft:blocks", "minecraft:block/diamond_block", "Khối kim cương")),
			new Cell("minecraft:items", "survivalarcade:item/thunder_blade", "Kiếm Sấm Sét"), "Chuột phải gọi sét, lan sang quái gần đó."),
		new R("blink_staff", "Gậy Dịch Chuyển", Formatting.GREEN, List.of(" P ", "PEP", " R "),
			keys('P', new Cell("minecraft:items", "minecraft:item/ender_pearl", "Ngọc ender"), 'E', new Cell("minecraft:items", "minecraft:item/ender_eye", "Mắt ender"), 'R', new Cell("minecraft:items", "minecraft:item/blaze_rod", "Que lửa")),
			new Cell("minecraft:items", "survivalarcade:item/blink_staff", "Gậy Dịch Chuyển"), "Chuột phải chớp tới phía trước."),
		new R("miner_pick", "Cúp Thợ Mỏ", Formatting.LIGHT_PURPLE, List.of("DRD", " P "),
			keys('D', new Cell("minecraft:blocks", "minecraft:block/diamond_block", "Khối kim cương"), 'R', new Cell("minecraft:blocks", "minecraft:block/redstone_block", "Khối đá đỏ"), 'P', new Cell("minecraft:items", "minecraft:item/netherite_pickaxe", "Cúp netherite")),
			new Cell("minecraft:items", "survivalarcade:item/miner_pick", "Cúp Thợ Mỏ"), "Đào nhanh, phá cả mạch quặng."),
		new R("charm", "Bùa Hộ Mệnh", Formatting.GOLD, List.of("EDE", "DTD", "EDE"),
			keys('E', new Cell("minecraft:items", "minecraft:item/emerald", "Ngọc lục bảo"), 'D', new Cell("minecraft:items", "minecraft:item/diamond", "Kim cương"), 'T', new Cell("minecraft:items", "minecraft:item/totem_of_undying", "Totem bất tử")),
			new Cell("minecraft:items", "minecraft:item/heart_of_the_sea", "Bùa Hộ Mệnh"), "Để trong túi, tự cứu khi sắp chết."),
		new R("hunter_compass", "La Bàn Săn Mồi", Formatting.YELLOW, List.of(" E ", "ECE", " R "),
			keys('E', new Cell("minecraft:items", "minecraft:item/ender_eye", "Mắt ender"), 'C', new Cell("minecraft:items", "minecraft:item/compass_16", "La bàn"), 'R', new Cell("minecraft:blocks", "minecraft:block/redstone_block", "Khối đá đỏ")),
			new Cell("minecraft:items", "minecraft:item/compass_16", "La Bàn Săn Mồi"), "Cầm lên: chỉ hướng boss và bounty."),
		new R("power_wand", "Gậy Quyền Năng", Formatting.GOLD, List.of("TNT", " R ", " R "),
			keys('T', new Cell("minecraft:items", "minecraft:item/totem_of_undying", "Totem bất tử"), 'N', new Cell("minecraft:items", "minecraft:item/nether_star", "Sao địa ngục"), 'R', new Cell("minecraft:items", "minecraft:item/blaze_rod", "Que lửa")),
			new Cell("minecraft:items", "survivalarcade:item/power_wand", "Gậy Quyền Năng"), "Vũ khí hiếm nhất, 5 kỹ năng cực mạnh.")
	);
}
