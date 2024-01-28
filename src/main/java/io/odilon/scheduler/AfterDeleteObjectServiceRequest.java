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
package io.odilon.scheduler;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.odilon.log.Logger;
import io.odilon.model.ServerConstant;
import io.odilon.vfs.model.VFSop;
import io.odilon.vfs.model.VirtualFileSystemService;

/**
 * 
 * 
 * <p>ServiceRequest executed Async after a {@code VFSop.DELETE_OBJECT} or {@code VFSop.DELETE_OBJECT_PREVIOUS_VERSIONS}</p>  
 * 
 * 
 * <h3>VFSop.DELETE_OBJECT</h3>
 * <p>Cleans up all previous versions of an {@link VFSObject} (ObjectMetadata and Data).<br/>
 * Delete backup directory. <br/>
 * This request is executed Async after the delete transaction commited.</p>
 *	 
 * <h3>VFSop.DELETE_OBJECT_PREVIOUS_VERSIONS</h3>
 * <p>Cleans up all previous versions of an {@link VFSObject} (ObjectMetadata and Data), but keeps the head version.<br/>
 * Delete backup directory. <br/>
 * This request is executed Async after the delete transaction commited.</p>
 *
 * <h3>RETRIES</h3>
 * <p>if the request can not complete due to serious system issue, the request is discarded after 5 attemps. 
 * The clean up process will be executed after next system startup</p>
 * 
 * @see {@link RAIDZeroDeleteObjectHandler}, {@link RAIDOneDeleteObjectHandler} 
  */
@Component
@Scope("prototype")
@JsonTypeName("afterDeleteObject")
public class AfterDeleteObjectServiceRequest extends AbstractServiceRequest implements StandardServiceRequest {

	static private Logger logger = Logger.getLogger(AfterDeleteObjectServiceRequest.class.getName());
	
	private static final long serialVersionUID = 1L;

	@JsonProperty("bucketName")
	String bucketName;
	
	@JsonProperty("objectName")
	String objectName;
	
	@JsonProperty("headVersion")
	int headVersion=0;

	@JsonProperty("vfsop")
	VFSop vfsop;
	
	@JsonIgnore
	private boolean isSuccess = false;
	
	/**
	 * <p>created by the RAIDZeroDriver</p>
	 */
	protected AfterDeleteObjectServiceRequest() {
	}
	
	public AfterDeleteObjectServiceRequest(VFSop vfsop, String bucketName, String objectName, int headVersion) {
		
		this.vfsop=vfsop;
		this.bucketName=bucketName;
		this.objectName=objectName;
		this.headVersion=headVersion;
	}
	
	@Override
	public boolean isSuccess() {
		return isSuccess;
	}

	/**
	 * <p>{@link ServiceRequestExecutor} closes the Request</p>
	 */
	@Override
	public void execute() {
		try {

			setStatus(ServiceRequestStatus.RUNNING);
			clean();
			isSuccess=true;
			setStatus(ServiceRequestStatus.COMPLETED);
			
		} catch (Exception e) {
			logger.error(e, ServerConstant.NOT_THROWN);
			isSuccess=false;
			setStatus(ServiceRequestStatus.ERROR);
		}
	}

	@Override
	public String getUUID() {
		return  ((bucketName!=null) ? bucketName :"null" ) + ":" + 
				((objectName!=null) ? objectName :"null" );
	}
	
	@Override
	public boolean isObjectOperation() {
		return true;
	}
	
	@Override
	public void stop() {
		 isSuccess=true;
	}

	private void clean() {
			
		VirtualFileSystemService vfs = getApplicationContext().getBean(VirtualFileSystemService.class);
			
		if (this.vfsop==VFSop.DELETE_OBJECT)
				vfs.createVFSIODriver().postObjectDeleteTransaction(bucketName, objectName, headVersion);
			
		else if (this.vfsop==VFSop.DELETE_OBJECT_PREVIOUS_VERSIONS) 
				vfs.createVFSIODriver().postObjectPreviousVersionDeleteAllTransaction(bucketName, objectName, headVersion);
			
		else
			logger.error("Invalid Class -> " + this.vfsop.getName());
	}

}