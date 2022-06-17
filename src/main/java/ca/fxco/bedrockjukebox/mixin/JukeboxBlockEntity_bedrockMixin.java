package ca.fxco.bedrockjukebox.mixin;

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
public abstract class JukeboxBlockEntity_bedrockMixin extends BlockEntity implements Inventory, SidedInventory {

    @Shadow
    private ItemStack record;

    @Shadow
    public abstract void setRecord(ItemStack stack);

    @Shadow
    private boolean field_39484;

    private static final int[] MAIN_SLOT = new int[]{0};

    private final JukeboxBlockEntity self = (JukeboxBlockEntity)(Object)this;

    public JukeboxBlockEntity_bedrockMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static void updateState(JukeboxBlockEntity jukeboxBlockEntity) {
        World world = jukeboxBlockEntity.getWorld();
        if (world == null) return;
        BlockPos pos = jukeboxBlockEntity.getPos();
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;
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
            ItemStack stack = this.record.copy();
            this.record = ItemStack.EMPTY;
            updateState(self);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot == 0) {
            this.field_39484 = true;
            this.setRecord(stack);
            updateState(self);
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
        this.setRecord(ItemStack.EMPTY);
        updateState(self);
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
