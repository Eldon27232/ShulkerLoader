package com.fexl.shulkerloader.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.Objects;

@Mixin(ItemEntity.class)
public class ShulkerLoad {

	@Inject(method = "Lnet/minecraft/world/entity/item/ItemEntity;playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z", ordinal = 0), cancellable = true)
	private void itemPickupEvent(Player player, CallbackInfo event) {
		ItemEntity itemEntity = (ItemEntity)(Object) this;
		Inventory playerInv = player.getInventory();
		ItemStack offhand_item = playerInv.offhand.get(0);
		ItemStack pickup_item = itemEntity.getItem();

		//Check the player is carrying a shulker box, the item picked up is NOT a shulker box, the shulker box has a stack size of one, and the shulker box contains an inventory.
		if(!isShulkerBox(offhand_item) || isShulkerBox(pickup_item) || !offhand_item.getComponents().keySet().contains(DataComponents.CONTAINER) || offhand_item.getCount() > 1) {
			return;
		}
		
		//Stores the shulker box contents for processing
		NonNullList<ItemStack> shulkerInv = NonNullList.withSize((new ShulkerBoxBlockEntity(BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState())).getContainerSize(), ItemStack.EMPTY);
		offhand_item.getComponents().get(DataComponents.CONTAINER).copyInto(shulkerInv);

		//Count after processing
		int processed_count = checkSlots(pickup_item, shulkerInv);
		
		//Count before processing
		int previous_count = pickup_item.copy().getCount();
		
		//If at least one quantity of the pickup item wasn't deposited into the offhand shulker box
		if(!(previous_count > processed_count)) {
			return;
		}

		//Retrieve the amount of items the player picked up
		ItemStack pickup_amount = pickup_item.copy();
		pickup_amount.setCount(previous_count - processed_count);
		
		//Grant advancements associated with picking up the item
		CriteriaTriggers.INVENTORY_CHANGED.trigger((ServerPlayer)player, playerInv, pickup_amount);
				
		//Update player statistics with the item
		player.awardStat(Stats.ITEM_PICKED_UP.get(pickup_item.getItem()), pickup_amount.getCount());
		
		//Set the count of the pickup item
		pickup_item.setCount(processed_count);
		
		//Cancel the event so it isn't also processed by the inventory
		event.cancel();
		
		//Shows the player pickup animation
		player.take(itemEntity, pickup_amount.getCount());
		
		if(pickup_item.getCount() == 0) { 
			//Kill the ItemEntity
			itemEntity.kill();
		}

		//Set the shulker box tag equal to the new one
		offhand_item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(shulkerInv));
	}
	
	//Ported from Inventory.class
	private int getFreeSlot(NonNullList<ItemStack> items) {
		for(int i = 0; i < items.size(); ++i) {
			if (items.get(i).isEmpty()) {
				return i;
		    }
		}

		return -1;
	}
		
	//Ported from Inventory.class
	private boolean hasRemainingSpaceForItem(ItemStack item1, ItemStack item2) {
	      return !item1.isEmpty() && ItemStack.isSameItemSameComponents(item1, item2) && item1.isStackable() && item1.getCount() < item1.getMaxStackSize() && item1.getCount() < 64;
	}
		   
	//Ported from Inventory.class
	private int getSlotWithRemainingSpace(ItemStack item, NonNullList<ItemStack> items) {
		for(int i = 0; i < items.size(); ++i) {
			if (this.hasRemainingSpaceForItem(items.get(i), item)) {
				return i;
	        }
		}
	    return -1;
	}
	
	//Distribute an ItemStack into the components of an ItemStack list and return the remainder
	private int checkSlots(ItemStack item, NonNullList<ItemStack> items) {
		//True if the stack has been fully transferred into items
		Boolean item_transferred = false;
		
		//Copy item for processing
		ItemStack pickup_item_copy = item.copy();
		
		//Get the next avaliable slot that has remaining space for item
		int stack_avaliable = this.getSlotWithRemainingSpace(pickup_item_copy, items);
		
		//Iterate until no more stacks with remaining space are available
		while(stack_avaliable != -1) {
			ItemStack shulker_item = items.get(stack_avaliable).copy();
			int combined_stack = shulker_item.getCount() + pickup_item_copy.getCount();
			
			//If pickup item fits in selected stack
			if(combined_stack <= shulker_item.getMaxStackSize()) {
				shulker_item.setCount(combined_stack);
				pickup_item_copy.setCount(0);
				items.set(stack_avaliable, shulker_item);
				item_transferred = true;
				break;
			}
			//How much stack capacity is left
			int stack_left = shulker_item.getMaxStackSize() - shulker_item.getCount();
			
			//If pickup item doesn't fit in selected stack
			if(pickup_item_copy.getCount() > stack_left) {
				pickup_item_copy.setCount(pickup_item_copy.getCount()-stack_left);
				shulker_item.setCount(shulker_item.getMaxStackSize());
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
	private Boolean isShulkerBox(ItemStack item) {
		return this.isShulkerBox(item.getItem());
	}

	//Check if shulker
	private Boolean isShulkerBox(Item item) {
		//Get item registry name without namespace
		String item_name = BuiltInRegistries.ITEM.getKey(item).getPath();

		if (item_name.endsWith("shulker_box"))
			return true;
		return false;
	}
}
