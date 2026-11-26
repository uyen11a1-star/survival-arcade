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

	/** Một bộ giáp: 4 món, mỗi món có công thức riêng. */
	public record S(String id, String title, Formatting color, List<R> pieces) {}

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

	public static final List<S> SETS = List.of(
		new S("thunder", "Giáp Sấm Sét", Formatting.AQUA, List.of(
			new R("thunder_helmet", "Mũ Sấm Sét", Formatting.AQUA, "minecraft:netherite_helmet", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:lightning_rod", "Cột thu lôi"), 'B', new Cell("minecraft:diamond_block", "Khối kim cương"), 'P', new Cell("minecraft:netherite_helmet", "Mũ netherite")),
				List.of("Bộ Sấm Sét", "2 món: Tốc độ I", "4 món: Tốc độ II, Đào nhanh I, sét tự đánh quái ở gần"), "Bộ Sấm Sét: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("thunder_chest", "Giáp Ngực Sấm Sét", Formatting.AQUA, "minecraft:netherite_chestplate", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:lightning_rod", "Cột thu lôi"), 'B', new Cell("minecraft:diamond_block", "Khối kim cương"), 'P', new Cell("minecraft:netherite_chestplate", "Giáp ngực netherite")),
				List.of("Bộ Sấm Sét", "2 món: Tốc độ I", "4 món: Tốc độ II, Đào nhanh I, sét tự đánh quái ở gần"), "Bộ Sấm Sét: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("thunder_legs", "Quần Sấm Sét", Formatting.AQUA, "minecraft:netherite_leggings", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:lightning_rod", "Cột thu lôi"), 'B', new Cell("minecraft:diamond_block", "Khối kim cương"), 'P', new Cell("minecraft:netherite_leggings", "Quần netherite")),
				List.of("Bộ Sấm Sét", "2 món: Tốc độ I", "4 món: Tốc độ II, Đào nhanh I, sét tự đánh quái ở gần"), "Bộ Sấm Sét: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("thunder_boots", "Giày Sấm Sét", Formatting.AQUA, "minecraft:netherite_boots", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:lightning_rod", "Cột thu lôi"), 'B', new Cell("minecraft:diamond_block", "Khối kim cương"), 'P', new Cell("minecraft:netherite_boots", "Giày netherite")),
				List.of("Bộ Sấm Sét", "2 món: Tốc độ I", "4 món: Tốc độ II, Đào nhanh I, sét tự đánh quái ở gần"), "Bộ Sấm Sét: mặc đủ 4 món để mở hiệu ứng bộ."))),
		new S("void", "Giáp Hư Không", Formatting.DARK_PURPLE, List.of(
			new R("void_helmet", "Mũ Hư Không", Formatting.DARK_PURPLE, "minecraft:netherite_helmet", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:ender_eye", "Mắt ender"), 'B', new Cell("minecraft:crying_obsidian", "Hắc diện thạch khóc"), 'P', new Cell("minecraft:netherite_helmet", "Mũ netherite")),
				List.of("Bộ Hư Không", "2 món: Nhảy cao", "4 món: 20% né đòn bằng cách chớp đi, sắp chết tự thoát thân (hồi 3 phút)"), "Bộ Hư Không: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("void_chest", "Giáp Ngực Hư Không", Formatting.DARK_PURPLE, "minecraft:netherite_chestplate", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:ender_eye", "Mắt ender"), 'B', new Cell("minecraft:crying_obsidian", "Hắc diện thạch khóc"), 'P', new Cell("minecraft:netherite_chestplate", "Giáp ngực netherite")),
				List.of("Bộ Hư Không", "2 món: Nhảy cao", "4 món: 20% né đòn bằng cách chớp đi, sắp chết tự thoát thân (hồi 3 phút)"), "Bộ Hư Không: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("void_legs", "Quần Hư Không", Formatting.DARK_PURPLE, "minecraft:netherite_leggings", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:ender_eye", "Mắt ender"), 'B', new Cell("minecraft:crying_obsidian", "Hắc diện thạch khóc"), 'P', new Cell("minecraft:netherite_leggings", "Quần netherite")),
				List.of("Bộ Hư Không", "2 món: Nhảy cao", "4 món: 20% né đòn bằng cách chớp đi, sắp chết tự thoát thân (hồi 3 phút)"), "Bộ Hư Không: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("void_boots", "Giày Hư Không", Formatting.DARK_PURPLE, "minecraft:netherite_boots", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:ender_eye", "Mắt ender"), 'B', new Cell("minecraft:crying_obsidian", "Hắc diện thạch khóc"), 'P', new Cell("minecraft:netherite_boots", "Giày netherite")),
				List.of("Bộ Hư Không", "2 món: Nhảy cao", "4 món: 20% né đòn bằng cách chớp đi, sắp chết tự thoát thân (hồi 3 phút)"), "Bộ Hư Không: mặc đủ 4 món để mở hiệu ứng bộ."))),
		new S("phoenix", "Giáp Phượng Hoàng", Formatting.GOLD, List.of(
			new R("phoenix_helmet", "Mũ Phượng Hoàng", Formatting.GOLD, "minecraft:netherite_helmet", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:blaze_rod", "Que lửa"), 'B', new Cell("minecraft:gold_block", "Khối vàng"), 'P', new Cell("minecraft:netherite_helmet", "Mũ netherite")),
				List.of("Bộ Phượng Hoàng", "2 món: Miễn nhiễm lửa", "4 món: tự hồi máu khi dưới 50%, chết một lần được hồi sinh kèm sóng lửa (hồi 5 phút)"), "Bộ Phượng Hoàng: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("phoenix_chest", "Giáp Ngực Phượng Hoàng", Formatting.GOLD, "minecraft:netherite_chestplate", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:blaze_rod", "Que lửa"), 'B', new Cell("minecraft:gold_block", "Khối vàng"), 'P', new Cell("minecraft:netherite_chestplate", "Giáp ngực netherite")),
				List.of("Bộ Phượng Hoàng", "2 món: Miễn nhiễm lửa", "4 món: tự hồi máu khi dưới 50%, chết một lần được hồi sinh kèm sóng lửa (hồi 5 phút)"), "Bộ Phượng Hoàng: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("phoenix_legs", "Quần Phượng Hoàng", Formatting.GOLD, "minecraft:netherite_leggings", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:blaze_rod", "Que lửa"), 'B', new Cell("minecraft:gold_block", "Khối vàng"), 'P', new Cell("minecraft:netherite_leggings", "Quần netherite")),
				List.of("Bộ Phượng Hoàng", "2 món: Miễn nhiễm lửa", "4 món: tự hồi máu khi dưới 50%, chết một lần được hồi sinh kèm sóng lửa (hồi 5 phút)"), "Bộ Phượng Hoàng: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("phoenix_boots", "Giày Phượng Hoàng", Formatting.GOLD, "minecraft:netherite_boots", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:blaze_rod", "Que lửa"), 'B', new Cell("minecraft:gold_block", "Khối vàng"), 'P', new Cell("minecraft:netherite_boots", "Giày netherite")),
				List.of("Bộ Phượng Hoàng", "2 món: Miễn nhiễm lửa", "4 món: tự hồi máu khi dưới 50%, chết một lần được hồi sinh kèm sóng lửa (hồi 5 phút)"), "Bộ Phượng Hoàng: mặc đủ 4 món để mở hiệu ứng bộ."))),
		new S("guardian", "Giáp Hộ Vệ", Formatting.WHITE, List.of(
			new R("guardian_helmet", "Mũ Hộ Vệ", Formatting.WHITE, "minecraft:netherite_helmet", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:iron_block", "Khối sắt"), 'B', new Cell("minecraft:shield", "Khiên"), 'P', new Cell("minecraft:netherite_helmet", "Mũ netherite")),
				List.of("Bộ Hộ Vệ", "2 món: Giảm sát thương I", "4 món: Giảm sát thương II, khiên vàng tự hồi mỗi 20 giây"), "Bộ Hộ Vệ: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("guardian_chest", "Giáp Ngực Hộ Vệ", Formatting.WHITE, "minecraft:netherite_chestplate", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:iron_block", "Khối sắt"), 'B', new Cell("minecraft:shield", "Khiên"), 'P', new Cell("minecraft:netherite_chestplate", "Giáp ngực netherite")),
				List.of("Bộ Hộ Vệ", "2 món: Giảm sát thương I", "4 món: Giảm sát thương II, khiên vàng tự hồi mỗi 20 giây"), "Bộ Hộ Vệ: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("guardian_legs", "Quần Hộ Vệ", Formatting.WHITE, "minecraft:netherite_leggings", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:iron_block", "Khối sắt"), 'B', new Cell("minecraft:shield", "Khiên"), 'P', new Cell("minecraft:netherite_leggings", "Quần netherite")),
				List.of("Bộ Hộ Vệ", "2 món: Giảm sát thương I", "4 món: Giảm sát thương II, khiên vàng tự hồi mỗi 20 giây"), "Bộ Hộ Vệ: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("guardian_boots", "Giày Hộ Vệ", Formatting.WHITE, "minecraft:netherite_boots", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:iron_block", "Khối sắt"), 'B', new Cell("minecraft:shield", "Khiên"), 'P', new Cell("minecraft:netherite_boots", "Giày netherite")),
				List.of("Bộ Hộ Vệ", "2 món: Giảm sát thương I", "4 món: Giảm sát thương II, khiên vàng tự hồi mỗi 20 giây"), "Bộ Hộ Vệ: mặc đủ 4 món để mở hiệu ứng bộ."))),
		new S("shadow", "Giáp Bóng Đêm", Formatting.DARK_GRAY, List.of(
			new R("shadow_helmet", "Mũ Bóng Đêm", Formatting.DARK_GRAY, "minecraft:netherite_helmet", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:obsidian", "Hắc diện thạch"), 'B', new Cell("minecraft:phantom_membrane", "Màng phantom"), 'P', new Cell("minecraft:netherite_helmet", "Mũ netherite")),
				List.of("Bộ Bóng Đêm", "2 món: Nhìn đêm", "4 món: ngồi xuống để tàng hình, ban đêm chạy nhanh"), "Bộ Bóng Đêm: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("shadow_chest", "Giáp Ngực Bóng Đêm", Formatting.DARK_GRAY, "minecraft:netherite_chestplate", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:obsidian", "Hắc diện thạch"), 'B', new Cell("minecraft:phantom_membrane", "Màng phantom"), 'P', new Cell("minecraft:netherite_chestplate", "Giáp ngực netherite")),
				List.of("Bộ Bóng Đêm", "2 món: Nhìn đêm", "4 món: ngồi xuống để tàng hình, ban đêm chạy nhanh"), "Bộ Bóng Đêm: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("shadow_legs", "Quần Bóng Đêm", Formatting.DARK_GRAY, "minecraft:netherite_leggings", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:obsidian", "Hắc diện thạch"), 'B', new Cell("minecraft:phantom_membrane", "Màng phantom"), 'P', new Cell("minecraft:netherite_leggings", "Quần netherite")),
				List.of("Bộ Bóng Đêm", "2 món: Nhìn đêm", "4 món: ngồi xuống để tàng hình, ban đêm chạy nhanh"), "Bộ Bóng Đêm: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("shadow_boots", "Giày Bóng Đêm", Formatting.DARK_GRAY, "minecraft:netherite_boots", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:obsidian", "Hắc diện thạch"), 'B', new Cell("minecraft:phantom_membrane", "Màng phantom"), 'P', new Cell("minecraft:netherite_boots", "Giày netherite")),
				List.of("Bộ Bóng Đêm", "2 món: Nhìn đêm", "4 món: ngồi xuống để tàng hình, ban đêm chạy nhanh"), "Bộ Bóng Đêm: mặc đủ 4 món để mở hiệu ứng bộ."))),
		new S("power", "Giáp Quyền Năng", Formatting.YELLOW, List.of(
			new R("power_helmet", "Mũ Quyền Năng", Formatting.YELLOW, "minecraft:netherite_helmet", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:netherite_ingot", "Thỏi netherite"), 'B', new Cell("minecraft:nether_star", "Sao địa ngục"), 'P', new Cell("minecraft:netherite_helmet", "Mũ netherite")),
				List.of("Bộ Quyền Năng", "2 món: Giảm sát thương I, giảm 15% hồi chiêu", "4 món: Sức mạnh I, tự hồi máu, giảm 30% hồi chiêu, +25% sát thương chiêu, vòng hào quang trên đầu"), "Bộ Quyền Năng: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("power_chest", "Giáp Ngực Quyền Năng", Formatting.YELLOW, "minecraft:netherite_chestplate", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:netherite_ingot", "Thỏi netherite"), 'B', new Cell("minecraft:nether_star", "Sao địa ngục"), 'P', new Cell("minecraft:netherite_chestplate", "Giáp ngực netherite")),
				List.of("Bộ Quyền Năng", "2 món: Giảm sát thương I, giảm 15% hồi chiêu", "4 món: Sức mạnh I, tự hồi máu, giảm 30% hồi chiêu, +25% sát thương chiêu, vòng hào quang trên đầu"), "Bộ Quyền Năng: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("power_legs", "Quần Quyền Năng", Formatting.YELLOW, "minecraft:netherite_leggings", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:netherite_ingot", "Thỏi netherite"), 'B', new Cell("minecraft:nether_star", "Sao địa ngục"), 'P', new Cell("minecraft:netherite_leggings", "Quần netherite")),
				List.of("Bộ Quyền Năng", "2 món: Giảm sát thương I, giảm 15% hồi chiêu", "4 món: Sức mạnh I, tự hồi máu, giảm 30% hồi chiêu, +25% sát thương chiêu, vòng hào quang trên đầu"), "Bộ Quyền Năng: mặc đủ 4 món để mở hiệu ứng bộ."),
			new R("power_boots", "Giày Quyền Năng", Formatting.YELLOW, "minecraft:netherite_boots", List.of("ABA", " P "),
				keys('A', new Cell("minecraft:netherite_ingot", "Thỏi netherite"), 'B', new Cell("minecraft:nether_star", "Sao địa ngục"), 'P', new Cell("minecraft:netherite_boots", "Giày netherite")),
				List.of("Bộ Quyền Năng", "2 món: Giảm sát thương I, giảm 15% hồi chiêu", "4 món: Sức mạnh I, tự hồi máu, giảm 30% hồi chiêu, +25% sát thương chiêu, vòng hào quang trên đầu"), "Bộ Quyền Năng: mặc đủ 4 món để mở hiệu ứng bộ.")))
	);
}
