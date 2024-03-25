![spring-gaede65182_1280](https://github.com/atolomei/odilon-server/assets/29349757/f1c6f491-9d1f-4e4d-af87-f7e57713542a)
<h1>Odilon Object Storage</h2>

<h2>Lightweight and scalable</h2>
<p>Odilon is a scalable and lightweight <b>Open Source</b> <b>Object Storage</b> that runs on standard hardware (<a href="https://odilon.io" target="_blank">Odilon project website</a>).</p>
<p>It is an infrastructure software designed to be used by applications that need to store terabytes of medium to large size objects (like photos, pdfs, audio, video) securely and safely through encryption, replication and redundancy. A typical installation would by 100M pdfs 20KB-30MB each.</p>
<p>It has a simple single-level folder structure similar to the <b>Bucket</b> / <b>Object</b> model of <a href="https://aws.amazon.com/s3 /" target="_blank">Amazon S3</a>. It is small and easy to integrate, offers <b>encryption</b>, data protection and fault tolerance (<b>software RAID </b> and <b>Erasure Codes</b>) and detection of <b>silent data degradation</b>. Odilon also supports <b>version control</b> and <b>master - standby replication over the Internet</b>.</p>
</p>

<h2>Main features</h2>
				<p>
				<ul>
				<li> Scalable Object Storage on commodity disks</li>
				<li>Single binary, does not neet Database or other external service</li>
				<li> Developed in Java (uses Spring Boot, OkHttp, Jackson, Caffeine, Metrics, among others) </li>
				<li> Runs on Linux and Windows</li>				
				<li>Single dependency-free binary, does not neet Database or other external service</li>
				<li> SDK Java 11+ for client applications</li >
				<li> HTTP/S for client server communication</li>
				<li>License <a href="https://www.apache.org/licenses/LICENSE-2.0" target="_blank">Open Source Apache 2</a>. It can be used for Open Source and commercial projects </li>
				<li>Encryption <i>at rest</i> (<a href="https://es.wikipedia.org/wiki/Advanced_Encryption_Standard" target="_blank">AES 256</a>) </li>
				<li>Integration with Key Management Server <a href="https://www.vaultproject.io/" target="_blank">Hashicorp Vault</a> </li>
				<li>Simple operation. Adding new disks requires one line in the config file, and an <i>async process</i> sets up disks and replicata data in background</li>
				<li>Data replication using <a href="https://en.wikipedia.org/wiki/Erasure_code" target="_blank">Erasure Coding</a> and <a href="https://en.wikipedia.org/wiki/RAID" target="_blank">software RAID</a></li>
				<li>Data immutability. Odilon supports two storage modes that protect data from deletion, whether accidental or intentional: Read Only and <a href="https://en.wikipedia.org/wiki/Write_once_read_many" target="_blank">WORM</a> (Write Once Read Many)
				<li>Master - Standby architecture with async replication over the web, for disaster recovery, high availability, archival, ransomware recovery</li>
				<li>Version Control</b></li>
				<li>Tolerates full disk failures</li>
				<li>Disk monitoring for silent and slow data degradation detection (<a href="https://en.wikipedia.org/wiki/Data_degradation" target="_blank" >bit rot detection</a>)</li>
				</ul>
				</p>

<h2>Security</h2>
<p>Odilon keeps objects encrypted (<i><b>Encryption at Rest</b></i>) using modern algorithms such as <a href="https://es.wikipedia.org/wiki/Advanced_Encryption_Standard" target="_blank">AES-256</a>. Each object has a unique encryption key. In turn, the encryption key of the object can be generated by Odilon or, which is recommended for greater security, by a <b>Key Management Server</b> (<a href="https://en.wikipedia.org/wiki/Key_management" target="_blank ">KMS</a>)</p>
<p>A KMS is software for generating, distributing, and managing cryptographic keys. It includes <i>back-end</i> functionality for key generation, distribution, and replacement, as well as client-side functionality for injecting keys, storing, and managing keys on devices. Moving key management to KMS prevents application reverse engineering attacks, simplifies operational maintenance, and compliance with security policies and regulations.</p>
<p>Odilon integrates with the KMS Open Source <a href="https://www.vaultproject.io/" target="_blank">Hashicorp Vault</a>.</p>
<p>Communication from source to storage is via <b>HTTPS</b>, which uses encryption to increase the security of data transfers (this functionality requires Java 17. v2 05/2024).</p>
 
<h2>Data Replication</h2>
<p>Odilon can be configured to use software RAID for data replication. The supported configurations are</p>
<p>
<ul>
<li><b>RAID 0.</b> Two or more disks are combined to form a volume, which appears as a single virtual drive.
It is not a configuration with data replication, its function is to provide greater storage and performance by allowing access to the disks in parallel.<br/><br/>
</li>
<li><b>RAID 1.</b>For each object, 1 or more exact copies (or mirrors) are created on two or more disks. This provides redundancy in case of disk failure. At least 2 disks are required, Odilon also supports 3 or more for greater redundancy.<br/><br/>
</li>
<li><b>RAID 6 / Erasure Coding.</b>
It is a method of encoding data into blocks that can be distributed across multiple disks or nodes and then reconstructed from a subset of those blocks. It has great flexibility since you can adjust the number and size of the blocks and the minimum required for recovery. It uses less disk space than RAID 1 and can withstand multiple full disk failures. Odilon implements this architecture using Reed Solomon error-correction codes. The configurations are: <br/>
	<b>3 disks</b> (2 data 1 parity, supports 1 disk failure), <br/>  
	<b>6 disks</b> (4 data 2 parity, supports up to 2 disks failure) <br/>
	<b>12</b> disks (8 data 4 parity, supports up to 4 disks failure).</li> <br/>
</ul>
</p>

<h2>Master Standby Architecture</h2>
<p>Odilon supports Master - Standby Architecture for <b>disaster recovery</b>, <b>high availability</b>, <b>archival</b>, and <b>anti-ransomware</b> protection. Data replication is done asynchronously using HTTP/S over the local network or the Internet. Setting up a standby server is simple, just add the URL and credentials to the master configuration file and restart. 
Odilon will propagate each operation to the standby server. It will also run a replication process in background for data existing before connecting the standby server. 
<br/>
<br/>
<br/>
<br/>
​</p>


![odilon-master-standby](https://github.com/atolomei/odilon-server/assets/29349757/913f7b54-1acf-46a2-97c6-3bd42190b9af)


<br/>
<br/>
<br/>
<br/>
<h2>What Odilon is not</h2>
<p>
<ul class="group-list>
<li class="list-item"><b>Odilon is not S3 compatible</b><br/>
Odilon API is way simpler than S3. The only thing it has in common with AWS S3 it that uses the bucket/object methafor to organize the object space.
<br/>
<br/>
</li>
<li class="list-item"><b>Odilon is not a Distributed Storage like Cassandra, Hadoop etc.</b><br/>
Odilon supports master-standby architecture for archival, backup and data protection, 
but it is not a Distributed Storage and it does not support active-active replication.
<br/>
<br/>
</li>
<li class="list-item"><b>Odilon is not a File System like GlusterFS, Ceph, ext4, etc.</b><br/>
It uses the underlying file system to stores objects as encrypted files, or in some configurations to break objects into chunks.
<br/>
<br/>
</li>
<li class="list-item"><b>Odilon is not a NoSQL database like MongoDB, CouchDB, etc.</b><br/> 
It does not use a database engine, 
Odilon uses its own journaling agent for Transaction Management 
and only supports very simple queries, ie. to retrieve an object and to list the objects of a bucket filtered by objectname's prefix.
<br/>
<br/>
<li class="list-item"><b>Odilon is not optimized for a very large number of small files</b></b><br/>  
Odilon does not have optimization for lots of small files. 
The files are simply stored encrypted and compressed to local disks. 
Plus the extra meta file and shards for erasure coding.
<br/>
<br/>
</li>
</ul>


<h2>Using Odilon</h2>
<p>
A Java client program that interacts with the Odilon server must include the Odilon SDK jar in the classpath.
A typical architecture for a Web Application is</p> 

<br/>
<br/>

![web-app-odilon-en](https://github.com/atolomei/odilon-server/assets/29349757/115e1cc0-223d-4f92-a121-e3f9ad3a1418)

<br/>
<br/>
Example to upload 2 pdf files:
<br/>
<br/>

```java
String endpoint = "http://localhost"; 

/** default port */
int port = 9234; 

/** default credentials */
String accessKey = "odilon";
String secretKey = "odilon";
			
String bucketName  = "demo_bucket";
String objectName1 = "demo_object1";
String objectName2 = "demo_object2";
			
File file1 = new File("test1.pdf");
File file2 = new File("test2.pdf");
			
/* put two objects in the bucket,
the bucket must exist before sending the object,
and object names must be unique for that bucket */
			
OdilonClient client = new ODClient(endpoint, port, accessKey, secretKey);

client.putObject(bucketName, objectName1, file1);
client.putObject(bucketName, objectName2, file2);
```
<p>
More info in Odilon's website <br/>
<a href="https://odilon.io/development.html" target="_blank">Java Application Development with Odilon</a>
</p>

<h2>Download</h2>
<p>
<ul>
<li><a href="https://odilon.io#download" target="_blank">Odilon Server</a></li>	
<li><a href="https://odilon.io#download" target="_blank">Odilon Java SDK</a></li>	
</ul>
</p>

<h2>Resources</h2>
<p>
<ul>
<li><a href="https://odilon.io" target="_blank">Odilon website</a></li>	
<li><a href="https://odilon.io/configuration-linux.html" target="_blank">Installation, Configuration and Operation on Linux</a></li>	
<li><a href="https://odilon.io/configuration-windows.html" target="_blank">Installation, Configuration and Operation on Windows</a></li>		
<li><a href="https://odilon.io/development.html" target="_blank">Java Application Development with Odilon</a></li>	
<li><a href="https://odilon.io/javadoc/index.html" target="_blank">Odilon SDK Javadoc</a></li>	
<li><a href="https://youtu.be/kI6jG9vZAjI?si=3KSOpbvN-6ThJf1m" target="_blank">Odilon demo - YouTube video (4 min)<a></li>	
<li><a href="https://twitter.com/odilonSoftware" target="_blank">Twitter</a></li>
</ul>
</p>

<h2>SDK Source Code</h2>
<p>
<ul>
<li class="list-item"><a href="https://github.com/atolomei/odilon-client" target="_blank">odilon-client</a></li>
<li class="list-item"><a href="https://github.com/atolomei/odilon-model" target="_blank">odilon-model</a></li>
</ul>
</p>



