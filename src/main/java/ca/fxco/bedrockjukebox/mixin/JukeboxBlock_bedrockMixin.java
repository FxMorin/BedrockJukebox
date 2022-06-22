package ca.fxco.bedrockjukebox.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import static net.minecraft.block.JukeboxBlock.HAS_RECORD;

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
        if (blockEntity instanceof JukeboxBlockEntity jbbe && ((JukeboxBlockEntityAccessor)jbbe).isPlaying())
            return 15;
        return 0;
    }

    @Override
    public PistonBehavior getPistonBehavior(BlockState state) {
        return state.get(HAS_RECORD) ? PistonBehavior.BLOCK : PistonBehavior.NORMAL;
    }


    @ModifyConstant(
            method = "*",
            constant = @Constant(intValue = 2)
    )
    private int addBlockUpdates(int original) {
        return 3;
    }
}
