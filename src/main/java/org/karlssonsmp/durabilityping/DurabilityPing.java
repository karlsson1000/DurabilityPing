package org.karlssonsmp.durabilityping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.karlssonsmp.durabilityping.config.DurabilityPingConfig;
import java.util.HashSet;
import java.util.Set;

public class DurabilityPing implements ClientModInitializer {

	private static final int CHECK_INTERVAL = 20; // Every second

	private final Set<String> warned = new HashSet<>();
	private int ticks = 0;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.world == null || ++ticks < CHECK_INTERVAL) return;

			ticks = 0;
			checkDurability(client);
		});
	}

	private void checkDurability(MinecraftClient client) {
		if (client.player == null) return;

		// Check hands
		checkItem(client.player.getMainHandStack(), "mainhand");
		checkItem(client.player.getOffHandStack(), "offhand");

		// Check armor slots
		checkItem(client.player.getEquippedStack(EquipmentSlot.HEAD), "head");
		checkItem(client.player.getEquippedStack(EquipmentSlot.CHEST), "chest");
		checkItem(client.player.getEquippedStack(EquipmentSlot.LEGS), "legs");
		checkItem(client.player.getEquippedStack(EquipmentSlot.FEET), "feet");
	}

	private double getThresholdForItem(ItemStack stack) {
		DurabilityPingConfig config = DurabilityPingConfig.getInstance();
		int unbreakingLevel = 0;

		for (var entry : stack.getEnchantments().getEnchantments()) {
			if (entry.matchesKey(Enchantments.UNBREAKING)) {
				unbreakingLevel = stack.getEnchantments().getLevel(entry);
				break;
			}
		}

		return switch (unbreakingLevel) {
			case 1 -> config.threshold * 0.6;
			case 2 -> config.threshold * 0.4;
			case 3 -> config.threshold * 0.2;
			default -> config.threshold;
		};
	}

	private void checkItem(ItemStack stack, String slot) {
		if (stack.isEmpty() || !stack.isDamageable()) return;

		String key = slot + ":" + stack.getItem();
		double durabilityPercent = (double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
		double threshold = getThresholdForItem(stack);

		if (durabilityPercent <= threshold && warned.add(key)) {
			warnPlayer(stack);
		}

		if (durabilityPercent > threshold + 0.05) {
			warned.remove(key);
		}
	}

	private void warnPlayer(ItemStack stack) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		DurabilityPingConfig config = DurabilityPingConfig.getInstance();

		if (config.enableSound) {
			client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
		}

		Text message = Text.literal("⚠ Your " + stack.getName().getString() + " is almost broken!")
				.formatted(Formatting.RED);
		client.player.sendMessage(message, true);
	}
}