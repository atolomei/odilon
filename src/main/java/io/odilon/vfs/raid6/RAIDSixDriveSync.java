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

package io.odilon.vfs.raid6;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.odilon.log.Logger;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.ServerConstant;
import io.odilon.model.ServiceStatus;
import io.odilon.model.SharedConstant;
import io.odilon.model.list.DataList;
import io.odilon.model.list.Item;
import io.odilon.vfs.DriveInfo;
import io.odilon.vfs.model.Drive;
import io.odilon.vfs.model.DriveStatus;
import io.odilon.vfs.model.LockService;
import io.odilon.vfs.model.VFSBucket;
import io.odilon.vfs.raid1.RAIDOneDriveSync;

/**					
 * <p>Al iniciar el {@link VirtualFileSystemService} se detecta si hay uno o mas Drives nuevos.
 * Si hay al menos un Drive nuevo se corre este proceso de incorporacion 
 * del nuevo Drive. 
 * 
 * . Para los Object creados a partir del inicio del {@link VirtualFileSystemService} se utilizan los discos enabled y los not sync
 * . Para los Object anteriores al inicio del VFS se lleva a cabo la sincronizacion en el o los discos nuevos
 *   - grabar ObjectMetadata head y versiones anteriores
 *   - grabar el/los Block/s RS del data file que correspondan en cada disco nuevo   
 * 
 * Al terminar el proceso de integracion del nuevo disco, los discos pasan a status enabled.
 * </p>
 * @author atolomei@novamens.com (Alejandro Tolomei)
 */
@Component
@Scope("prototype")
public class RAIDSixDriveSync implements Runnable {

	static private Logger logger = Logger.getLogger(RAIDSixDriveSync.class.getName());
	static private Logger startuplogger = Logger.getLogger("StartupLogger");

	@JsonIgnore
	private List<Drive> drives = new ArrayList<Drive>();
	
	@JsonIgnore
	private AtomicBoolean bucketsCreated = new AtomicBoolean(false);

	@JsonIgnore
	private AtomicLong checkOk = new AtomicLong(0);
	
	@JsonIgnore
	private AtomicLong counter = new AtomicLong(0);
	
	@JsonIgnore			
	private AtomicLong encoded = new AtomicLong(0);
	
	@JsonIgnore
	private AtomicLong totalBytes = new AtomicLong(0);
	
	@JsonIgnore
	private AtomicLong errors = new AtomicLong(0);
	
	@JsonIgnore			
	private AtomicLong cleaned = new AtomicLong(0);

	@JsonIgnore
	private AtomicLong notAvailable = new AtomicLong(0);

	@JsonIgnore
	private RAIDSixDriver driver;

	@JsonIgnore
	private Thread thread;

	@JsonIgnore
	private AtomicBoolean done;
	
	@JsonIgnore
	private LockService vfsLockService;
	
	private OffsetDateTime dateConnected;
	
	
	public RAIDSixDriveSync(RAIDSixDriver driver) {
		this.driver=driver;
		this.vfsLockService = this.driver.getLockService();
	}
	
	public AtomicBoolean isDone() {
		return this.done;
	}
	
	public AtomicLong getErrors() {
		return this.errors;
	}
	
	public AtomicLong getNnotAvailable() {
		return this.notAvailable;
	}

	@Override
	public void run() {
		
		logger.info("Starting -> " + getClass().getSimpleName());
		
		// wait until the VFS is operational
		
		
		long start = System.currentTimeMillis();
		
		try {
			Thread.sleep(1000 * 2);											
		} catch (InterruptedException e) {
		}
		
		while (getDriver().getVFS().getStatus()!=ServiceStatus.RUNNING) {
			logger.info("waiting for Virtual File System to startup (" + String.valueOf(Double.valueOf(System.currentTimeMillis() - start) / Double.valueOf(1000)) + " secs");
			try {
				Thread.sleep(1000 * 2);											
			} catch (InterruptedException e) {
			}
			
		}
		
		encode();
		
		if (this.errors.get()>0 || this.notAvailable.get()>0) {
			startuplogger.error("The process can not be completed due to errors");
			return;
		}
	
		updateDrives();
		
		this.done = new AtomicBoolean(true);
	}
	
	
	/**
	 * 
	 */
	@PostConstruct
	public void onInitialize() {
		this.thread = new Thread(this);
		this.thread.setDaemon(true);
		this.thread.setName(this.getClass().getSimpleName());
		this.thread.start();
	}
	
	/**
	 * 
	 */
	private void encode() {
		
		logger.debug("Starting Drive sync");
		
		long start_ms = System.currentTimeMillis();
		
		
		final int maxProcessingThread = Double.valueOf(Double.valueOf(Runtime.getRuntime().availableProcessors()-1) / 2.0 ).intValue() + 1;
		
		getDriver().getDrivesAll().forEach( d -> drives.add(d));
		this.drives.sort( new Comparator<Drive>() {
			@Override
			public int compare(Drive o1, Drive o2) {
				try {
					return o1.getDriveInfo().getOrder()<o2.getDriveInfo().getOrder()?-1:1;
				} catch (Exception e) {
					return 0;
				}
			}
		});
		
							
		this.dateConnected = getDriver().getVFS().getMapDrivesAll().values().
				stream().
				filter(d -> d.getDriveInfo().getStatus()==DriveStatus.NOTSYNC).
				map(v -> v.getDriveInfo().getDateConnected()).
				reduce( OffsetDateTime.MIN, (a, b) -> 
				a.isAfter(b) ?
				a:
				b);
		
		ExecutorService executor = null;

		try {
			
			this.errors = new AtomicLong(0);
			
			executor = Executors.newFixedThreadPool(maxProcessingThread);
			
			for (VFSBucket bucket: this.driver.getVFS().listAllBuckets()) {
				
				Integer pageSize = Integer.valueOf(ServerConstant.DEFAULT_COMMANDS_PAGE_SIZE);
				Long offset = Long.valueOf(0);
				String agentId = null;
				
				boolean done = false;
				
				
				 
				 
				while (!done) {
															
					DataList<Item<ObjectMetadata>> data = getDriver().getVFS().listObjects(	bucket.getName(), 
																							Optional.of(offset),
																							Optional.ofNullable(pageSize),
																							Optional.empty(),
																							Optional.ofNullable(agentId)
																						  ); 
					if (agentId==null)
						agentId = data.getAgentId();

					List<Callable<Object>> tasks = new ArrayList<>(data.getList().size());
					
					for (Item<ObjectMetadata> item: data.getList()) {
						tasks.add(() -> {
							try {

								this.counter.getAndIncrement();
								
								if (( (this.counter.get()+1) % 50) == 0)
									logger.debug("scanned (encoded) so far -> " + String.valueOf(this.counter.get()));
								
								if (item.isOk()) {
									if ( requireSync(item) ) {
										getDriver().syncObject(item.getObject());
										this.encoded.incrementAndGet();
									}
								}
								else {
									this.notAvailable.getAndIncrement();
								}
							} catch (Exception e) {
								logger.error(e,ServerConstant.NOT_THROWN);
								this.errors.getAndIncrement();
							}
							return null;
						 });
					}
					
					try {
						executor.invokeAll(tasks, 10, TimeUnit.MINUTES);
						
					} catch (InterruptedException e) {
						logger.error(e);
					}
					
					offset += Long.valueOf(Integer.valueOf(data.getList().size()).longValue());
					done = (data.isEOD() || (this.errors.get()>0) || (this.notAvailable.get()>0));
				}
			}
			
			try {
				executor.shutdown();
				executor.awaitTermination(10, TimeUnit.MINUTES);
				
			} catch (InterruptedException e) {
			}
			
		} finally {
			
			startuplogger.info("Process completed");
			startuplogger.debug("Threads: " + String.valueOf(maxProcessingThread));
			startuplogger.info("Total scanned: " + String.valueOf(this.counter.get()));
			startuplogger.info("Total encoded: " + String.valueOf(this.encoded.get()));
			//startuplogger.info("Total size: " + String.format("%14.4f", Double.valueOf(totalBytes.get()).doubleValue() / SharedConstant.d_gigabyte).trim() + " GB");

			if (this.errors.get()>0)
				startuplogger.info("Errors: " + String.valueOf(this.errors.get()));
			
			if (this.notAvailable.get()>0)
				startuplogger.info("Not Available: " + String.valueOf(this.notAvailable.get()));
			
			
			startuplogger.info("Duration: " + String.valueOf(Double.valueOf(System.currentTimeMillis() - start_ms) / Double.valueOf(1000)) + " secs");
			startuplogger.info("---------");
		}
	}

	protected RAIDSixDriver getDriver() {
		return this.driver;
	}

	protected LockService getLockService() {
		return this.vfsLockService;
	}

	protected List<Drive> getDrives() {
		return drives;
	}
	
	private void updateDrives() {
		for (Drive drive: getDriver().getDrivesAll()) {
			if (drive.getDriveInfo().getStatus()==DriveStatus.NOTSYNC) {
				DriveInfo info=drive.getDriveInfo();
				info.setStatus(DriveStatus.ENABLED);
				info.setOrder(drive.getConfigOrder());
				drive.setDriveInfo(info);
				getDriver().getVFS().updateDriveStatus(drive);
				startuplogger.debug("drive synced -> " + drive.toString());
			}
		}
	}
	
	private boolean requireSync(Item<ObjectMetadata> item) {
		return item.getObject().lastModified.isBefore(dateConnected);
	}

}
