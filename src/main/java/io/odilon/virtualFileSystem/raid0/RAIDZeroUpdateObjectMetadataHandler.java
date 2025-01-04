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
package io.odilon.virtualFileSystem.raid0;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

import io.odilon.errors.InternalCriticalException;
import io.odilon.log.Logger;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.SharedConstant;

import io.odilon.virtualFileSystem.model.ServerBucket;
import io.odilon.virtualFileSystem.model.VirtualFileSystemOperation;

/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
public class RAIDZeroUpdateObjectMetadataHandler extends RAIDZeroTransactionObjectHandler {

    private static Logger logger = Logger.getLogger(RAIDZeroUpdateObjectMetadataHandler.class.getName());

    public RAIDZeroUpdateObjectMetadataHandler(RAIDZeroDriver driver, ServerBucket bucket, String objectName) {
        super(driver, bucket, objectName);
    }

    protected void updateObjectMetadata(ObjectMetadata meta) {

        VirtualFileSystemOperation operation = null;
        boolean commitOK = false;
        boolean isMainException = false;

        objectWriteLock();
        try {

            bucketReadLock();
            try {
                checkExistsBucket();
                checkExistObject();

                /** backup existing metadata */
                FileUtils.copyDirectory(getObjectPath().metadataDirPath().toFile(),
                        getObjectPath().metadataBackupDirPath().toFile());

                operation = updateObjectMetadata(meta.getVersion());
                /** save metadata */
                Files.writeString(getObjectPath().metadataFilePath(), getObjectMapper().writeValueAsString(meta));
                commitOK = operation.commit();

            } catch (Exception e) {
                isMainException = true;
                commitOK = false;
                throw new InternalCriticalException(e, info());

            } finally {
                try {
                    if (!commitOK) {
                        try {
                            rollback(operation);
                        } catch (Exception e) {
                            if (!isMainException)
                                throw new InternalCriticalException(e, info());
                            else
                                logger.error(e, info(), SharedConstant.NOT_THROWN);
                        }
                    } else {
                        /** TODO-> Delete backup Metadata. change to Async */
                        try {
                            FileUtils.deleteQuietly(new File(
                                    getDriver().getWriteDrive(getBucket(), getObjectName()).getBucketWorkDirPath(getBucket())
                                            + File.separator + getObjectName()));
                        } catch (Exception e) {
                            logger.error(e, SharedConstant.NOT_THROWN);
                        }

                    }
                } finally {
                    bucketReadUnLock();
                }
            }
        } finally {
            objectWriteUnLock();
        }
    }

    private VirtualFileSystemOperation updateObjectMetadata(int version) {
        return getJournalService().updateObjectMetadata(getBucket(), getObjectName(), version);
    }

    /**
     * private void backup() { try { ServerBucket bucket = getBucket(); String
     * objectMetadataDirPath = getDriver().getWriteDrive(bucket,
     * getObjectName()).getObjectMetadataDirPath(bucket, getObjectName()); String
     * objectMetadataBackupDirPath = getDriver().getWriteDrive(bucket,
     * getObjectName()) .getBucketWorkDirPath(bucket) + File.separator +
     * getObjectName(); File src = new File(objectMetadataDirPath); if
     * (src.exists()) FileUtils.copyDirectory(src, new
     * File(objectMetadataBackupDirPath)); } catch (IOException e) { throw new
     * InternalCriticalException(e, info()); } }
     **/
}
