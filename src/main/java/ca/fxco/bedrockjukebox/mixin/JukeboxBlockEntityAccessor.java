package ca.fxco.bedrockjukebox.mixin;

import net.minecraft.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(JukeboxBlockEntity.class)
public interface JukeboxBlockEntityAccessor {
    @Accessor("field_39482")
    long getTickCount();

    @Accessor("field_39483")
    void setRecordStartTick(long recordStartTick);

}
