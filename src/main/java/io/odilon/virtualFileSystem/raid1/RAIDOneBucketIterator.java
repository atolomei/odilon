/*
 * Odilon Object Storage
 * (C) Novamens 
 *
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
package io.odilon.virtualFileSystem.raid1;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.odilon.errors.InternalCriticalException;
import io.odilon.log.Logger;
import io.odilon.model.ServerConstant;
import io.odilon.virtualFileSystem.model.BucketIterator;
import io.odilon.virtualFileSystem.model.Drive;
import io.odilon.virtualFileSystem.model.ServerBucket;

/**
 * <p>
 * <b>RAID 1</b> <br/>
 * Data is replicated and all Drives have all files
 * </p>
 * Drives that are in status NOT_SYNC are being synced un background. All new
 * Objects (since the Drive started the sync process) are replicated in the
 * NOT_SYNC drive while the drive is being synced.
 *
 * <p>
 * The RAID 1 {@link BucketIterator} uses a randomly selected Drive (among the
 * drives in status DriveStatus.ENABLED) to walk and return ObjectMetaata files
 * </p>
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class RAIDOneBucketIterator extends BucketIterator implements Closeable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(RAIDOneBucketIterator.class.getName());

    @JsonProperty("drive")
    private final Drive drive;

    @JsonIgnore
    private Iterator<Path> it;

    @JsonIgnore
    private Stream<Path> stream;

    public RAIDOneBucketIterator(RAIDOneDriver driver, ServerBucket bucket, Optional<Long> opOffset,
            Optional<String> opPrefix) {
        super(driver, bucket);

        opPrefix.ifPresent(x -> setPrefix(x.toLowerCase().trim()));
        opOffset.ifPresent(x -> setOffset(x));

        this.drive = getDriver().getDrivesEnabled().get(
                Double.valueOf(Math.abs(Math.random() * 10000)).intValue() % getDriver().getDrivesEnabled().size());
    }

    /**
     * 
     * 
     */
    @Override
    public synchronized void close() throws IOException {
        if (this.stream != null)
            this.stream.close();
    }

    /**
     * @return false if there are no more items
     */
    protected boolean fetch() {
        setRelativeIndex(0);
        setBuffer(new ArrayList<Path>());

        boolean isItems = true;
        while (isItems && getBuffer().size() < ServerConstant.BUCKET_ITERATOR_DEFAULT_BUFFER_SIZE) {
            if (it.hasNext())
                getBuffer().add(it.next());
            else
                isItems = false;
        }
        return !getBuffer().isEmpty();
    }

    @Override
    protected void init() {

        Path start = new File(getDrive().getBucketMetadataDirPath(getBucketId())).toPath();
        try {
            this.stream = Files.walk(start, 1).skip(1).filter(file -> Files.isDirectory(file))
                    .filter(file -> (getPrefix() == null)
                            || file.getFileName().toString().toLowerCase().startsWith(getPrefix()));
            // filter(file -> isObjectStateEnabled(file));

        } catch (IOException e) {
            throw new InternalCriticalException(e, "Files.walk ...");
        }
        this.it = this.stream.iterator();
        skipOffset();
        setInitiated(true);
    }

    private void skipOffset() {
        if (getOffset() == 0)
            return;
        boolean isItems = true;
        int skipped = 0;
        while (isItems && skipped < getOffset()) {
            if (this.it.hasNext()) {
                this.it.next();
                skipped++;
            } else {
                break;
            }
        }
    }

    private Drive getDrive() {
        return this.drive;
    }

}
