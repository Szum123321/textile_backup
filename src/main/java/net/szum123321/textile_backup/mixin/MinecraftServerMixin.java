package net.szum123321.textile_backup.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.BackupHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow private long timeReference;

    @Shadow public abstract PlayerManager getPlayerManager();

    private long lastBackup = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci){
        if(timeReference - lastBackup >= TextileBackup.config.backupInterval){
            if(getPlayerManager().getCurrentPlayerCount() == 0 && !TextileBackup.config.doBackupsOnEmptyServer)
                return;

            lastBackup = timeReference;

            BackupHelper.create((MinecraftServer)(Object)this, null, true);
        }
    }

    @Inject(method = "shutdown", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z"))
    public void onShutdown(CallbackInfo ci){
        if(TextileBackup.config.shutdownBackup)
            BackupHelper.create((MinecraftServer)(Object)this, null, false);
    }
}
