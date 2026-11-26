package com.nguyenquochuy.survivalarcade;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.object.AtlasTextObjectContents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Sổ tay có chữ bấm được để chạy lệnh /mg và các trang công thức chế tạo vẽ bằng biểu tượng vật phẩm. */
public final class ArcadeBook {
	/** Đổi số cuối khi sửa nội dung sách: sách bản cũ sẽ tự bị thu hồi và cấp lại bản mới. */
	public static final String TITLE = "Sổ tay Arcade 3";
	private static final List<String> LEGACY_TITLES = List.of("Sổ tay Arcade", "Sổ tay Arcade 2");

	private ArcadeBook() {}

	private static MutableText link(String label, String cmd, Formatting color) {
		return Text.literal(label + "\n")
			.setStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent.RunCommand(cmd)));
	}

	private static Text page1() {
		MutableText t = Text.literal("SỔ TAY ARCADE\n").formatted(Formatting.DARK_BLUE, Formatting.BOLD);
		t.append(Text.literal("Bấm vào chữ để chạy\n\n").formatted(Formatting.GRAY));
		t.append(link("▶ Trạng thái", "mg", Formatting.DARK_GREEN));
		t.append(link("▶ Thử thách ngày", "mg daily", Formatting.DARK_GREEN));
		t.append(link("▶ Nhiệm vụ tuần", "mg weekly", Formatting.DARK_GREEN));
		t.append(link("▶ Đấu trường", "mg arena", Formatting.DARK_RED));
		t.append(link("▶ Sống sót 5 phút", "mg survive 5", Formatting.DARK_RED));
		t.append(link("▶ Rush Mode", "mg rush", Formatting.GOLD));
		t.append(link("▶ Rèn đồ", "mg forge", Formatting.GOLD));
		t.append(link("▶ Giao hàng", "mg delivery", Formatting.DARK_AQUA));
		t.append(link("■ Dừng minigame", "mg stop", Formatting.RED));
		return t;
	}

	private static Text page2() {
		MutableText t = Text.literal("KỸ NĂNG\n").formatted(Formatting.DARK_BLUE, Formatting.BOLD);
		t.append(Text.literal("Hoặc cầm vật phẩm kỹ năng trên thanh nhanh, bấm chuột phải\n").formatted(Formatting.GRAY));
		t.append(link("▶ Lướt nhanh (cấp 1)", "mg skill dash", Formatting.DARK_AQUA));
		t.append(link("▶ Hồi máu (cấp 3)", "mg skill heal", Formatting.DARK_GREEN));
		t.append(link("▶ Sóng xung kích (cấp 5)", "mg skill shock", Formatting.DARK_RED));
		t.append(link("▶ Cây kỹ năng", "mg tree", Formatting.DARK_PURPLE));
		t.append(link("  ⬆ Nâng Lướt", "mg tree dash", Formatting.DARK_AQUA));
		t.append(link("  ⬆ Nâng Hồi máu", "mg tree heal", Formatting.DARK_GREEN));
		t.append(link("  ⬆ Nâng Sóng xung kích", "mg tree shock", Formatting.DARK_RED));
		t.append(Text.literal("\nBoss và Bounty xuất hiện bất ngờ!").formatted(Formatting.DARK_PURPLE));
		return t;
	}

	private static Text page3() {
		MutableText t = Text.literal("KHÁC\n\n").formatted(Formatting.DARK_BLUE, Formatting.BOLD);
		t.append(link("▶ Cửa hàng điểm", "mg shop", Formatting.DARK_PURPLE));
		t.append(link("▶ Lò nâng cấp vũ khí", "mg upgrade", Formatting.GOLD));
		t.append(link("▶ Bảng xếp hạng", "mg top", Formatting.DARK_PURPLE));
		t.append(link("▶ Thành tựu", "mg ach", Formatting.DARK_PURPLE));
		t.append(link("▶ Danh sách lệnh", "mg help", Formatting.DARK_GRAY));
		t.append(Text.literal("\nLò nâng cấp: cầm vũ khí độc quyền trên tay, tốn điểm + kim cương, tối đa +5.\n").formatted(Formatting.GRAY));
		t.append(Text.literal("Các trang sau là công thức chế tạo.").formatted(Formatting.GRAY));
		return t;
	}

	// ---------- trang công thức ----------

	private static Text icon(RecipeData.Cell c) {
		return Text.object(new AtlasTextObjectContents(Identifier.of(c.atlas()), Identifier.of(c.sprite())));
	}

	private static Text recipePage(RecipeData.R r) {
		MutableText t = Text.literal(r.title() + "\n").formatted(r.color(), Formatting.BOLD);
		for (String row : r.pattern()) {
			for (int i = 0; i < 3; i++) {
				char ch = i < row.length() ? row.charAt(i) : ' ';
				if (ch == ' ') t.append(Text.literal("  "));
				else t.append(icon(r.key().get(ch)));
				if (i < 2) t.append(Text.literal(" "));
			}
			t.append(Text.literal("\n"));
		}
		t.append(Text.literal("→ ").formatted(Formatting.DARK_GRAY));
		t.append(icon(r.result()));
		t.append(Text.literal(" " + r.result().label() + "\n\n").formatted(Formatting.BLACK));
		for (Map.Entry<Character, RecipeData.Cell> e : r.key().entrySet()) {
			t.append(icon(e.getValue()));
			t.append(Text.literal(" " + e.getValue().label() + "\n").formatted(Formatting.DARK_GRAY));
		}
		t.append(Text.literal("\n" + r.desc()).formatted(Formatting.DARK_AQUA));
		return t;
	}

	public static ItemStack create() {
		List<RawFilteredPair<Text>> pages = new ArrayList<>();
		pages.add(RawFilteredPair.of(page1()));
		pages.add(RawFilteredPair.of(page2()));
		pages.add(RawFilteredPair.of(page3()));
		for (RecipeData.R r : RecipeData.ALL) pages.add(RawFilteredPair.of(recipePage(r)));
		ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
		s.set(DataComponentTypes.WRITTEN_BOOK_CONTENT,
			new WrittenBookContentComponent(RawFilteredPair.of(TITLE), "Survival Arcade", 0, pages, true));
		return s;
	}

	private static boolean titleIs(ItemStack s, String title) {
		if (!s.isOf(Items.WRITTEN_BOOK)) return false;
		WrittenBookContentComponent c = s.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
		return c != null && title.equals(c.title().raw());
	}

	private static boolean isLegacy(ItemStack s) {
		for (String t : LEGACY_TITLES) if (titleIs(s, t)) return true;
		return false;
	}

	static boolean isBook(ItemStack s) {
		return titleIs(s, TITLE);
	}

	private static boolean isAnyVersion(ItemStack s) {
		return isBook(s) || isLegacy(s);
	}

	private static boolean has(ServerPlayerEntity p) {
		PlayerInventory inv = p.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (isBook(inv.getStack(i))) return true;
		}
		return isBook(p.currentScreenHandler.getCursorStack());
	}

	/** Gỡ sách khỏi túi đồ trước khi chết để không rơi ra đất (sẽ được cấp lại khi hồi sinh). */
	public static void strip(ServerPlayerEntity p) {
		PlayerInventory inv = p.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (isAnyVersion(inv.getStack(i))) inv.setStack(i, ItemStack.EMPTY);
		}
	}

	/** Gọi định kỳ: thu hồi sách bản cũ, dọn sách bị vứt xuống đất, và cấp sách nếu đang không có. */
	public static void ensure(ServerPlayerEntity p) {
		if (!p.isAlive()) return;
		PlayerInventory inv = p.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (isLegacy(inv.getStack(i))) inv.setStack(i, ItemStack.EMPTY);
		}
		ServerWorld w = p.getEntityWorld();
		Box box = new Box(p.getX() - 8, p.getY() - 8, p.getZ() - 8, p.getX() + 8, p.getY() + 8, p.getZ() + 8);
		for (ItemEntity e : w.getEntitiesByClass(ItemEntity.class, box, ie -> isAnyVersion(ie.getStack()))) {
			e.discard();
		}
		if (!has(p) && inv.getEmptySlot() >= 0) {
			inv.offerOrDrop(create());
			Store.say(p, "Bạn nhận được Sổ tay Arcade: cầm lên, bấm chuột phải, rồi bấm vào chữ để chơi.", Formatting.GOLD);
		}
	}
}
