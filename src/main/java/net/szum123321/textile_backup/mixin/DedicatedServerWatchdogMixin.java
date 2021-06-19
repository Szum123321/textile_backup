package net.szum123321.textile_backup.mixin;

import net.minecraft.server.dedicated.DedicatedServerWatchdog;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.Statics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DedicatedServerWatchdog.class)
public class DedicatedServerWatchdogMixin {

    @ModifyVariable(method = "run()V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"), ordinal = 0, name = "l")
    private long redirectedCall(long original) {
        return Statics.disableWatchdog ? Util.getMeasuringTimeMs() : original;
    }
}
