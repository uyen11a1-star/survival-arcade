package com.nguyenquochuy.survivalarcade;

import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dữ liệu công thức (khớp với data/survivalarcade/recipe/*.json) dùng cho menu công thức. Tạo tự động. */
public final class RecipeData {
	/** item = id vật phẩm vanilla, ví dụ "minecraft:diamond". */
	public record Cell(String item, String label) {}
	public record R(String id, String title, Formatting color, String base, List<String> pattern,
					Map<Character, Cell> key, List<String> lore, String desc) {}

	private RecipeData() {}

	private static Map<Character, Cell> keys(Object... kv) {
		Map<Character, Cell> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) m.put((Character) kv[i], (Cell) kv[i + 1]);
		return m;
	}

	public static final List<R> ALL = List.of(
		new R("thunder_blade", "Kiếm Sấm Sét", Formatting.AQUA, "minecraft:netherite_sword", List.of("L L", " S ", "LDL"),
			keys('L', new Cell("minecraft:lightning_rod", "Cột thu lôi"), 'S', new Cell("minecraft:netherite_sword", "Kiếm netherite"), 'D', new Cell("minecraft:diamond_block", "Khối kim cương")),
			List.of("Chuột phải: gọi sét đánh quái đang ngắm", "Sét lan sang quái ở gần", "Không bao giờ hỏng"), "Chuột phải gọi sét, lan sang quái gần đó."),
		new R("blink_staff", "Gậy Dịch Chuyển", Formatting.GREEN, "minecraft:stick", List.of(" P ", "PEP", " R "),
			keys('P', new Cell("minecraft:ender_pearl", "Ngọc ender"), 'E', new Cell("minecraft:ender_eye", "Mắt ender"), 'R', new Cell("minecraft:blaze_rod", "Que lửa")),
			List.of("Chuột phải: dịch chuyển tức thời về phía trước"), "Chuột phải chớp tới phía trước."),
		new R("miner_pick", "Cúp Thợ Mỏ", Formatting.LIGHT_PURPLE, "minecraft:netherite_pickaxe", List.of("DRD", " P "),
			keys('D', new Cell("minecraft:diamond_block", "Khối kim cương"), 'R', new Cell("minecraft:redstone_block", "Khối đá đỏ"), 'P', new Cell("minecraft:netherite_pickaxe", "Cúp netherite")),
			List.of("Cầm trên tay: đào nhanh", "Đào quặng: phá cả mạch quặng", "(ngồi xuống để tắt)", "Không bao giờ hỏng"), "Đào nhanh, phá cả mạch quặng."),
		new R("charm", "Bùa Hộ Mệnh", Formatting.GOLD, "minecraft:heart_of_the_sea", List.of("EDE", "DTD", "EDE"),
			keys('E', new Cell("minecraft:emerald", "Ngọc lục bảo"), 'D', new Cell("minecraft:diamond", "Kim cương"), 'T', new Cell("minecraft:totem_of_undying", "Totem bất tử")),
			List.of("Để trong túi đồ", "Sắp chết: tự hồi máu + khiên", "Hồi chiêu 2 phút"), "Để trong túi, tự cứu khi sắp chết."),
		new R("hunter_compass", "La Bàn Săn Mồi", Formatting.YELLOW, "minecraft:compass", List.of(" E ", "ECE", " R "),
			keys('E', new Cell("minecraft:ender_eye", "Mắt ender"), 'C', new Cell("minecraft:compass", "La bàn"), 'R', new Cell("minecraft:redstone_block", "Khối đá đỏ")),
			List.of("Cầm lên để chỉ hướng boss và bounty"), "Cầm lên: chỉ hướng boss và bounty."),
		new R("power_wand", "Gậy Quyền Năng", Formatting.GOLD, "minecraft:blaze_rod", List.of("TNT", " R ", " R "),
			keys('T', new Cell("minecraft:totem_of_undying", "Totem bất tử"), 'N', new Cell("minecraft:nether_star", "Sao địa ngục"), 'R', new Cell("minecraft:blaze_rod", "Que lửa")),
			List.of("Vũ khí hiếm nhất, mạnh nhất", "Chuột phải: dùng kỹ năng hiện tại", "Ngồi + chuột phải: đổi kỹ năng", "Thiên Thạch, Bão Sét, Hố Đen, Hỏa Phụng, Ngưng Đọng"), "Vũ khí hiếm nhất, 5 kỹ năng cực mạnh.")
	);
}
