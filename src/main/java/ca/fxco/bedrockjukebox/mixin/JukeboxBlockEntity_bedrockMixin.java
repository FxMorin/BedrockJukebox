package ca.fxco.bedrockjukebox.mixin;

import ca.fxco.bedrockjukebox.helpers.ExtendedJukebox;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.block.JukeboxBlock.HAS_RECORD;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntity_bedrockMixin extends BlockEntity
        implements Inventory, SidedInventory, ExtendedJukebox {

    @Shadow
    private ItemStack record;

    @Shadow
    public abstract void setRecord(ItemStack stack);

    @Shadow
    private boolean field_39484;

    private static final int[] MAIN_SLOT = new int[]{0};

    // No inventory change update in the game yet (That passes world, pos, etc...)
    // Might in the future use the block entity to do the actions tho, so it mostly contains the right things
    private boolean needsUpdate = false;

    public JukeboxBlockEntity_bedrockMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isPlaying() {
        return this.field_39484;
    }

    @Override
    public boolean update() {
        boolean needsUpdate = this.needsUpdate;
        this.needsUpdate = false;
        return needsUpdate;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return MAIN_SLOT;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.isIn(ItemTags.MUSIC_DISCS);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return dir == Direction.DOWN && !this.field_39484; //not isPlaying
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.record == ItemStack.EMPTY;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot == 0 ? this.record : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return removeStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot == 0) {
            this.field_39484 = false;
            this.needsUpdate = true;
            ItemStack stack = this.record.copy();
            this.record = ItemStack.EMPTY;
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot == 0) {
            this.field_39484 = true;
            this.needsUpdate = true;
            this.setRecord(stack);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world.getBlockEntity(this.pos) != this) return false;
        return player.getBlockPos().getSquaredDistance(this.pos.getX(),this.pos.getY(),this.pos.getZ()) <= 64.0;
    }

    @Override
    public void clear() {
        this.field_39484 = false;
        this.needsUpdate = true;
        this.setRecord(ItemStack.EMPTY);
    }


    @Inject(
            method = "method_44370(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;" +
                    "Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/JukeboxBlockEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void beforeTick(
            World world, BlockPos pos, BlockState state, JukeboxBlockEntity jukeboxBlockEntity, CallbackInfo ci
    ) {
        if (((ExtendedJukebox)jukeboxBlockEntity).update()) {
            ((JukeboxBlockEntityAccessor)jukeboxBlockEntity).setRecordStartTick(
                    ((JukeboxBlockEntityAccessor)jukeboxBlockEntity).getTickCount()
            );
            ItemStack stack = jukeboxBlockEntity.getRecord();
            if (stack == ItemStack.EMPTY) {
                if (state.get(HAS_RECORD)) {
                    state = state.with(HAS_RECORD, false);
                    world.setBlockState(pos, state, Block.NOTIFY_ALL);
                    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
                    world.syncWorldEvent(WorldEvents.MUSIC_DISC_PLAYED, pos, 0);
                }
            } else {
                if (!state.get(HAS_RECORD)) {
                    state = state.with(HAS_RECORD, true);
                    world.setBlockState(pos, state, Block.NOTIFY_ALL);
                    world.updateNeighbors(pos,state.getBlock());
                    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
                    world.syncWorldEvent(WorldEvents.MUSIC_DISC_PLAYED, pos, Item.getRawId(stack.getItem()));
                }
            }
            ci.cancel();
        } else if (!state.get(HAS_RECORD)) {
            ci.cancel();
        }
    }


    @Inject(
            method = "method_44370(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;" +
                    "Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/JukeboxBlockEntity;)V",
            at = @At(
                value = "FIELD",
                target = "Lnet/minecraft/block/entity/JukeboxBlockEntity;field_39484:Z",
                    shift = At.Shift.AFTER
            )
    )
    private static void stopPlaying(
            World world, BlockPos pos, BlockState state, JukeboxBlockEntity jukeboxBlockEntity, CallbackInfo ci
    ) {
        world.setBlockState(pos, state.with(HAS_RECORD, false), Block.NOTIFY_ALL);
    }
}
