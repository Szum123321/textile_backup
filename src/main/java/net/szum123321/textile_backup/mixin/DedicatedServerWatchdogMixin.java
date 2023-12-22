/*
 * A simple backup mod for Fabric
 * Copyright (C)  2022   Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.mixin;

import net.minecraft.server.dedicated.DedicatedServerWatchdog;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.Globals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * This mixin should numb Watchdog while a backup runs.
 * If works as intended solves issues with watchdog errors
 */
@Mixin(DedicatedServerWatchdog.class)
public class DedicatedServerWatchdogMixin {

    @ModifyVariable(method = "run()V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/Util;getMeasuringTimeNano()J"), ordinal = 0, name = "l")
    private long redirectedCall(long original) {
        return Globals.INSTANCE.disableWatchdog ? Util.getMeasuringTimeNano() : original;
    }
}
