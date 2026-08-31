package dev.noellx.inventarium.managers;

import dev.noellx.inventarium.models.ButtonConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth for protected Inventarium slots.
 * A protected slot is reserved: player inventory operations may not move another
 * item into it or move the button out of it.
 */
public final class ButtonLockManager {

    private static final ButtonLockManager INSTANCE = new ButtonLockManager();

    private final Map<UUID, Map<Integer, ItemStack>> lockedItems = new ConcurrentHashMap<>();
    private final Set<UUID> mutating = ConcurrentHashMap.newKeySet();

    private ButtonLockManager() {}

    public static ButtonLockManager getInstance() {
        return INSTANCE;
    }

    public void beginMutation(Player player) {
        mutating.add(player.getUniqueId());
    }

    public void endMutation(Player player) {
        mutating.remove(player.getUniqueId());
    }

    public boolean isMutating(Player player) {
        return mutating.contains(player.getUniqueId());
    }

    public void lock(Player player, int slot, ItemStack item) {
        lockedItems.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(slot, item.clone());
        player.getInventory().setItem(slot, item.clone());
    }

    public void unlock(Player player, int slot) {
        Map<Integer, ItemStack> map = lockedItems.get(player.getUniqueId());
        if (map != null) {
            map.remove(slot);
            if (map.isEmpty()) lockedItems.remove(player.getUniqueId());
        }
    }

    public void unlockAll(Player player) {
        lockedItems.remove(player.getUniqueId());
    }

    public boolean isLockedSlot(Player player, int slot) {
        Map<Integer, ItemStack> map = lockedItems.get(player.getUniqueId());
        return map != null && map.containsKey(slot);
    }

    public boolean hasAnyLock(Player player) {
        Map<Integer, ItemStack> map = lockedItems.get(player.getUniqueId());
        return map != null && !map.isEmpty();
    }

    public ItemStack getLockedItem(Player player, int slot) {
        Map<Integer, ItemStack> map = lockedItems.get(player.getUniqueId());
        ItemStack item = map == null ? null : map.get(slot);
        return item == null ? null : item.clone();
    }

    public ButtonConfig getButtonForLockedSlot(Player player, int slot) {
        if (!isLockedSlot(player, slot)) return null;
        return ConfigManager.getInstance().getButton(slot);
    }

    public int findLockedSlotByItem(Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return -1;
        Map<Integer, ItemStack> map = lockedItems.get(player.getUniqueId());
        if (map == null) return -1;
        for (Map.Entry<Integer, ItemStack> entry : map.entrySet()) {
            if (sameButton(entry.getValue(), stack)) return entry.getKey();
        }
        return -1;
    }

    public boolean isProtectedButtonItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return false;
        String id = InventariumKeys.getButtonId(stack);
        if (id == null) return false;
        return ConfigManager.getInstance().getButtonsBySlot().values().stream()
                .anyMatch(button -> button.isProtect() && button.getId().equals(id));
    }

    /** Replace any protected slot that was changed by non-inventory API code. */
    public int enforce(Player player) {
        if (isMutating(player)) return 0;
        Map<Integer, ItemStack> map = lockedItems.get(player.getUniqueId());
        if (map == null || map.isEmpty()) return 0;

        int corrected = 0;
        beginMutation(player);
        try {
            for (Map.Entry<Integer, ItemStack> entry : map.entrySet()) {
                int slot = entry.getKey();
                ItemStack expected = entry.getValue();
                ItemStack current = player.getInventory().getItem(slot);
                if (!sameButton(expected, current)) {
                    player.getInventory().setItem(slot, expected.clone());
                    corrected++;
                }
            }
        } finally {
            endMutation(player);
        }
        return corrected;
    }

    /**
     * Install a protected button without destroying the item that already occupies
     * the reserved slot. Existing player items are moved to an empty player slot;
     * only if every slot is occupied are they dropped at the player's feet.
     */
    public void install(Player player, ButtonConfig button, Set<Integer> allReservedSlots) {
        ItemStack menu = button.buildItem();
        int target = button.getSlot();
        ItemStack displaced = player.getInventory().getItem(target);

        beginMutation(player);
        try {
            if (displaced != null && !displaced.getType().isAir() && !sameButton(displaced, menu)) {
                Set<Integer> reserved = new HashSet<>(allReservedSlots);
                int destination = findSafePlayerSlot(player, reserved, target);
                if (destination >= 0) {
                    player.getInventory().setItem(destination, displaced.clone());
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), displaced.clone());
                }
            }

            lockedItems.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                    .put(target, menu.clone());
            player.getInventory().setItem(target, menu.clone());
        } finally {
            endMutation(player);
        }
    }

    private int findSafePlayerSlot(Player player, Set<Integer> reserved, int target) {
        // Prefer normal storage inventory before hotbar to avoid altering the active bar.
        for (int slot = 9; slot <= 35; slot++) {
            if (slot == target || reserved.contains(slot)) continue;
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) return slot;
        }
        for (int slot = 0; slot <= 8; slot++) {
            if (slot == target || reserved.contains(slot)) continue;
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) return slot;
        }
        return -1;
    }

    private boolean sameButton(ItemStack expected, ItemStack current) {
        if (expected == null || current == null || current.getType().isAir()) return false;
        String expectedId = InventariumKeys.getButtonId(expected);
        String currentId = InventariumKeys.getButtonId(current);
        return expectedId != null && expectedId.equals(currentId);
    }

    public void clearOnQuit(Player player) {
        lockedItems.remove(player.getUniqueId());
        mutating.remove(player.getUniqueId());
    }
}
