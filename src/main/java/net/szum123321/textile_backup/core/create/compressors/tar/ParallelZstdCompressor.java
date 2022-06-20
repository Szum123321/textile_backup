package net.szum123321.textile_backup.core.create.compressors.tar;

import com.github.luben.zstd.RecyclingBufferPool;
import com.github.luben.zstd.ZstdOutputStream;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.create.BackupContext;

import java.io.IOException;
import java.io.OutputStream;

public class ParallelZstdCompressor extends AbstractTarArchiver {
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    public static ParallelZstdCompressor getInstance() {
        return new ParallelZstdCompressor();
    }

    @Override
    protected OutputStream getCompressorOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) throws IOException {
        var out = new ZstdOutputStream(stream, RecyclingBufferPool.INSTANCE, config.get().compression);
        out.setWorkers(coreLimit);
        out.setChecksum(true);
        return out;
    }
}
