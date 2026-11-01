package dev.sortkey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Sortiert rein clientseitig ueber normale Klicks (wie ein Spieler es von Hand taete).
 * Funktioniert dadurch auf Servern ohne Server-Mod.
 */
final class Sorter {
    private Sorter() {}

    /** Alphabetisch nach Anzeigename, dann nach Item-ID, dann groessere Stapel zuerst. */
    private static final Comparator<ItemStack> ORDER = Comparator
            .comparing((ItemStack s) -> s.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
            .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());

    static void sort(Minecraft mc, AbstractContainerMenu menu) {
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gameMode = mc.gameMode;
        if (player == null || gameMode == null) {
            return;
        }

        boolean storage = menu instanceof ChestMenu
                || menu instanceof ShulkerBoxMenu
                || menu instanceof HopperMenu
                || menu instanceof DispenserMenu;
        if (!storage && !(menu instanceof InventoryMenu)) {
            return; // Amboss, Ofen, Handel usw. nicht anfassen
        }

        if (!menu.getCarried().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.sortkey.cursor_not_empty"));
            return;
        }

        Inventory inventory = player.getInventory();
        List<Integer> containerSlots = new ArrayList<>();
        List<Integer> inventorySlots = new ArrayList<>();

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container == inventory) {
                int c = slot.getContainerSlot();
                if (c >= 9 && c <= 35) { // Hauptinventar, ohne Hotbar/Ruestung/Zweithand
                    inventorySlots.add(i);
                }
            } else if (storage) {
                containerSlots.add(i);
            }
        }

        if (storage) {
            sortSlots(mc, menu, containerSlots);
        }
        sortSlots(mc, menu, inventorySlots);
    }

    private static void sortSlots(Minecraft mc, AbstractContainerMenu menu, List<Integer> ids) {
        mergeStacks(mc, menu, ids);
        orderStacks(mc, menu, ids);
    }

    /** Schritt 1: angebrochene Stapel gleicher Items zusammenlegen. */
    private static void mergeStacks(Minecraft mc, AbstractContainerMenu menu, List<Integer> ids) {
        for (int a = 0; a < ids.size(); a++) {
            int slotA = ids.get(a);
            for (int b = a + 1; b < ids.size(); b++) {
                ItemStack stackA = stackAt(menu, slotA);
                if (stackA.isEmpty() || stackA.getCount() >= stackA.getMaxStackSize()) {
                    break;
                }
                int slotB = ids.get(b);
                ItemStack stackB = stackAt(menu, slotB);
                if (stackB.isEmpty() || !ItemStack.isSameItemSameComponents(stackA, stackB)) {
                    continue;
                }
                click(mc, menu, slotB);                 // B aufnehmen
                click(mc, menu, slotA);                 // auf A ablegen (fuellt A auf)
                if (!menu.getCarried().isEmpty()) {
                    click(mc, menu, slotB);             // Rest zurueck nach B
                }
                if (!menu.getCarried().isEmpty()) {
                    return;                             // Sicherheitsnetz: nichts Riskantes tun
                }
            }
        }
    }

    /** Schritt 2: Reihenfolge herstellen (Tausch ueber je drei Klicks). */
    private static void orderStacks(Minecraft mc, AbstractContainerMenu menu, List<Integer> ids) {
        List<ItemStack> target = new ArrayList<>();
        for (int id : ids) {
            ItemStack s = stackAt(menu, id);
            if (!s.isEmpty()) {
                target.add(s.copy());
            }
        }
        target.sort(ORDER); // stabil

        for (int pos = 0; pos < target.size(); pos++) {
            int slotPos = ids.get(pos);
            ItemStack want = target.get(pos);
            ItemStack have = stackAt(menu, slotPos);
            if (!have.isEmpty() && ItemStack.isSameItemSameComponents(have, want)) {
                continue;
            }
            for (int k = pos + 1; k < ids.size(); k++) {
                int slotK = ids.get(k);
                ItemStack other = stackAt(menu, slotK);
                if (!other.isEmpty() && ItemStack.isSameItemSameComponents(other, want)) {
                    if (have.isEmpty()) {
                        click(mc, menu, slotK);
                        click(mc, menu, slotPos);
                    } else {
                        click(mc, menu, slotPos);
                        click(mc, menu, slotK);
                        click(mc, menu, slotPos);
                    }
                    break;
                }
            }
        }
    }

    private static ItemStack stackAt(AbstractContainerMenu menu, int slotId) {
        return menu.slots.get(slotId).getItem();
    }

    private static void click(Minecraft mc, AbstractContainerMenu menu, int slotId) {
        mc.gameMode.handleContainerInput(menu.containerId, slotId, 0, ContainerInput.PICKUP, mc.player);
    }
}
