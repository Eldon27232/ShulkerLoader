package com.fexl.shulkerloader.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ShulkerLoad {

	@Inject(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z", ordinal = 0), cancellable = true)
	private void itemPickupEvent(PlayerEntity player, CallbackInfo event) {
		ItemEntity itemEntity = (ItemEntity)(Object) this;
		PlayerInventory playerInv = player.getInventory();
		ItemStack offhand_item = player.getOffHandStack();
		ItemStack pickup_item = itemEntity.getStack();

		//Check the player is carrying a shulker box, the item picked up is NOT a shulker box, the shulker box has a stack size of one, and the shulker box contains an inventory.
		if(!isShulkerBox(offhand_item) || isShulkerBox(pickup_item) || offhand_item.getCount() > 1 || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		
		//Stores the shulker box contents for processing
		DefaultedList<ItemStack> shulkerInv = DefaultedList.ofSize(ShulkerBoxBlockEntity.INVENTORY_SIZE, ItemStack.EMPTY);
		offhand_item.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(shulkerInv);

		//Count after processing
		int processed_count = checkSlots(pickup_item, shulkerInv);
		
		//Count before processing
		int previous_count = pickup_item.copy().getCount();
		
		//If at least one quantity of the pickup item wasn't deposited into the offhand shulker box
		if(!(previous_count > processed_count)) {
			return;
		}

		//Set the count of the pickup item
		pickup_item.setCount(processed_count);

		//Let the player's inventory handle any item quantity that did not fit into the shulker.
		playerInv.insertStack(pickup_item);

		//Retrieve the amount of items the player picked up
		ItemStack pickup_amount = pickup_item.copy();
		pickup_amount.setCount(previous_count - pickup_item.getCount());
		
		//Cancel the event so it isn't also processed by the inventory
		event.cancel();
		
		//Shows the player pickup animation
		serverPlayer.sendPickup(itemEntity, pickup_amount.getCount());
		
		if(pickup_item.getCount() == 0) { 
			//Remove the ItemEntity
			itemEntity.discard();
		}

		//Grant advancements associated with picking up the item
		Criteria.INVENTORY_CHANGED.trigger(serverPlayer, playerInv, pickup_amount);

		//Update player statistics with the item
		player.increaseStat(Stats.PICKED_UP.getOrCreateStat(pickup_item.getItem()), pickup_amount.getCount());
		serverPlayer.triggerItemPickedUpByEntityCriteria(itemEntity);

		//Set the shulker box tag equal to the new one
		offhand_item.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(shulkerInv));
	}
	
	//Ported from Inventory.class
	private int getFreeSlot(DefaultedList<ItemStack> items) {
		for(int i = 0; i < items.size(); ++i) {
			if (items.get(i).isEmpty()) {
				return i;
		    }
		}

		return -1;
	}
		
	//Ported from Inventory.class
	private boolean hasRemainingSpaceForItem(ItemStack item1, ItemStack item2) {
	      return !item1.isEmpty() && ItemStack.areItemsAndComponentsEqual(item1, item2) && item1.isStackable() && item1.getCount() < item1.getMaxCount() && item1.getCount() < 64;
	}
		   
	//Ported from Inventory.class
	private int getSlotWithRemainingSpace(ItemStack item, DefaultedList<ItemStack> items) {
		for(int i = 0; i < items.size(); ++i) {
			if (this.hasRemainingSpaceForItem(items.get(i), item)) {
				return i;
	        }
		}
	    return -1;
	}
	
	//Distribute an ItemStack into the components of an ItemStack list and return the remainder
	private int checkSlots(ItemStack item, DefaultedList<ItemStack> items) {
		//True if the stack has been fully transferred into items
		boolean item_transferred = false;
		
		//Copy item for processing
		ItemStack pickup_item_copy = item.copy();
		
		//Get the next avaliable slot that has remaining space for item
		int stack_avaliable = this.getSlotWithRemainingSpace(pickup_item_copy, items);
		
		//Iterate until no more stacks with remaining space are available
		while(stack_avaliable != -1) {
			ItemStack shulker_item = items.get(stack_avaliable).copy();
			int combined_stack = shulker_item.getCount() + pickup_item_copy.getCount();
			
			//If pickup item fits in selected stack
			if(combined_stack <= shulker_item.getMaxCount()) {
				shulker_item.setCount(combined_stack);
				pickup_item_copy.setCount(0);
				items.set(stack_avaliable, shulker_item);
				item_transferred = true;
				break;
			}
			//How much stack capacity is left
			int stack_left = shulker_item.getMaxCount() - shulker_item.getCount();
			
			//If pickup item doesn't fit in selected stack
			if(pickup_item_copy.getCount() > stack_left) {
				pickup_item_copy.setCount(pickup_item_copy.getCount()-stack_left);
				shulker_item.setCount(shulker_item.getMaxCount());
				items.set(stack_avaliable, shulker_item);
				stack_avaliable = this.getSlotWithRemainingSpace(pickup_item_copy, items);
				continue;
			}	
		}

		//Check if the shulker has any free slots
		int slot_avaliable = getFreeSlot(items);
		if(slot_avaliable != -1 && !item_transferred) {
			items.set(slot_avaliable, pickup_item_copy.copy());
			pickup_item_copy.setCount(0);
		}
		
		//Return the remainder of item
		return pickup_item_copy.getCount();
	}
	//Overloaded with ItemStack
	private boolean isShulkerBox(ItemStack item) {
		return this.isShulkerBox(item.getItem());
	}

	//Check if shulker
	private boolean isShulkerBox(Item item) {
		return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
	}
}
