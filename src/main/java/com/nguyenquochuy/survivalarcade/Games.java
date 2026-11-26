package com.nguyenquochuy.survivalarcade;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class Games {
	private static final Map<UUID, Session> ACTIVE = new HashMap<>();
	private static final Random RNG = new Random();

	private Games() {}

	// =====================================================================
	// Quản lý phiên chơi
	// =====================================================================

	public abstract static class Session {
		final UUID pid;
		final String name;
		final ServerBossBar bar;
		int age;

		Session(ServerPlayerEntity p, String name, BossBar.Color color) {
			this.pid = p.getUuid();
			this.name = name;
			this.bar = new ServerBossBar(Text.literal(name), color, BossBar.Style.PROGRESS);
			this.bar.addPlayer(p);
		}

		/** Trả về true khi minigame tự kết thúc (thắng hoặc thua). */
		abstract boolean tick(ServerPlayerEntity p);

		void onDeath(ServerPlayerEntity p) {
			Store.say(p, name + " kết thúc vì bạn đã gục.", Formatting.RED);
			Fx.lose(p, name);
		}

		void onStop(ServerPlayerEntity p) {
			Store.say(p, "Đã dừng " + name + ".", Formatting.GRAY);
		}

		void cleanup() {
			bar.clearPlayers();
		}
	}

	public static void tick(MinecraftServer server) {
		if (!ACTIVE.isEmpty()) {
			for (Session s : new ArrayList<>(ACTIVE.values())) {
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(s.pid);
				if (p == null) {
					ACTIVE.remove(s.pid);
					s.cleanup();
					continue;
				}
				s.age++;
				boolean done;
				try {
					done = s.tick(p);
				} catch (Exception e) {
					SurvivalArcade.LOG.error("Minigame bị lỗi: " + s.name, e);
					done = true;
				}
				if (done) {
					ACTIVE.remove(s.pid);
					s.cleanup();
				}
			}
		}
		int t = server.getTicks();
		Fx.tick();
		if (t % 100 == 0) {
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				Store.applyPerks(p);
			}
		}
		if (t % 10 == 0) {
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				ArcadeBook.ensure(p);
				ArcadeItems.ensureSoulbound(p);
				ArcadeItems.tickHeld(p);
			}
		}
		if (t % 3 == 0) {
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				if (p.isAlive() && !p.isSpectator()) Aura.tick(p, t);
			}
		}
		if (t % 20 == 0) {
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				if (p.isAlive()) {
					Quests.login(p);
					ArcadeItems.tickCharm(p);
					Armor.tick(p);
				}
			}
			Events.tick(server, t);
		}
		if (t % 6000 == 0) Store.save();
	}

	public static String current(ServerPlayerEntity p) {
		Session s = ACTIVE.get(p.getUuid());
		return s == null ? null : s.name;
	}

	public static boolean stop(ServerPlayerEntity p) {
		Session s = ACTIVE.remove(p.getUuid());
		if (s == null) return false;
		s.onStop(p);
		s.cleanup();
		return true;
	}

	/** Dừng im lặng (khi người chơi thoát game). */
	public static void drop(UUID id) {
		Session s = ACTIVE.remove(id);
		if (s != null) s.cleanup();
	}

	public static void onPlayerDeath(ServerPlayerEntity p) {
		Session s = ACTIVE.remove(p.getUuid());
		if (s == null) return;
		s.onDeath(p);
		s.cleanup();
	}

	/** Bắt đầu minigame. Trả về thông báo lỗi, hoặc null nếu thành công. */
	public static String begin(ServerPlayerEntity p, String kind, int arg) {
		if (ACTIVE.containsKey(p.getUuid())) return "Bạn đang chơi một minigame rồi — dùng /mg stop để dừng.";
		ServerWorld w = p.getEntityWorld();
		Session s;
		switch (kind) {
			case "arena":
				if (!onSurface(w, p)) return "Phải đứng ngoài mặt đất ở Overworld mới mở được Đấu trường.";
				s = new Arena(p, w);
				break;
			case "survive":
				if (!onSurface(w, p)) return "Phải đứng ngoài mặt đất ở Overworld mới chơi được Sống sót.";
				s = new Survive(p, w, arg);
				break;
			case "rush":
				s = new Rush(p);
				break;
			case "forge":
				s = new Forge(p);
				break;
			case "delivery":
				if (w.getRegistryKey() != World.OVERWORLD) return "Giao hàng chỉ chơi được ở Overworld.";
				s = new Delivery(p, w);
				break;
			default:
				return "Minigame không tồn tại.";
		}
		ACTIVE.put(p.getUuid(), s);
		return null;
	}

	// =====================================================================
	// Tiện ích chung
	// =====================================================================

	static boolean onSurface(ServerWorld w, ServerPlayerEntity p) {
		if (w.getRegistryKey() != World.OVERWORLD) return false;
		int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.getX()), (int) Math.floor(p.getZ()));
		return p.getY() >= top - 3;
	}

	static String mmss(int ticks) {
		int s = Math.max(0, ticks / 20);
		return String.format("%d:%02d", s / 60, s % 60);
	}

	static EntityType<? extends MobEntity> pick(int wave) {
		List<EntityType<? extends MobEntity>> l = new ArrayList<>();
		l.add(EntityType.ZOMBIE);
		if (wave >= 3) l.add(EntityType.SKELETON);
		if (wave >= 5) l.add(EntityType.SPIDER);
		if (wave >= 7) l.add(EntityType.HUSK);
		if (wave >= 9) l.add(EntityType.WITCH);
		if (wave >= 12) l.add(EntityType.VINDICATOR);
		return l.get(RNG.nextInt(l.size()));
	}

	/** Sinh 1 con quái trên mặt đất quanh (cx, cz), bán kính rmin..rmax, và cho nó nhắm vào người chơi. */
	static MobEntity spawnAround(ServerWorld w, ServerPlayerEntity p, double cx, double cz, double rmin, double rmax,
								 EntityType<? extends MobEntity> type) {
		for (int i = 0; i < 6; i++) {
			double a = RNG.nextDouble() * Math.PI * 2;
			double r = rmin + RNG.nextDouble() * (rmax - rmin);
			int x = (int) Math.floor(cx + Math.cos(a) * r);
			int z = (int) Math.floor(cz + Math.sin(a) * r);
			int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			MobEntity m = type.spawn(w, new BlockPos(x, y, z), SpawnReason.EVENT);
			if (m != null) {
				m.setPersistent();
				// không cháy nắng giữa ban ngày để đợt quái không tự chết
				m.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 60 * 30, 0, false, false));
				m.setTarget(p);
				return m;
			}
		}
		return null;
	}

	static void pruneDead(ServerWorld w, Set<UUID> mobs) {
		mobs.removeIf(id -> {
			Entity e = w.getEntity(id);
			return e == null || !e.isAlive();
		});
	}

	static void discardAll(ServerWorld w, Set<UUID> mobs) {
		for (UUID id : mobs) {
			Entity e = w.getEntity(id);
			if (e != null) e.discard();
		}
		mobs.clear();
	}

	// =====================================================================
	// 1) Đấu trường: các đợt quái tăng dần, qua đợt nào thưởng đợt đó
	// =====================================================================

	static final class Arena extends Session {
		final ServerWorld w;
		final double cx, cz;
		final Set<UUID> mobs = new HashSet<>();
		int wave = 0, cleared = 0, total = 0, cooldown = 60;
		boolean waiting = true;

		Arena(ServerPlayerEntity p, ServerWorld w) {
			super(p, "Đấu trường", BossBar.Color.RED);
			this.w = w;
			this.cx = p.getX();
			this.cz = p.getZ();
			Store.say(p, "Đấu trường bắt đầu! Đừng đi xa quá 50 block khỏi chỗ này. Gõ /mg stop để dừng.", Formatting.RED);
		}

		@Override
		boolean tick(ServerPlayerEntity p) {
			if (p.getEntityWorld() != w || Math.hypot(p.getX() - cx, p.getZ() - cz) > 50) {
				Store.say(p, "Bạn đã rời đấu trường!", Formatting.RED);
				report(p);
				return true;
			}
			if (waiting) {
				cooldown--;
				bar.setName(Text.literal("Đợt " + (wave + 1) + " bắt đầu sau " + (cooldown / 20 + 1) + "s"));
				bar.setPercent(1f);
				if (cooldown <= 0) {
					waiting = false;
					spawnWave(p);
				}
				return false;
			}
			if (age % 10 == 0) pruneDead(w, mobs);
			bar.setName(Text.literal("Đợt " + wave + " — còn " + mobs.size() + " quái"));
			bar.setPercent(total == 0 ? 0f : Math.min(1f, mobs.size() / (float) total));
			if (mobs.isEmpty()) clearWave(p);
			return false;
		}

		void spawnWave(ServerPlayerEntity p) {
			wave++;
			int want = Math.min(40, 3 + wave * 2);
			for (int i = 0; i < want; i++) {
				MobEntity m = spawnAround(w, p, cx, cz, 8, 14, pick(wave));
				if (m != null) mobs.add(m.getUuid());
			}
			total = mobs.size();
			if (total == 0) {
				wave--;
				waiting = true;
				cooldown = 100;
				Store.say(p, "Không sinh được quái ở chỗ này, thử lại sau vài giây...", Formatting.GRAY);
				return;
			}
			Store.say(p, "Đợt " + wave + ": " + total + " quái!", Formatting.YELLOW);
			Fx.waveStart(p, wave, total);
		}

		void clearWave(ServerPlayerEntity p) {
			cleared = wave;
			int got = Store.addPoints(p, 8 + 4 * wave, "combat");
			Store.PData d = Store.get(p);
			if (cleared > d.bestWave) d.bestWave = cleared;
			Store.say(p, "Vượt qua đợt " + wave + "! +" + got + " điểm", Formatting.GREEN);
			Fx.waveClear(p, wave);
			Quests.arenaWave(p, cleared);
			if (cleared >= 5) Store.grant(p, "wave_5");
			if (cleared >= 10) Store.grant(p, "wave_10");
			waiting = true;
			cooldown = 100;
		}

		void report(ServerPlayerEntity p) {
			Store.say(p, "Đấu trường kết thúc — vượt qua " + cleared + " đợt (kỷ lục: " + Store.get(p).bestWave + ").", Formatting.GOLD);
			Fx.title(p, "KẾT THÚC", "Vượt qua " + cleared + " đợt", Formatting.GOLD);
		}

		@Override
		void onDeath(ServerPlayerEntity p) {
			report(p);
		}

		@Override
		void onStop(ServerPlayerEntity p) {
			report(p);
		}

		@Override
		void cleanup() {
			discardAll(w, mobs);
			super.cleanup();
		}
	}

	// =====================================================================
	// 2) Sống sót X phút
	// =====================================================================

	static final class Survive extends Session {
		final ServerWorld w;
		final int minutes, total;
		final Set<UUID> mobs = new HashSet<>();

		Survive(ServerPlayerEntity p, ServerWorld w, int minutes) {
			super(p, "Sống sót", BossBar.Color.PURPLE);
			this.w = w;
			this.minutes = minutes;
			this.total = minutes * 1200;
			Store.say(p, "Sống sót " + minutes + " phút! Quái kéo đến liên tục, chết là thua, trụ đủ giờ nhận thưởng.", Formatting.LIGHT_PURPLE);
		}

		@Override
		boolean tick(ServerPlayerEntity p) {
			if (p.getEntityWorld() != w) {
				Store.say(p, "Bạn rời Overworld — Sống sót bị hủy.", Formatting.RED);
				return true;
			}
			if (age >= total) {
				int got = Store.addPoints(p, 20 * minutes, "combat");
				p.addExperience(20 * minutes);
				Store.get(p).surviveWins++;
				Store.grant(p, "survivor");
				Store.say(p, "Bạn đã sống sót " + minutes + " phút! +" + got + " điểm, +" + (20 * minutes) + " XP", Formatting.GREEN);
				Fx.win(p, "CHIẾN THẮNG", "Sống sót " + minutes + " phút");
				return true;
			}
			if (age % 20 == 0) pruneDead(w, mobs);
			if (age % 80 == 0 && mobs.size() < 14) {
				int tier = 2 + age / 1200;
				for (int i = 0; i < 2; i++) {
					MobEntity m = spawnAround(w, p, p.getX(), p.getZ(), 12, 18, pick(tier));
					if (m != null) mobs.add(m.getUuid());
				}
			}
			bar.setName(Text.literal("Sống sót — còn " + mmss(total - age)));
			bar.setPercent(Math.max(0f, (total - age) / (float) total));
			return false;
		}

		@Override
		void cleanup() {
			discardAll(w, mobs);
			super.cleanup();
		}
	}

	// =====================================================================
	// 3) Rush Mode: 5 phút gom đủ tài nguyên, thắng được buff tạm thời
	// =====================================================================

	static final class Rush extends Session {
		record Goal(Item item, int need, String label) {}

		static final List<Goal> POOL = List.of(
			new Goal(Items.COBBLESTONE, 48, "đá cuội"),
			new Goal(Items.OAK_LOG, 24, "gỗ sồi"),
			new Goal(Items.COAL, 12, "than"),
			new Goal(Items.RAW_IRON, 6, "sắt thô"),
			new Goal(Items.SAND, 32, "cát"),
			new Goal(Items.DIRT, 64, "đất"),
			new Goal(Items.SUGAR_CANE, 10, "mía")
		);
		static final int TIME = 20 * 60 * 5;

		final List<Goal> goals;
		final int[] base = new int[3];

		Rush(ServerPlayerEntity p) {
			super(p, "Rush Mode", BossBar.Color.YELLOW);
			List<Goal> l = new ArrayList<>(POOL);
			Collections.shuffle(l, RNG);
			goals = new ArrayList<>(l.subList(0, 3));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 3; i++) {
				Goal g = goals.get(i);
				base[i] = p.getInventory().count(g.item());
				sb.append(g.need()).append(' ').append(g.label());
				if (i < 2) sb.append(", ");
			}
			Store.say(p, "Rush Mode! 5 phút để gom: " + sb + " (giữ trong túi đồ). Thắng nhận buff Speed + Haste.", Formatting.YELLOW);
		}

		@Override
		boolean tick(ServerPlayerEntity p) {
			if (age % 10 == 0) {
				int done = 0;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < 3; i++) {
					Goal g = goals.get(i);
					int have = Math.max(0, p.getInventory().count(g.item()) - base[i]);
					if (have >= g.need()) done++;
					sb.append(g.label()).append(' ').append(Math.min(have, g.need())).append('/').append(g.need());
					if (i < 2) sb.append("  |  ");
				}
				Store.bar(p, sb.toString());
				if (done == 3) {
					int got = Store.addPoints(p, 30, "mining");
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3600, 1));
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 3600, 1));
					Store.get(p).rushWins++;
					Store.grant(p, "rusher");
					Store.say(p, "Rush hoàn thành! +" + got + " điểm, Speed II + Haste II trong 3 phút.", Formatting.GREEN);
					Fx.win(p, "CHIẾN THẮNG", "Rush Mode");
					return true;
				}
			}
			int left = TIME - age;
			if (left <= 0) {
				Store.say(p, "Hết giờ! Rush Mode thất bại.", Formatting.RED);
				Fx.lose(p, "Rush Mode hết giờ");
				return true;
			}
			bar.setName(Text.literal("Rush Mode — còn " + mmss(left)));
			bar.setPercent(left / (float) TIME);
			return false;
		}
	}

	// =====================================================================
	// 4) Rèn đồ: chế đúng món trong thời gian giới hạn, nhận đồ xịn miễn phí
	// =====================================================================

	static final class Forge extends Session {
		record Job(Item target, int secs, String label, String hint, String rewardLabel, Supplier<ItemStack> reward) {}

		static final List<Job> JOBS = List.of(
			new Job(Items.STONE_PICKAXE, 90, "cúp đá", "3 đá cuội + 2 gậy", "cúp sắt", () -> new ItemStack(Items.IRON_PICKAXE)),
			new Job(Items.STONE_SWORD, 90, "kiếm đá", "2 đá cuội + 1 gậy", "kiếm sắt", () -> new ItemStack(Items.IRON_SWORD)),
			new Job(Items.BREAD, 90, "bánh mì", "3 lúa mì", "16 bò nướng", () -> new ItemStack(Items.COOKED_BEEF, 16)),
			new Job(Items.BUCKET, 120, "xô", "3 thỏi sắt", "2 kim cương", () -> new ItemStack(Items.DIAMOND, 2)),
			new Job(Items.SHIELD, 150, "khiên", "1 thỏi sắt + 6 ván gỗ", "giáp ngực sắt", () -> new ItemStack(Items.IRON_CHESTPLATE)),
			new Job(Items.BOW, 120, "cung", "3 gậy + 3 dây", "32 mũi tên", () -> new ItemStack(Items.ARROW, 32))
		);

		final Job job;
		final int base, total;

		Forge(ServerPlayerEntity p) {
			super(p, "Rèn đồ", BossBar.Color.GREEN);
			job = JOBS.get(RNG.nextInt(JOBS.size()));
			base = p.getInventory().count(job.target());
			total = job.secs() * 20;
			Store.say(p, "Rèn đồ! Chế " + job.label() + " trong " + job.secs() + "s (" + job.hint() + "). Thưởng: " + job.rewardLabel() + ".", Formatting.GREEN);
		}

		@Override
		boolean tick(ServerPlayerEntity p) {
			if (age % 10 == 0 && p.getInventory().count(job.target()) - base >= 1) {
				int got = Store.addPoints(p, 20, "craft");
				p.getInventory().offerOrDrop(job.reward().get());
				Store.get(p).forgeWins++;
				Store.grant(p, "smith");
				Store.say(p, "Rèn xong " + job.label() + "! +" + got + " điểm, nhận " + job.rewardLabel() + ".", Formatting.GREEN);
				Fx.win(p, "CHIẾN THẮNG", "Rèn xong " + job.label());
				return true;
			}
			int left = total - age;
			if (left <= 0) {
				Store.say(p, "Hết giờ! Rèn đồ thất bại.", Formatting.RED);
				Fx.lose(p, "Rèn đồ hết giờ");
				return true;
			}
			bar.setName(Text.literal("Rèn " + job.label() + " — còn " + mmss(left)));
			bar.setPercent(left / (float) total);
			return false;
		}
	}

	// =====================================================================
	// 5) Giao hàng: đi tới tọa độ xa, càng xa thưởng càng lớn
	// =====================================================================

	static final class Delivery extends Session {
		static final int LIMIT = 20 * 60 * 10;
		static final String[] DIRS = {"Bắc", "Đông Bắc", "Đông", "Đông Nam", "Nam", "Tây Nam", "Tây", "Tây Bắc"};

		final ServerWorld w;
		final int tx, tz, dist;

		Delivery(ServerPlayerEntity p, ServerWorld w) {
			super(p, "Giao hàng", BossBar.Color.BLUE);
			this.w = w;
			this.dist = 150 + RNG.nextInt(251);
			double a = RNG.nextDouble() * Math.PI * 2;
			this.tx = (int) Math.round(p.getX() + Math.cos(a) * dist);
			this.tz = (int) Math.round(p.getZ() + Math.sin(a) * dist);
			Store.say(p, "Giao hàng! Đến tọa độ X: " + tx + "  Z: " + tz + " (~" + dist + " block) trong 10 phút. Càng xa thưởng càng lớn.", Formatting.AQUA);
		}

		@Override
		boolean tick(ServerPlayerEntity p) {
			if (p.getEntityWorld() != w) {
				Store.say(p, "Bạn rời Overworld — Giao hàng bị hủy.", Formatting.RED);
				return true;
			}
			double dx = tx - p.getX();
			double dz = tz - p.getZ();
			double d = Math.hypot(dx, dz);
			if (d <= 8) {
				int got = Store.addPoints(p, 10 + dist / 6, "adventure");
				p.addExperience(dist / 5);
				Store.get(p).delivered++;
				Store.grant(p, "courier");
				if (dist >= 300) Store.grant(p, "far_courier");
				Store.say(p, "Giao hàng thành công! +" + got + " điểm, +" + (dist / 5) + " XP", Formatting.GREEN);
				Fx.win(p, "CHIẾN THẮNG", "Giao hàng thành công");
				return true;
			}
			int left = LIMIT - age;
			if (left <= 0) {
				Store.say(p, "Hết giờ! Giao hàng thất bại.", Formatting.RED);
				Fx.lose(p, "Giao hàng hết giờ");
				return true;
			}
			if (age % 20 == 0) {
				Store.bar(p, "Giao hàng → X: " + tx + " Z: " + tz + " | còn " + (int) d + " block, hướng " + dir(dx, dz));
			}
			bar.setName(Text.literal("Giao hàng — còn " + mmss(left)));
			bar.setPercent(left / (float) LIMIT);
			return false;
		}

		static String dir(double dx, double dz) {
			double ang = Math.toDegrees(Math.atan2(dx, -dz));
			if (ang < 0) ang += 360;
			return DIRS[(int) Math.round(ang / 45) % 8];
		}
	}
}
