package com.esri.geoevent.test.performance.websocket;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import com.esri.geoevent.test.performance.PerformanceCollectorBase;
import com.esri.geoevent.test.performance.Mode;
import com.esri.geoevent.test.performance.RunningState;
import com.esri.geoevent.test.performance.RunningStateType;
import com.esri.geoevent.test.performance.TestException;
import com.esri.geoevent.test.performance.jaxb.Config;

/* ------------------------------------------------------------ */
public class WebsocketEventProducer extends PerformanceCollectorBase
{
	private static final String STREAM_SERVICE = "/streamservice";
	private static final String INBOUND = "/inbound";

	private MyConnection[] connections;
	private String host;
	private int port;
	private WebSocketClientFactory factory;
	private WebSocketClient client;
	private int connectionCount;

	public WebsocketEventProducer()
	{
		super(Mode.PRODUCER);
	}
	
	class MyConnection  implements WebSocket.OnTextMessage
	{
		WebSocket.Connection connection;
		public void setConnection( WebSocket.Connection connection )
		{
			this.connection = connection;
		}

		public WebSocket.Connection getConnection()
		{
			return connection;
		}

		/* ------------------------------------------------------------ */
		/** Callback on close of the WebSocket connection
		 */
		@Override
		public void onClose(int closeCode, String message)
		{
			System.out.println("The connection was closed by the remote host.  (this should not happen)");
			connection = null;
		}

		/* ------------------------------------------------------------ */
		/** Callback on receiving a message
		 */
		@Override
		public void onMessage(String data)
		{

		}

		/* ------------------------------------------------------------ */
		/** Callback on receiving a connection
		 */
		@Override
		public void onOpen(Connection connection)
		{
			this.connection = connection;
		}

	}

	@Override
	public void init(Config config) throws TestException
	{
		try
		{
			//System.out.println("WebsocketEventProducer.init()");
			if( factory == null )
			{
				//System.out.println("Factory is null . . .");
				factory = new WebSocketClientFactory();
				factory.start();
			}
			else
			{
				//System.out.println("Factory exists, reusing it. . . ");
			}

			if( client == null )
			{
				client = factory.newWebSocketClient();
				client.setMaxIdleTime(30000);
				client.setProtocol("input");
			}

			loadEvents(new File(config.getPropertyValue("simulationFile", "")));
			host = config.getPropertyValue("host", "localhost");
			port = Integer.parseInt(config.getPropertyValue("port", "5570"));
			connectionCount = Integer.parseInt(config.getPropertyValue("connectionCount", "1"));
			
			String url = "ws://"+host+":"+port+STREAM_SERVICE+INBOUND;
			URI uri = new URI(url);

			connections = new MyConnection[connectionCount];
			for( int i = 0; i < connectionCount; i++ )
			{
				connections[i] = new MyConnection();
				connections[i].setConnection( client.open(uri, connections[i], 10, TimeUnit.SECONDS) );
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			throw new TestException(e.getMessage());
		}
	}

	@Override
	public void validate() throws TestException
	{
		for( MyConnection connection : connections )
		{
			if (connection.getConnection() == null)
				throw new TestException("Socket connection is not established. Please initialize "+WebsocketEventProducer.class.getName()+" before it starts collecting diagnostics.");
		}
		if (events.isEmpty())
			throw new TestException( WebsocketEventProducer.class.getName()+" is missing events to produce.");
	}


	@Override
	public void run()
	{
		if (numberOfEvents > 0)
		{
			if (runningStateListener != null)
				runningStateListener.onStateChange(new RunningState(RunningStateType.STARTED));
			int eventIx = 0;
			Long[] timeStamp = new Long[2];
			timeStamp[0] = System.currentTimeMillis();
			System.out.println("Sending " + numberOfEvents + " events.");
			for (int i=0; i < numberOfEvents; i++)
			{
				if (eventIx == events.size())
					eventIx = 0;
				try
				{
					String message = events.get(eventIx++);
					for( MyConnection conn : connections )
						conn.getConnection().sendMessage(message);
					long currentCount = successfulEvents.incrementAndGet();
					if( currentCount % 100000 == 0 )
						System.out.println("Sent " + currentCount + " messages.");
					if (running.get() == false)
						break;
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			timeStamp[1] = System.currentTimeMillis();
			timeStamps.put(timeStamps.size(), timeStamp);
			running.set(false);
			if (runningStateListener != null)
				runningStateListener.onStateChange(new RunningState(RunningStateType.STOPPED));
		}
	}

	@Override
	public void destroy()
	{
		super.destroy();
		events.clear();
		for( MyConnection conn : connections )
		{
			if( conn.getConnection() != null )
				conn.getConnection().close();
		}
		try {
			//System.out.println("stopping the factory.");
			factory.stop();
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		//System.out.println("Destroying the factory.");
		factory.destroy();
		//System.out.println("Setting the factory to null.");
		factory = null;
		client = null;
		connections = null;
	}
}