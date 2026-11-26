package com.nguyenquochuy.survivalarcade;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
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
import java.util.function.IntConsumer;

/**
 * Menu rương 2 tầng: danh sách vật phẩm độc quyền, bấm vào một món để mở rương hiện công thức chế tạo của nó.
 * Mọi thao tác nhặt / đặt đồ đều bị chặn, chỉ có thể bấm.
 */
public final class RecipeMenu extends GenericContainerScreenHandler {
	private static final int FIRST_ITEM_SLOT = 10;
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

	private static ItemStack pane(net.minecraft.item.Item item) {
		ItemStack s = new ItemStack(item);
		s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
		return s;
	}

	private static ItemStack named(net.minecraft.item.Item item, String name, Formatting color, String lore) {
		ItemStack s = new ItemStack(item);
		s.set(DataComponentTypes.CUSTOM_NAME, t(name, color));
		if (lore != null) s.set(DataComponentTypes.LORE, new LoreComponent(List.of(t(lore, Formatting.GRAY))));
		return s;
	}

	/** Chạy sau 1 tick để tránh đổi màn hình ngay trong lúc đang xử lý cú bấm. */
	private static void later(ServerPlayerEntity p, java.util.function.Consumer<ServerPlayerEntity> next) {
		UUID id = p.getUuid();
		Fx.later(1, () -> {
			ServerPlayerEntity q = Fx.player(id);
			if (q != null) next.accept(q);
		});
	}

	// ---------- tầng 1: danh sách vật phẩm ----------

	public static void openCatalog(ServerPlayerEntity p) {
		SimpleInventory inv = new SimpleInventory(27);
		for (int i = 0; i < 27; i++) inv.setStack(i, pane(Items.GRAY_STAINED_GLASS_PANE));
		inv.setStack(4, named(Items.BOOK, "Vật phẩm độc quyền", Formatting.GOLD, "Bấm vào một món để xem công thức chế tạo"));
		for (int i = 0; i < RecipeData.ALL.size(); i++) {
			inv.setStack(FIRST_ITEM_SLOT + i, ArcadeItems.display(RecipeData.ALL.get(i), true));
		}
		open(p, Text.literal("Vật phẩm độc quyền"), ScreenHandlerType.GENERIC_9X3, 3, inv, slot -> {
			int idx = slot - FIRST_ITEM_SLOT;
			if (idx >= 0 && idx < RecipeData.ALL.size()) {
				RecipeData.R r = RecipeData.ALL.get(idx);
				later(p, q -> openRecipe(q, r));
			}
		});
	}

	// ---------- tầng 2: công thức của một món ----------

	private static void openRecipe(ServerPlayerEntity p, RecipeData.R r) {
		SimpleInventory inv = new SimpleInventory(45);
		for (int i = 0; i < 45; i++) inv.setStack(i, pane(Items.GRAY_STAINED_GLASS_PANE));
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
			if (slot == BACK_SLOT) later(p, RecipeMenu::openCatalog);
		});
	}
}
