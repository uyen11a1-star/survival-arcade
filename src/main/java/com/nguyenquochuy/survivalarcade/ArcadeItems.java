package com.nguyenquochuy.survivalarcade;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vật phẩm độc quyền. Mỗi món được nhận diện bằng component item_model có namespace "survivalarcade"
 * (công thức trong data/survivalarcade/recipe tạo ra đúng component này).
 */
public final class ArcadeItems {
	public static final String NS = "survivalarcade";

	private record Info(String name, Formatting color) {}

	private static final Map<String, Info> UPGRADABLE = Map.of(
		"thunder_blade", new Info("Kiếm Sấm Sét", Formatting.AQUA),
		"blink_staff", new Info("Gậy Dịch Chuyển", Formatting.GREEN),
		"miner_pick", new Info("Cúp Thợ Mỏ", Formatting.LIGHT_PURPLE),
		"power_wand", new Info("Gậy Quyền Năng", Formatting.GOLD)
	);

	/** Vật phẩm dùng bằng chuột phải. */
	private static final Set<String> USABLE = Set.of(
		"skill_dash", "skill_heal", "skill_shock", "thunder_blade", "blink_staff", "power_wand");

	/** Vật phẩm cấp sẵn, không vứt được, chết không mất (như sổ tay). */
	private static final List<String> SOULBOUND = List.of("skill_dash", "skill_heal", "skill_shock");

	private static final Pattern TIER = Pattern.compile("\\+(\\d+)$");

	private static final Map<String, Item> VANILLA = Map.ofEntries(
		Map.entry("minecraft:lightning_rod", Items.LIGHTNING_ROD),
		Map.entry("minecraft:netherite_sword", Items.NETHERITE_SWORD),
		Map.entry("minecraft:diamond_block", Items.DIAMOND_BLOCK),
		Map.entry("minecraft:ender_pearl", Items.ENDER_PEARL),
		Map.entry("minecraft:ender_eye", Items.ENDER_EYE),
		Map.entry("minecraft:blaze_rod", Items.BLAZE_ROD),
		Map.entry("minecraft:redstone_block", Items.REDSTONE_BLOCK),
		Map.entry("minecraft:netherite_pickaxe", Items.NETHERITE_PICKAXE),
		Map.entry("minecraft:emerald", Items.EMERALD),
		Map.entry("minecraft:diamond", Items.DIAMOND),
		Map.entry("minecraft:totem_of_undying", Items.TOTEM_OF_UNDYING),
		Map.entry("minecraft:compass", Items.COMPASS),
		Map.entry("minecraft:nether_star", Items.NETHER_STAR),
		Map.entry("minecraft:stick", Items.STICK),
		Map.entry("minecraft:heart_of_the_sea", Items.HEART_OF_THE_SEA)
	);

	private ArcadeItems() {}

	/** Vật phẩm vanilla theo id (dùng cho menu công thức). */
	public static Item vanilla(String id) {
		Item i = VANILLA.get(id);
		return i == null ? Items.BARRIER : i;
	}

	/** Stack chỉ để hiển thị trong menu công thức, giống hệt món chế tạo ra (tên, mô tả, model riêng). */
	public static ItemStack display(RecipeData.R r, boolean hint) {
		ItemStack s = new ItemStack(vanilla(r.base()));
		s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(r.title()).formatted(r.color(), Formatting.BOLD).styled(st -> st.withItalic(false)));
		List<Text> lore = new ArrayList<>();
		for (String l : r.lore()) lore.add(line(l, Formatting.GRAY));
		if (hint) {
			lore.add(line(" ", Formatting.GRAY));
			lore.add(line("▶ Bấm để xem công thức chế tạo", Formatting.YELLOW));
		}
		s.set(DataComponentTypes.LORE, new LoreComponent(lore));
		s.set(DataComponentTypes.ITEM_MODEL, Identifier.of(NS, r.id()));
		s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		return s;
	}

	// =====================================================================
	// Nhận diện
	// =====================================================================

	/** Trả về id vật phẩm độc quyền (ví dụ "thunder_blade") hoặc null. */
	public static String id(ItemStack s) {
		if (s.isEmpty()) return null;
		Identifier m = s.get(DataComponentTypes.ITEM_MODEL);
		return (m != null && NS.equals(m.getNamespace())) ? m.getPath() : null;
	}

	private static boolean isSoulbound(ItemStack s) {
		String id = id(s);
		return id != null && SOULBOUND.contains(id);
	}

	/** Bậc nâng cấp, đọc từ đuôi "+N" của tên vật phẩm. */
	public static int tierOf(ItemStack s) {
		Text n = s.get(DataComponentTypes.CUSTOM_NAME);
		if (n == null) return 0;
		Matcher m = TIER.matcher(n.getString());
		return m.find() ? Integer.parseInt(m.group(1)) : 0;
	}

	private static boolean has(ServerPlayerEntity p, String id) {
		PlayerInventory inv = p.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (id.equals(id(inv.getStack(i)))) return true;
		}
		return id.equals(id(p.currentScreenHandler.getCursorStack()));
	}

	public static boolean holdingCompass(ServerPlayerEntity p) {
		return "hunter_compass".equals(id(p.getMainHandStack())) || "hunter_compass".equals(id(p.getOffHandStack()));
	}

	// =====================================================================
	// Vật phẩm kỹ năng cấp sẵn
	// =====================================================================

	private static Text line(String s, Formatting f) {
		return Text.literal(s).formatted(f).styled(st -> st.withItalic(false));
	}

	private static ItemStack make(Item base, String id, String name, Formatting color, String lore) {
		ItemStack s = new ItemStack(base);
		s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(color, Formatting.BOLD).styled(st -> st.withItalic(false)));
		s.set(DataComponentTypes.LORE, new LoreComponent(List.of(line(lore, Formatting.GRAY))));
		s.set(DataComponentTypes.ITEM_MODEL, Identifier.of(NS, id));
		s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		return s;
	}

	private static ItemStack soulbound(String id) {
		return switch (id) {
			case "skill_dash" -> make(Items.FEATHER, id, "Lướt Nhanh", Formatting.AQUA, "Chuột phải: lướt về phía trước");
			case "skill_heal" -> make(Items.GHAST_TEAR, id, "Hồi Máu", Formatting.GREEN, "Chuột phải: hồi máu (cấp 3)");
			default -> make(Items.BLAZE_POWDER, id, "Sóng Xung Kích", Formatting.RED, "Chuột phải: đẩy lùi và đánh quái quanh bạn (cấp 5)");
		};
	}

	/** Gọi định kỳ: dọn vật phẩm kỹ năng bị vứt và cấp lại nếu thiếu. */
	public static void ensureSoulbound(ServerPlayerEntity p) {
		if (!p.isAlive()) return;
		ServerWorld w = p.getEntityWorld();
		Box box = new Box(p.getX() - 8, p.getY() - 8, p.getZ() - 8, p.getX() + 8, p.getY() + 8, p.getZ() + 8);
		for (ItemEntity e : w.getEntitiesByClass(ItemEntity.class, box, ie -> isSoulbound(ie.getStack()))) {
			e.discard();
		}
		PlayerInventory inv = p.getInventory();
		for (String sid : SOULBOUND) {
			if (!has(p, sid) && inv.getEmptySlot() >= 0) inv.offerOrDrop(soulbound(sid));
		}
	}

	/** Gỡ vật phẩm kỹ năng khỏi túi đồ trước khi chết. */
	public static void strip(ServerPlayerEntity p) {
		PlayerInventory inv = p.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (isSoulbound(inv.getStack(i))) inv.setStack(i, ItemStack.EMPTY);
		}
	}

	// =====================================================================
	// Chuột phải
	// =====================================================================

	public static ActionResult onUse(PlayerEntity player, World world, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		String id = id(stack);
		if (id == null || !USABLE.contains(id)) return ActionResult.PASS;
		if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.SUCCESS; // phía client chỉ để vung tay
		String err = switch (id) {
			case "skill_dash" -> Skills.use(sp, "dash");
			case "skill_heal" -> Skills.use(sp, "heal");
			case "skill_shock" -> Skills.use(sp, "shock");
			default -> Weapons.use(sp, id, tierOf(stack));
		};
		if (err != null) Store.bar(sp, err);
		return ActionResult.SUCCESS;
	}

	// =====================================================================
	// Cúp Thợ Mỏ: phá cả mạch quặng
	// =====================================================================

	private static boolean isOre(BlockState s) {
		return s.isIn(BlockTags.COAL_ORES) || s.isIn(BlockTags.IRON_ORES) || s.isIn(BlockTags.GOLD_ORES)
			|| s.isIn(BlockTags.COPPER_ORES) || s.isIn(BlockTags.REDSTONE_ORES) || s.isIn(BlockTags.LAPIS_ORES)
			|| s.isIn(BlockTags.DIAMOND_ORES) || s.isIn(BlockTags.EMERALD_ORES);
	}

	public static void onBreak(ServerPlayerEntity p, World world, BlockPos pos, BlockState state) {
		ItemStack main = p.getMainHandStack();
		if (!"miner_pick".equals(id(main)) || p.isSneaking() || !isOre(state)) return;
		if (!(world instanceof ServerWorld w)) return;
		int max = 12 + 4 * tierOf(main);
		Block type = state.getBlock();
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		seen.add(pos);
		queue.add(pos);
		int broken = 0;
		outer:
		while (!queue.isEmpty()) {
			BlockPos c = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) continue;
						BlockPos n = c.add(dx, dy, dz);
						if (!seen.add(n)) continue;
						BlockState ns = w.getBlockState(n);
						if (!ns.isOf(type)) continue;
						if (broken >= max) break outer;
						Quests.onMine(p, ns);
						w.breakBlock(n, true, p);
						broken++;
						queue.add(n);
					}
				}
			}
		}
		if (broken > 0) {
			Fx.burst(p, ParticleTypes.ENCHANT, 25);
			Store.bar(p, "Cúp Thợ Mỏ: phá thêm " + broken + " quặng");
		}
	}

	// =====================================================================
	// Nâng cấp bằng điểm + kim cương
	// =====================================================================

	private static Text named(Info i, int tier) {
		MutableText t = Text.literal(i.name()).formatted(i.color(), Formatting.BOLD).styled(st -> st.withItalic(false));
		if (tier > 0) t.append(Text.literal(" +" + tier).formatted(Formatting.YELLOW));
		return t;
	}

	private static void take(PlayerInventory inv, Item item, int n) {
		for (int i = 0; i < inv.size() && n > 0; i++) {
			ItemStack s = inv.getStack(i);
			if (s.isOf(item)) {
				int t = Math.min(n, s.getCount());
				s.decrement(t);
				n -= t;
			}
		}
	}

	/** Nâng cấp vũ khí độc quyền đang cầm ở tay chính. Trả về thông báo kết quả. */
	public static String upgrade(ServerPlayerEntity p) {
		ItemStack s = p.getMainHandStack();
		String id = id(s);
		Info info = id == null ? null : UPGRADABLE.get(id);
		if (info == null) return "Hãy cầm một vũ khí độc quyền trên tay chính để nâng cấp.";
		int tier = tierOf(s);
		if (tier >= 5) return info.name() + " đã đạt bậc tối đa (+5).";
		int cost = 50 * (tier + 1);
		int dia = tier + 1;
		Store.PData d = Store.get(p);
		PlayerInventory inv = p.getInventory();
		if (d.points < cost) return "Cần " + cost + " điểm để nâng cấp (bạn có " + d.points + ").";
		if (inv.count(Items.DIAMOND) < dia) return "Cần " + dia + " kim cương trong túi đồ.";
		d.points -= cost;
		take(inv, Items.DIAMOND, dia);
		s.set(DataComponentTypes.CUSTOM_NAME, named(info, tier + 1));
		Fx.title(p, "NÂNG CẤP", info.name() + " +" + (tier + 1), Formatting.GOLD);
		Fx.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2f);
		Fx.burst(p, ParticleTypes.TOTEM_OF_UNDYING, 40);
		Fx.helix(p, ParticleTypes.END_ROD, 30);
		return "Đã nâng " + info.name() + " lên +" + (tier + 1) + " (-" + cost + " điểm, -" + dia + " kim cương).";
	}

	// =====================================================================
	// Tick: hiệu ứng khi cầm, thanh trạng thái, bùa hộ mệnh
	// =====================================================================

	/** Gọi mỗi 10 tick cho từng người chơi. */
	public static void tickHeld(ServerPlayerEntity p) {
		ItemStack main = p.getMainHandStack();
		String id = id(main);
		if (id == null) {
			if (holdingCompass(p) && Games.current(p) == null) Store.bar(p, Events.trackLine(p));
			return;
		}
		int tier = tierOf(main);
		if ("miner_pick".equals(id)) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 1 + tier / 3, true, false, false));
		}
		if (Games.current(p) != null) return;
		String hud = switch (id) {
			case "skill_dash" -> Skills.status(p, "dash");
			case "skill_heal" -> Skills.status(p, "heal");
			case "skill_shock" -> Skills.status(p, "shock");
			case "hunter_compass" -> Events.trackLine(p);
			default -> Weapons.hud(p, id);
		};
		if (hud != null) Store.bar(p, hud);
	}

	/** Gọi mỗi giây: bùa hộ mệnh tự cứu khi sắp chết. */
	public static void tickCharm(ServerPlayerEntity p) {
		if (!p.isAlive() || p.getHealth() > 6.0f || !has(p, "charm")) return;
		if (Cd.use(p, "charm", 2400) > 0) return;
		p.heal(10.0f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
		Fx.title(p, "BÙA HỘ MỆNH", "Bạn được bảo vệ!", Formatting.GOLD);
		Fx.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f);
		Fx.burst(p, ParticleTypes.TOTEM_OF_UNDYING, 50);
		Fx.helix(p, ParticleTypes.END_ROD, 30);
	}
}
