package org.karlssonsmp.durabilityping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.karlssonsmp.durabilityping.config.DurabilityPingConfig;

import java.util.HashSet;
import java.util.Set;

public class DurabilityPing implements ClientModInitializer {

	private final Set<String> warned = new HashSet<>();
	private final int[] previousDamages = new int[6];
	private Holder<Enchantment> unbreakingEntry = null;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) return;

			if (unbreakingEntry == null) {
				unbreakingEntry = client.player.connection.registryAccess()
						.lookupOrThrow(Registries.ENCHANTMENT)
						.get(Enchantments.UNBREAKING)
						.orElse(null);
			}

			checkDurability(client);
		});
	}

	private void checkDurability(Minecraft client) {
		if (client.player == null) return;

		ItemStack[] items = {
				client.player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND),
				client.player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND),
				client.player.getItemBySlot(EquipmentSlot.HEAD),
				client.player.getItemBySlot(EquipmentSlot.CHEST),
				client.player.getItemBySlot(EquipmentSlot.LEGS),
				client.player.getItemBySlot(EquipmentSlot.FEET)
		};

		String[] slots = {"mainhand", "offhand", "head", "chest", "legs", "feet"};

		for (int i = 0; i < items.length; i++) {
			ItemStack stack = items[i];
			if (stack.isEmpty() || !stack.isDamageableItem()) {
				previousDamages[i] = 0;
				continue;
			}

			int currentDamage = stack.getDamageValue();

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
		if (stack.isEmpty() || !stack.isDamageableItem()) return;

		String key = slot + ":" + stack.getItem();
		double durabilityPercent = (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
		double threshold = getThresholdForItem(stack);

		if (durabilityPercent <= threshold && warned.add(key)) {
			warnPlayer(stack);
		}

		if (durabilityPercent > threshold + 0.05) {
			warned.remove(key);
		}
	}

	private void warnPlayer(ItemStack stack) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		DurabilityPingConfig config = DurabilityPingConfig.getInstance();

		if (config.enableSound) {
			client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
		}

		Component message = Component.literal("⚠ Your " + stack.getHoverName().getString() + " is almost broken!")
				.withStyle(ChatFormatting.RED);
		client.player.sendOverlayMessage(message);
	}
}