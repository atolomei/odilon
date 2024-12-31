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
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import io.odilon.errors.InternalCriticalException;
import io.odilon.log.Logger;
import io.odilon.model.SharedConstant;
import io.odilon.virtualFileSystem.model.OperationCode;
import io.odilon.virtualFileSystem.model.ServerBucket;
import io.odilon.virtualFileSystem.model.VirtualFileSystemOperation;

/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
public class RAIDZeroRollbackDeleteHandler extends RAIDZeroRollbackHandler {

    private static Logger logger = Logger.getLogger(RAIDZeroRollbackDeleteHandler.class.getName());

    public RAIDZeroRollbackDeleteHandler(RAIDZeroDriver driver, VirtualFileSystemOperation operation, boolean recoveryMode) {
        super(driver, operation, recoveryMode);
    }

    @Override
    protected void rollback() {

        boolean done = false;

        try {
            /** Rollback is the same for both operations -> DELETE_OBJECT and
             DELETE_OBJECT_PREVIOUS_VERSIONS */
            if ((getOperation().getOperationCode() == OperationCode.DELETE_OBJECT) ||
                (getOperation().getOperationCode() == OperationCode.DELETE_OBJECT_PREVIOUS_VERSIONS))
                restore();
            done = true;

        } catch (InternalCriticalException e) {
            if (!isRecovery())
                throw (e);
            else
                logger.error(opInfo(getOperation()), SharedConstant.NOT_THROWN);

        } catch (Exception e) {
            if (!isRecovery())
                throw new InternalCriticalException(e, opInfo(getOperation()));
            else
                logger.error(opInfo(getOperation()), SharedConstant.NOT_THROWN);
        } finally {
            if (done || isRecovery())
                getOperation().cancel();
        }

    }

    /**
     * restore metadata directory
     * 
     * @param bucketName
     * @param objectName
     */
    private void restore() {

        ServerBucket bucket = getBucketCache().get(getOperation().getBucketId());
        String objectName = getOperation().getObjectName();
        
        String objectMetadataBackupDirPath = getDriver().getWriteDrive(bucket, objectName).getBucketWorkDirPath(bucket)
                + File.separator + objectName;
        String objectMetadataDirPath = getDriver().getWriteDrive(bucket, objectName).getObjectMetadataDirPath(bucket, objectName);
        try {
            FileUtils.copyDirectory(new File(objectMetadataBackupDirPath), new File(objectMetadataDirPath));
        } catch (InternalCriticalException e) {
            throw e;
        } catch (IOException e) {
            throw new InternalCriticalException(e, objectInfo(bucket, objectName));
        }
    }
}
