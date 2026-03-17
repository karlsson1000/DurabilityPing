package org.karlssonsmp.durabilityping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.karlssonsmp.durabilityping.config.DurabilityPingConfig;
import java.util.HashSet;
import java.util.Set;

public class DurabilityPing implements ClientModInitializer {

	private final Set<String> warned = new HashSet<>();
	private final int[] previousDamages = new int[6];
	private RegistryEntry<Enchantment> unbreakingEntry = null;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.world == null) return;

			if (unbreakingEntry == null) {
				unbreakingEntry = client.world.getRegistryManager()
						.getOrThrow(RegistryKeys.ENCHANTMENT)
						.getOptional(Enchantments.UNBREAKING)
						.orElse(null);
			}

			checkDurability(client);
		});
	}

	private void checkDurability(MinecraftClient client) {
		if (client.player == null) return;

		ItemStack[] items = {
				client.player.getMainHandStack(),
				client.player.getOffHandStack(),
				client.player.getEquippedStack(EquipmentSlot.HEAD),
				client.player.getEquippedStack(EquipmentSlot.CHEST),
				client.player.getEquippedStack(EquipmentSlot.LEGS),
				client.player.getEquippedStack(EquipmentSlot.FEET)
		};

		String[] slots = {"mainhand", "offhand", "head", "chest", "legs", "feet"};

		for (int i = 0; i < items.length; i++) {
			ItemStack stack = items[i];
			if (stack.isEmpty() || !stack.isDamageable()) {
				previousDamages[i] = 0;
				continue;
			}

			int currentDamage = stack.getDamage();

			// Only check if damage has changed
			if (currentDamage != previousDamages[i]) {
				previousDamages[i] = currentDamage;
				checkItem(stack, slots[i]);
			}
		}
	}

	private int getUnbreakingLevel(ItemStack stack) {
		if (unbreakingEntry == null) return 0;
		return stack.getEnchantments().getLevel(unbreakingEntry);
	}

	private double getThresholdForItem(ItemStack stack) {
		DurabilityPingConfig config = DurabilityPingConfig.getInstance();
		return switch (getUnbreakingLevel(stack)) {
			case 1 -> Math.min(config.threshold * 1.2, 1.0);
			case 2 -> Math.min(config.threshold * 1.5, 1.0);
			case 3 -> Math.min(config.threshold * 2.0, 1.0);
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