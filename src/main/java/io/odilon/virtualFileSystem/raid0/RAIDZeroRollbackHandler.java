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

import com.fasterxml.jackson.annotation.JsonProperty;

import io.odilon.virtualFileSystem.model.VirtualFileSystemOperation;

/**
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
public abstract class RAIDZeroRollbackHandler extends RAIDZeroHandler {

    @JsonProperty("operation")
    final private VirtualFileSystemOperation operation;
    
    @JsonProperty("recovery")
    final private boolean recovery;

    public RAIDZeroRollbackHandler(RAIDZeroDriver driver, VirtualFileSystemOperation operation, boolean recovery) {
        super(driver);

        this.operation = operation;
        this.recovery = recovery;
    }

    protected VirtualFileSystemOperation getOperation() {
        return this.operation;
    }

    protected boolean isRecovery() {
        return this.recovery;
    }

    protected String info() {
        return opInfo(getOperation());
    }
    
    protected abstract void rollback();
}
