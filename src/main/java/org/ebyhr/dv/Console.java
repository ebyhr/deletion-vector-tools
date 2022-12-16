/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ebyhr.dv;

import io.trino.filesystem.TrinoInputFile;
import io.trino.filesystem.local.LocalInputFile;
import io.trino.plugin.deltalake.delete.RoaringBitmapArray;
import org.roaringbitmap.RoaringBitmap;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;
import static io.trino.plugin.deltalake.delete.DeletionVectors.readDeletionVector;
import static java.lang.Math.toIntExact;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

@Command(
        name = "dv",
        header = "Deletion Vector command line interface",
        synopsisHeading = "%nUSAGE:%n%n",
        optionListHeading = "%nOPTIONS:%n",
        usageHelpAutoWidth = true)
public class Console
        implements Callable<Integer>
{
    private static final int PORTABLE_ROARING_BITMAP_MAGIC_NUMBER = 1681511377;

    @Option(names = "--path", paramLabel = "<path>", description = "File path to Deletion Vector")
    public String path;

    @Option(names = "--offset", paramLabel = "<offset>", description = "File offset")
    public int offset;

    @Option(names = "--size", paramLabel = "<size>", description = "Size in bytes")
    public int size;

    @Override
    public Integer call()
    {
        return run() ? 0 : 1;
    }

    public boolean run()
    {
        try {
            TrinoInputFile file = new LocalInputFile(Paths.get(path).toFile());
            ByteBuffer byteBuffer = readDeletionVector(file, offset, size);
            RoaringBitmapArray roaringBitmaps = deserializeDeletionVectors(byteBuffer);
            for (int i = 0; i < roaringBitmaps.length(); i++) {
                RoaringBitmap bitmap = roaringBitmaps.get(i);
                System.out.println(i + ": " + bitmap);
            }
        }
        catch (RuntimeException | IOException e) {
            return false;
        }
        return true;
    }

    private static RoaringBitmapArray deserializeDeletionVectors(ByteBuffer buffer)
            throws IOException
    {
        checkArgument(buffer.order() == LITTLE_ENDIAN, "Byte order must be little endian: %s", buffer.order());
        int magicNumber = buffer.getInt();
        if (magicNumber == PORTABLE_ROARING_BITMAP_MAGIC_NUMBER) {
            int size = toIntExact(buffer.getLong());
            RoaringBitmapArray bitmaps = new RoaringBitmapArray();
            for (int i = 0; i < size; i++) {
                int key = buffer.getInt();
                checkArgument(key >= 0, "key must not be negative: %s", key);

                RoaringBitmap bitmap = new RoaringBitmap();
                bitmap.deserialize(buffer);
                bitmap.stream().forEach(bitmaps::add);

                int consumedBytes = bitmap.serializedSizeInBytes();
                buffer.position(buffer.position() + consumedBytes);
            }
            return bitmaps;
        }
        throw new IllegalArgumentException("Unsupported magic number: " + magicNumber);
    }
}
