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
package io.odilon.vfs;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.odilon.log.Logger;
import io.odilon.model.RedundancyLevel;
import io.odilon.vfs.model.JournalService;
import io.odilon.vfs.model.VFSOperation;
import io.odilon.vfs.model.VFSop;

/**
 *
 */
public class ODVFSOperation implements VFSOperation {
				
	static private Logger logger = Logger.getLogger(ODVFSOperation.class.getName());

	@JsonIgnore
	static private ObjectMapper mapper = new ObjectMapper();
	
	static  {
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	@JsonIgnore
	static final private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.XXX-z", Locale.ENGLISH);
	
	@JsonIgnore
	private JournalService journalService;
	
	@JsonProperty("id")
	private String id;

	@JsonProperty("version")
	private int version;
	
	@JsonProperty("bucketName")
	private String bucketName;

	@JsonProperty("objectName")
	private String objectName;
	
	@JsonProperty("timestamp")
	private OffsetDateTime timestamp;
	
	@JsonProperty("operation")
	private VFSop op;
	
	@JsonProperty("raid")
	private RedundancyLevel raid;
	
	public ODVFSOperation() {
	}
	
	@Override
	public String getUUID() {

		return  	op.getEntityGroupCode() + ":"  +
					((bucketName!=null) ? bucketName :"null" ) + ":" + 
					((objectName!=null) ? objectName :"null" );
	}
	
	public ODVFSOperation(	String id, 
							VFSop op,  
							Optional<String> bucketName,
							Optional<String> objectName,
							Optional<Integer> iVersion,
							RedundancyLevel raid, 
							JournalService journalService) {
	
		this.id = id;
		this.op = op;
		
		if (iVersion.isPresent())
			version= iVersion.get().intValue();
			
		if (bucketName.isPresent())
			this.bucketName = bucketName.get();
		
		if (objectName.isPresent())
			this.objectName = objectName.get();
		this.raid =raid;
		this.journalService = journalService;
		this.timestamp = OffsetDateTime.now();  
	}

	@Override
	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	@Override
	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public int getVersion() {
		return this.version;
	}
	
	protected void setTimestamp(OffsetDateTime date) {
		this.timestamp=date;
	}

	protected void setRedundancyLevel(RedundancyLevel level) {
		this.raid=level;
	}

	@Override
	public RedundancyLevel getRedundancyLevel() {
		return this.raid;
	}

	@Override
	public boolean equals(Object o) {
		
		if (o instanceof ODVFSOperation) {
			String oid =((ODVFSOperation) o).getId();
			if (this.id==null) 
				return oid==null;
			if (oid==null)
				return false;
			return this.id.equals(oid);
		}
		return false;
		
	}
	
	@Override
	public String toJSON() {
		 try {
			return mapper.writeValueAsString(this);
		 } catch (JsonProcessingException e) {
					logger.error(e);
					return "\"error\":\"" + e.getClass().getName()+"\""; 
		}
	  }

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.getClass().getSimpleName());
			str.append(toJSON());
		return str.toString();
	}
	
	
	@Override
	public OffsetDateTime getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public VFSop getOp() {
		return op;
	}

	public void setOp(VFSop op) {
		this.op = op;
	}

	@Override
	public boolean commit() {
		return this.journalService.commit(this);
	}

	@Override
	public boolean cancel() {
		return this.journalService.cancel(this);
	}

	public void setJournalService(JournalService journalService) {
		this.journalService=journalService;
	}


}
