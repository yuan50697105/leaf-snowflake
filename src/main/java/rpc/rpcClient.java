package rpc;

/**
 * Created by weizhenyi on 2017/6/27.
 */
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Utils;


public class rpcClient {

	private static final Logger LOG = LoggerFactory.getLogger(rpcClient.class);
	private static AtomicInteger ai = new AtomicInteger(0);
	private static ConcurrentHashMap chm = new ConcurrentHashMap();
	public static Map<String,Long> getPeersTimeStamps(List<String> peers)
	{
		Map<String,Long> peersTimestamps = new HashMap<>();
		for(String peer : peers)
		{
			try {
				String ip = peer.split(":")[0];
				int  port = Integer.valueOf(peer.split(":")[1]);
				long timestamp = getTimestamp(ip,port,3000);
				peersTimestamps.put(peer,timestamp);
			} catch (Exception e)
			{
				LOG.warn("failed to get timestamp from peer: " + peer,e);
				continue;
			}

		}
		return peersTimestamps;
	}

	public static long getTimestamp(String ip, int port ,int timeout) throws Exception
	{
		TTransport transport = new TSocket(ip,port,timeout);
		TProtocol protocol = new TBinaryProtocol(transport);
		leafrpc.Client client = new leafrpc.Client(protocol);
		transport.open();
		long timestamp = client.gettimestamp(Utils.currentTimeMs());
		transport.close();
		return timestamp;
	}

	//client of TThreadPoolServer
	public static void startClient(String ip ,int port ,int timeout) throws Exception
	{
		TTransport transport = new TSocket(ip,port,timeout);
		TProtocol protocol = new TBinaryProtocol(transport);
		leafrpc.Client client = new leafrpc.Client(protocol);
		transport.open();

		int i = 0;
		while(i < 2000000)
		{
			 client.getID("");
			 ++i;
		}

		transport.close();
	}
	// client of TThreadedSelectorServer , TNonblockingServerSocket ,THsHaServer
	public static void startClient2(String ip ,int port ,int timeout) throws Exception
	{
		TTransport transport = new TFramedTransport(new TSocket(ip,port,timeout));
		TProtocol protocol = new TBinaryProtocol(transport);
		leafrpc.Client client = new leafrpc.Client(protocol);
		transport.open();

		for(int i = 0; i< 1000000; i++)
		{
			client.getID("");
			if (i % 100000 == 0)
			{
				System.out.println(Thread.currentThread().getName() + " " + client.getID(""));
			}
			//ai.incrementAndGet();
		}
		transport.close();
	}

	public static void main(String[] args) throws Exception
	{
		final CountDownLatch latch = new CountDownLatch(1);
		long current = Utils.currentTimeMs();
		Thread thread1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					startClient("172.21.0.190",2182,10000);
					latch.countDown();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		thread1.setName("thread1");
		thread1.start();

		/*Thread thread2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					startClient("172.21.0.189",2182,10000);
					latch.countDown();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		thread2.setName("thread2");
		//thread2.start();*/

/*		Thread thread3 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					startClient("172.21.0.188",2182,10000);
					latch.countDown();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		thread3.setName("thread3");
		//thread3.start();*/
		latch.await();
		long total = Utils.currentTimeMs() - current;
		System.out.println("spend " + total + " ms with " + 2000000 + " requests." );
		//System.out.println(chm.size());



	}
}
