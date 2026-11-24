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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Menu cửa hàng dạng rương 3 hàng: bấm vào món để mua, không lấy được đồ ra khỏi menu. */
public final class ShopMenu extends GenericContainerScreenHandler {
	private static final int FIRST_GOOD_SLOT = 10;

	private final SimpleInventory inv;

	private ShopMenu(int syncId, PlayerInventory playerInv, SimpleInventory inv) {
		super(ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3);
		this.inv = inv;
	}

	public static void open(ServerPlayerEntity p) {
		SimpleInventory inv = new SimpleInventory(27);
		fill(inv, p);
		p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
			(syncId, playerInv, player) -> new ShopMenu(syncId, playerInv, inv),
			Text.literal("Cửa hàng điểm")));
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		int idx = slotIndex - FIRST_GOOD_SLOT;
		if (actionType == SlotActionType.PICKUP && idx >= 0 && idx < Shop.ALL.size()
			&& player instanceof ServerPlayerEntity sp) {
			String msg = Shop.buy(sp, Shop.ALL.get(idx).id());
			Store.say(sp, msg, Formatting.YELLOW);
			Fx.sound(sp, msg.startsWith("Đã mua") ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.ENTITY_VILLAGER_NO, 1.0f);
			fill(inv, sp);
		}
		// mọi thao tác khác đều bị chặn: không nhặt / đặt / kéo đồ trong menu
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	private static Text t(String s, Formatting f) {
		return Text.literal(s).formatted(f).styled(st -> st.withItalic(false));
	}

	private static void fill(SimpleInventory inv, ServerPlayerEntity p) {
		Store.PData d = Store.get(p);
		int lv = Store.level(d.xp);

		ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
		for (int i = 0; i < 27; i++) inv.setStack(i, pane.copy());

		ItemStack info = new ItemStack(Items.GOLD_INGOT);
		info.set(DataComponentTypes.CUSTOM_NAME, t("Điểm: " + d.points + "  |  Cấp " + lv, Formatting.GOLD));
		info.set(DataComponentTypes.LORE, new LoreComponent(List.of(t("Bấm vào món để mua", Formatting.GRAY))));
		inv.setStack(4, info);

		for (int i = 0; i < Shop.ALL.size(); i++) {
			Shop.Good g = Shop.ALL.get(i);
			ItemStack icon = g.stack() == null ? new ItemStack(Items.EXPERIENCE_BOTTLE) : g.stack().get();
			boolean lvOk = lv >= g.minLevel();
			boolean ptOk = d.points >= g.cost();
			icon.set(DataComponentTypes.CUSTOM_NAME, t(g.label(), Formatting.YELLOW));
			icon.set(DataComponentTypes.LORE, new LoreComponent(List.of(
				t("Giá: " + g.cost() + " điểm", ptOk ? Formatting.GREEN : Formatting.RED),
				t("Cần cấp " + g.minLevel(), lvOk ? Formatting.GREEN : Formatting.RED))));
			inv.setStack(FIRST_GOOD_SLOT + i, icon);
		}
	}
}
