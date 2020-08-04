/*
    A simple backup mod for Fabric
    Copyright (C) 2020  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package net.szum123321.textile_backup.mixin;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.core.LivingServer;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.BackupHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements LivingServer {
    private boolean isAlive = true;

    @Inject(method = "shutdown", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z"))
    public void onFinalWorldSave(CallbackInfo ci) {
        if (TextileBackup.CONFIG.shutdownBackup && TextileBackup.globalShutdownBackupFlag.get()) {
            TextileBackup.executorService.submit(
                    BackupHelper.create(
                            new BackupContext.Builder()
                                    .setServer((MinecraftServer) (Object) this)
                                    .setInitiator(BackupContext.BackupInitiator.Shutdown)
                                    .setComment("shutdown")
                                    .build()
                    )
            );
        }

        isAlive = false;
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }
}
