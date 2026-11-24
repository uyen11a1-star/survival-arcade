package com.nguyenquochuy.survivalarcade;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Supplier;

public final class Shop {
	/** stack = null nghĩa là phần thưởng đặc biệt (id "xp"). */
	public record Good(String id, String label, int cost, int minLevel, Supplier<ItemStack> stack) {}

	public static final List<Good> ALL = List.of(
		new Good("food", "16 bò nướng", 15, 1, () -> new ItemStack(Items.COOKED_BEEF, 16)),
		new Good("arrows", "32 mũi tên", 15, 1, () -> new ItemStack(Items.ARROW, 32)),
		new Good("iron", "6 thỏi sắt", 25, 1, () -> new ItemStack(Items.IRON_INGOT, 6)),
		new Good("xp", "300 điểm kinh nghiệm", 25, 1, null),
		new Good("gapple", "1 táo vàng", 40, 2, () -> new ItemStack(Items.GOLDEN_APPLE, 1)),
		new Good("diamond", "1 kim cương", 60, 3, () -> new ItemStack(Items.DIAMOND, 1)),
		new Good("ender", "4 ngọc ender", 80, 5, () -> new ItemStack(Items.ENDER_PEARL, 4)),
		new Good("totem", "1 totem bất tử", 200, 8, () -> new ItemStack(Items.TOTEM_OF_UNDYING, 1))
	);

	private Shop() {}

	/** Trả về thông báo kết quả (đã gồm lỗi nếu có). */
	public static String buy(ServerPlayerEntity p, String id) {
		Good g = null;
		for (Good x : ALL) if (x.id().equalsIgnoreCase(id)) g = x;
		if (g == null) return "Không có món '" + id + "'. Gõ /mg shop để xem danh sách.";
		Store.PData d = Store.get(p);
		int lv = Store.level(d.xp);
		if (lv < g.minLevel()) return "Cần cấp " + g.minLevel() + " mới mua được " + g.label() + " (bạn đang cấp " + lv + ").";
		if (d.points < g.cost()) return "Không đủ điểm: cần " + g.cost() + ", bạn có " + d.points + ".";
		d.points -= g.cost();
		if (g.stack() == null) p.addExperience(300);
		else p.getInventory().offerOrDrop(g.stack().get());
		return "Đã mua " + g.label() + " (-" + g.cost() + " điểm). Còn " + d.points + " điểm.";
	}
}
