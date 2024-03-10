package io.odilon.vfs.raid6;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.odilon.errors.InternalCriticalException;
import io.odilon.log.Logger;
import io.odilon.model.ObjectMetadata;
import io.odilon.model.ObjectStatus;
import io.odilon.model.ServerConstant;
import io.odilon.vfs.model.BucketIterator;
import io.odilon.vfs.model.Drive;


/**
 * <p><b>RAID 6</b> <br/>
 * Data is partitioned into blocks encoded in RS and stored on all drives</p>
 *
 * <p>The RAID 6 {@link BucketIterator} uses a randomly selected Drive to 
 * walk and return {@link ObjectMetata} instances. All Drives contain all ObjectMetadata in RAID 6.</p>
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
public class RAIDSixBucketIterator extends BucketIterator implements Closeable {

	private static final Logger logger = Logger.getLogger(RAIDSixBucketIterator.class.getName());
	
	@JsonProperty("prefix")
	private String prefix = null;
	
	@JsonProperty("drive")
	private Drive drive;
	
	@JsonProperty("cumulativeIndex")		
	private long cumulativeIndex = 0;

	/** next item to return -> 0 .. [ list.size()-1 ] */
	@JsonIgnore
	private int relativeIndex = 0;  
	
	@JsonIgnore
	private Iterator<Path> it;
	
	@JsonIgnore
	private Stream<Path> stream;
	
	@JsonIgnore
	private List<Path> buffer;
	
	@JsonIgnore
	private boolean initiated = false;

	@JsonIgnore
	RAIDSixDriver driver;
				
	public RAIDSixBucketIterator(RAIDSixDriver driver, String bucketName, Optional<Long> opOffset,  Optional<String> opPrefix) {
		super(bucketName);
		opPrefix.ifPresent( x -> this.prefix=x.toLowerCase().trim());
		opOffset.ifPresent( x -> setOffset(x));
		this.driver = driver;
		this.drive  = driver.getDrivesEnabled().get(Double.valueOf(Math.abs(Math.random()*10000)).intValue() % driver.getDrivesEnabled().size());
	}
	
	
	@Override
	public boolean hasNext() {
		
		if(!this.initiated) {
			init();
			return fetch();
		}
		/** 
		 * if the buffer 
		 *  still has items  
		 * **/
		if (this.relativeIndex < this.buffer.size())
			return true;
				
		return fetch();
	}

	@Override
	public Path next() {
		
		/**	
		 * if the buffer still has items to return 
		 * */
		if (this.relativeIndex < this.buffer.size()) {
			Path object = this.buffer.get(this.relativeIndex);
			this.relativeIndex++; 
			this.cumulativeIndex++; 
			return object;
		}

		boolean hasItems = fetch();
		
		if (!hasItems)
			throw new IndexOutOfBoundsException(   "No more items available. hasNext() should be called before this method. "
												 + "[returned so far -> " + String.valueOf(cumulativeIndex)+"]" );
		
		Path object = this.buffer.get(this.relativeIndex);

		this.relativeIndex++;
		this.cumulativeIndex++;
		
		return object;

	}
	@Override
	public synchronized void close() throws IOException {
		if (this.stream!=null)
			this.stream.close();
	}
	
	
	
	private void init() {
		
		Path start = new File(this.drive.getBucketMetadataDirPath(getBucketName())).toPath();
		try {
			this.stream = Files.walk(start, 1).skip(1).
					filter(file -> Files.isDirectory(file)).
					filter(file -> this.prefix==null || file.getFileName().toString().toLowerCase().startsWith(this.prefix));
					//filter(file -> isObjectStateEnabled(file));
			
		} catch (IOException e) {
			logger.error(e);
			throw new InternalCriticalException(e);
		}
		this.it = this.stream.iterator();
	skipOffset();
	this.initiated = true;
	}
	
	private void skipOffset() {
		if (getOffset()==0)
			return;
		boolean isItems = true;
		int skipped = 0;
		while (isItems && skipped<getOffset()) {
			if (this.it.hasNext()) {
				this.it.next();
				skipped++;
			}
			else {
				break;
			}
		}
	}

	
	/**
	 * <p>This method should be used when the delete operation is logical (ObjectStatus.DELETED)</p>
	 * @param path
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean isObjectStateEnabled(Path path) {
		ObjectMetadata meta = getDriver().getObjectMetadata(getBucketName(), path.toFile().getName());
		if (meta==null)
			return false;
		if (meta.status == ObjectStatus.ENABLED) 
			return true;
		return false;
	}
	
	/**
	 * @return false if there are no more items
	 */
	private boolean fetch() {

		this.relativeIndex = 0;
		this.buffer = new ArrayList<Path>();
		boolean isItems = true;
		while (isItems && this.buffer.size() < ServerConstant.BUCKET_ITERATOR_DEFAULT_BUFFER_SIZE) {
			if (it.hasNext())
				this.buffer.add(it.next());		
			else 
				isItems = false;
		}
		return !this.buffer.isEmpty();
	}

	private RAIDSixDriver getDriver() {
		return driver;
	}

}
