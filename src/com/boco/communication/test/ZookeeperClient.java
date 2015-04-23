package com.boco.communication.test;

import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

public class ZookeeperClient {

	
	public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
		ZooKeeper zk = new ZooKeeper("10.11.11.144", 60000, new Watcher() {
			
			@Override
			public void process(WatchedEvent event) {
				System.out.println("EVENT:" + event.getType());
			}
		});
		System.out.println("ls / => " + zk.getChildren("/", true));
		
		if(zk.exists("/node", true) == null){
			zk.create("/node", "long".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create /node long");
			System.out.println("get /node => " + new String(zk.getData("/node", false, null)));
			System.out.println("ls / => " + zk.getChildren("/", true));
		}
		
		if(zk.exists("/node", true) != null){
			zk.setData("/node", "long changed".getBytes(), -1);
			System.out.println("get /node => " + new String(zk.getData("/node", false, null)));
		}
		
		if(zk.exists("/node/sub1", true) != null){
			zk.delete("/node/sub1", -1);
			zk.delete("/node", -1);
			System.out.println("ls / =>" + zk.getChildren("/", true));
		}
		
		zk.close();
		
	}
	
	
}
