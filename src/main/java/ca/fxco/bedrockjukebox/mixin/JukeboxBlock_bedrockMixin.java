package ca.fxco.bedrockjukebox.mixin;

import ca.fxco.bedrockjukebox.helpers.ExtendedJukebox;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.*;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlock_bedrockMixin extends BlockWithEntity {

    protected JukeboxBlock_bedrockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof JukeboxBlockEntity jbbe && ((ExtendedJukebox)jbbe).isPlaying())
            return 15;
        return 0;
    }


    @ModifyConstant(
            method = "*",
            constant = @Constant(intValue = 2)
    )
    private int addBlockUpdates(int original) {
        return 3;
    }


    @Redirect(
            method = "getTicker",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;get(Lnet/minecraft/state/property/Property;)" +
                            "Ljava/lang/Comparable;"
            )
    )
    private Comparable alwaysTick(BlockState instance, Property property) {
        return Boolean.TRUE; //Needed to process inventory change updates
    }
}
