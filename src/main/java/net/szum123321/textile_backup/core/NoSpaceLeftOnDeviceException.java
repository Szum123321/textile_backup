package net.szum123321.textile_backup.core;

import java.io.IOException;

/**
 * Wrapper for specific IOException. Temporary way to get more info about issue #51
 */
public class NoSpaceLeftOnDeviceException extends IOException {
    public NoSpaceLeftOnDeviceException(Throwable cause) {
        super(cause);
    }
}
