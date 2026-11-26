package com.nguyenquochuy.survivalarcade;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Menu rương nhiều tầng: danh sách vũ khí và bộ giáp độc quyền, bấm một bộ để xem 4 món,
 * bấm một món để xem công thức chế tạo. Mọi thao tác nhặt / đặt đồ đều bị chặn.
 */
public final class RecipeMenu extends GenericContainerScreenHandler {
	private static final int WEAPON_SLOT = 10;
	private static final int SET_SLOT = 19;
	private static final int[] PIECE_SLOTS = {10, 12, 14, 16};
	private static final int SET_BACK_SLOT = 22;
	private static final int[][] GRID = {{11, 12, 13}, {20, 21, 22}, {29, 30, 31}};
	private static final int ARROW_SLOT = 23;
	private static final int RESULT_SLOT = 24;
	private static final int BACK_SLOT = 40;

	private final SimpleInventory inv;
	private final IntConsumer onClick;

	private RecipeMenu(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInv, SimpleInventory inv, int rows, IntConsumer onClick) {
		super(type, syncId, playerInv, inv, rows);
		this.inv = inv;
		this.onClick = onClick;
	}

	private static void open(ServerPlayerEntity p, Text title, ScreenHandlerType<?> type, int rows, SimpleInventory inv, IntConsumer onClick) {
		p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
			(syncId, playerInv, player) -> new RecipeMenu(type, syncId, playerInv, inv, rows, onClick), title));
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (actionType == SlotActionType.PICKUP && slotIndex >= 0 && slotIndex < inv.size()) {
			onClick.accept(slotIndex);
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	// ---------- tiện ích ----------

	private static Text t(String s, Formatting f) {
		return Text.literal(s).formatted(f).styled(st -> st.withItalic(false));
	}

	private static ItemStack pane(Item item) {
		ItemStack s = new ItemStack(item);
		s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
		return s;
	}

	private static ItemStack named(Item item, String name, Formatting color, String lore) {
		ItemStack s = new ItemStack(item);
		s.set(DataComponentTypes.CUSTOM_NAME, t(name, color));
		if (lore != null) s.set(DataComponentTypes.LORE, new LoreComponent(List.of(t(lore, Formatting.GRAY))));
		return s;
	}

	private static void fillPanes(SimpleInventory inv) {
		for (int i = 0; i < inv.size(); i++) inv.setStack(i, pane(Items.GRAY_STAINED_GLASS_PANE));
	}

	/** Chạy sau 1 tick để tránh đổi màn hình ngay trong lúc đang xử lý cú bấm. */
	private static void later(ServerPlayerEntity p, Consumer<ServerPlayerEntity> next) {
		UUID id = p.getUuid();
		Fx.later(1, () -> {
			ServerPlayerEntity q = Fx.player(id);
			if (q != null) next.accept(q);
		});
	}

	// ---------- tầng 1: vũ khí + các bộ giáp ----------

	private static ItemStack setIcon(RecipeData.S s) {
		ItemStack st = ArcadeItems.display(s.pieces().get(0), false);
		st.set(DataComponentTypes.CUSTOM_NAME, Text.literal(s.title()).formatted(s.color(), Formatting.BOLD).styled(x -> x.withItalic(false)));
		st.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			t("Bộ giáp 4 món", Formatting.GRAY), t("▶ Bấm để xem từng món", Formatting.YELLOW))));
		return st;
	}

	public static void openCatalog(ServerPlayerEntity p) {
		SimpleInventory inv = new SimpleInventory(36);
		fillPanes(inv);
		inv.setStack(4, named(Items.BOOK, "Vật phẩm độc quyền", Formatting.GOLD, "Bấm vào một món để xem công thức chế tạo"));
		for (int i = 0; i < RecipeData.ALL.size(); i++) {
			inv.setStack(WEAPON_SLOT + i, ArcadeItems.display(RecipeData.ALL.get(i), true));
		}
		for (int i = 0; i < RecipeData.SETS.size(); i++) {
			inv.setStack(SET_SLOT + i, setIcon(RecipeData.SETS.get(i)));
		}
		open(p, Text.literal("Vật phẩm độc quyền"), ScreenHandlerType.GENERIC_9X4, 4, inv, slot -> {
			int w = slot - WEAPON_SLOT;
			int s = slot - SET_SLOT;
			if (w >= 0 && w < RecipeData.ALL.size()) {
				RecipeData.R r = RecipeData.ALL.get(w);
				later(p, q -> openRecipe(q, r, RecipeMenu::openCatalog));
			} else if (s >= 0 && s < RecipeData.SETS.size()) {
				RecipeData.S set = RecipeData.SETS.get(s);
				later(p, q -> openSet(q, set));
			}
		});
	}

	// ---------- tầng 2 (giáp): 4 món của một bộ ----------

	private static void openSet(ServerPlayerEntity p, RecipeData.S s) {
		SimpleInventory inv = new SimpleInventory(27);
		fillPanes(inv);
		inv.setStack(4, named(Items.BOOK, s.title(), s.color(), "Bấm vào một món để xem công thức"));
		for (int i = 0; i < s.pieces().size(); i++) {
			inv.setStack(PIECE_SLOTS[i], ArcadeItems.display(s.pieces().get(i), true));
		}
		inv.setStack(SET_BACK_SLOT, named(Items.ARROW, "◀ Quay lại", Formatting.AQUA, null));
		open(p, Text.literal(s.title()), ScreenHandlerType.GENERIC_9X3, 3, inv, slot -> {
			if (slot == SET_BACK_SLOT) {
				later(p, RecipeMenu::openCatalog);
				return;
			}
			for (int i = 0; i < PIECE_SLOTS.length; i++) {
				if (slot == PIECE_SLOTS[i] && i < s.pieces().size()) {
					RecipeData.R r = s.pieces().get(i);
					later(p, q -> openRecipe(q, r, qq -> openSet(qq, s)));
					return;
				}
			}
		});
	}

	// ---------- tầng cuối: công thức của một món ----------

	private static void openRecipe(ServerPlayerEntity p, RecipeData.R r, Consumer<ServerPlayerEntity> back) {
		SimpleInventory inv = new SimpleInventory(45);
		fillPanes(inv);
		for (int[] row : GRID) {
			for (int slot : row) inv.setStack(slot, pane(Items.LIGHT_GRAY_STAINED_GLASS_PANE));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if (row >= r.pattern().size() || col >= r.pattern().get(row).length()) continue;
				char ch = r.pattern().get(row).charAt(col);
				if (ch == ' ') continue;
				RecipeData.Cell c = r.key().get(ch);
				ItemStack ing = new ItemStack(ArcadeItems.vanilla(c.item()));
				ing.set(DataComponentTypes.CUSTOM_NAME, t(c.label(), Formatting.WHITE));
				ing.set(DataComponentTypes.LORE, new LoreComponent(List.of(t("Nguyên liệu", Formatting.GRAY))));
				inv.setStack(GRID[row][col], ing);
			}
		}
		inv.setStack(ARROW_SLOT, named(Items.ARROW, "Chế tạo trong bàn chế tạo thường", Formatting.YELLOW, "Xếp đúng hình bên trái"));
		inv.setStack(RESULT_SLOT, ArcadeItems.display(r, false));
		inv.setStack(BACK_SLOT, named(Items.ARROW, "◀ Quay lại", Formatting.AQUA, null));
		open(p, Text.literal("Công thức: " + r.title()), ScreenHandlerType.GENERIC_9X5, 5, inv, slot -> {
			if (slot == BACK_SLOT) later(p, back);
		});
	}
}
