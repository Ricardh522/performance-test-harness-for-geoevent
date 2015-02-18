package com.esri.geoevent.test.performance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.esri.geoevent.test.performance.jaxb.Config;
import com.esri.geoevent.test.performance.jaxb.RemoteHost;
import com.esri.geoevent.test.performance.utils.KryoUtils;
import com.esri.geoevent.test.performance.utils.NetworkUtils;

public class RemotePerformanceCollectorBase implements PerformanceCollector 
{
	private static final String REQUEST_SEPERATOR = "::::";
	private ArrayList<Connection> clients = new ArrayList<Connection>();
	private RunningStateListener listener;
	private volatile boolean alive = true;

	public RemotePerformanceCollectorBase(List<RemoteHost> hosts)
	{
		for( RemoteHost host : hosts )
		{
			try {
				synchronized(clients)
				{
					clients.add( new Connection( host.getHost(), host.getCommandPort(), NetworkUtils.isLocal(host.getHost()) ) );
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void setRunningStateListener(RunningStateListener listener)
	{
		this.listener = listener;
	}
	
	@Override
	public void listenOnCommandPort(int port, boolean isLocal)
	{
		// This should never be called on the Remote Diagnostics Collectors
		System.err.println("listenOnCommandPort() was called on the " + this.getClass().getName() + " class, which should never happen.  It commands other machines, not the other way around.");
	}

	//-------------------------------------------------------------
	// Start Request Commands
	//-------------------------------------------------------------
	
	@Override
	public synchronized void start() throws RunningException 
	{
		Request request = new Request(RequestType.START);
		send( request );
	}

	@Override
	public synchronized void stop() 
	{
		Request request = new Request(RequestType.STOP);
		send( request );
	}

	@Override
	public synchronized boolean isRunning() 
	{
		Request request = new Request(RequestType.IS_RUNNING);
		List<Response> responses = send( request );
		boolean isRunning = true;
		for( Response response : responses )
		{
			isRunning = isRunning & BooleanUtils.toBoolean(response.getData());
		}
		return isRunning;
	}

	@Override
	public synchronized RunningStateType getRunningState() 
	{		
		Request request = new Request(RequestType.GET_RUNNING_STATE);
		List<Response> responses = send( request );
		
		// read the responses
		RunningStateType state = RunningStateType.UNAVAILABLE;
		for( Response response : responses )
		{
			RunningStateType clientState = RunningStateType.valueOf(response.getData());
			if( clientState == RunningStateType.STARTING || clientState == RunningStateType.STOPPING || clientState == RunningStateType.ERROR )
				return clientState;
			state = clientState;
		}
		return state;
	}

	@Override
	public synchronized void init(Config config) throws TestException 
	{
		String requestStr = KryoUtils.toString(new Request(RequestType.INIT), Request.class);
		String dataStr = KryoUtils.toString(config, Config.class);
		send(requestStr + REQUEST_SEPERATOR + dataStr);
	}

	@Override
	public synchronized void validate() throws TestException 
	{
		Request request = new Request(RequestType.VALIDATE);
		send( request );
	}

	@Override
	public synchronized void destroy() 
	{
		Request request = new Request(RequestType.DESTROY);
		send( request );		
		synchronized(clients)
		{
			alive = false;
			
			// cleanup
			while( clients.size() > 0 )
			{
				Connection connection = clients.remove(0);
				connection.destroy();
				connection = null;
			}
		}
	}

	@Override
	public synchronized void reset() 
	{
		Request request = new Request(RequestType.RESET);
		send( request );
	}

	@Override
	public synchronized int getNumberOfEvents() 
	{
		Request request = new Request(RequestType.GET_NUMBER_OF_EVENTS);
		List<Response> responses = send( request );
		if( responses != null )
		{
			Response response = responses.get(0);
			return Integer.parseInt(response.getData());
		}
		return -1;
	}

	@Override
	public synchronized void setNumberOfEvents(int numberOfEvents)
	{
		Request request = new Request(RequestType.SET_NUMBER_OF_EVENTS, String.valueOf(numberOfEvents));
		send( request );
	}

	@Override
	public synchronized void setNumberOfExpectedResults(int numberOfExpectedResults)
	{
		Request request = new Request(RequestType.SET_NUMBER_OF_EXPECTED_RESULTS, String.valueOf(numberOfExpectedResults));
		send( request );
	}
	
	@Override
	public synchronized long getSuccessfulEvents()
	{
		Request request = new Request(RequestType.GET_SUCCESSFUL_EVENTS);
		List<Response> responses = send( request );
		if( responses != null )
		{
			Response response = responses.get(0);
			return Long.parseLong(response.getData());
		}
		return 0;
	}
	
	@Override
	public synchronized Map<Integer, Long[]> getTimeStamps()
	{
		Map<Integer,Long[]> timeStamps = new ConcurrentHashMap<Integer, Long[]>();
		synchronized(clients)
		{
			String requestStr = KryoUtils.toString(new Request(RequestType.GET_TIMESTAMPS), Request.class);
			Response response = null;
			String timeStampStr = null;
			for( Connection connection: clients )
			{
				response = connection.send(requestStr);
				if( response == null )
					continue;
				timeStampStr = response.getData();
				if( StringUtils.isNotEmpty(timeStampStr) && timeStampStr.length() > 2 )
				{
					String[] entries = timeStampStr.split("__");
					for( String entry : entries )
					{
						String[] values = entry.split("::");
						Long[] v = new Long[values.length-1];
						for( int i = 1; i < values.length; i++ )
							v[i-1] = Long.valueOf(values[i]) + connection.clockOffset;
						synchronized (timeStamps)
						{
							timeStamps.put( Integer.valueOf(values[0]), v);
						}
					}
				}
			}
		}
		return timeStamps;
	}
	
	//-------------------------------------------------------------
	// Helper Methods
	//-------------------------------------------------------------
	
	private List<Response> send(Request request)
	{
		String requestStr = KryoUtils.toString(request, Request.class);
		return send(requestStr);
	}
	
	private List<Response> send(String requestStr)
	{
		List<Response> responses = new ArrayList<Response>();
		synchronized(clients)
		{
			Response response = null;
			for( Connection connection: clients )
			{
				response = connection.send(requestStr);
				responses.add( response );
			}
		}
		return responses;
	}
	
	//-------------------------------------------------------------
	// Inner Classes
	//-------------------------------------------------------------
	
	/**
	 * Takes care of state changes
	 * 
	 */
	private class StateDelivery extends Thread
	{
		private RunningStateListener listener;
		private RunningState state;

		public StateDelivery(RunningStateListener listener,	RunningState state)
		{
			this.listener = listener;
			this.state = state;
		}

		public void run()
		{
			listener.onStateChange(state);
		}
	}

	/**
	 * This class deals with connectivity between all of the remote performance collectors (consumers and producers)
	 */
	private class Connection
	{
		private BufferedReader in;
		private PrintWriter out;
		private String host;
		private int port;
		private Socket socket;
		private long clockOffset = 0;

		public Connection (String host, int commandPort, boolean isLocal) throws UnknownHostException, IOException
		{
			this.host = host;
			this.port = commandPort;
			System.out.println("Creating remote connection to " + host + ":"+port);
			socket = new Socket( host, port);
			socket.setSoTimeout(50);
			in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
			out = new PrintWriter( socket.getOutputStream() );
			
			Thread thread = new Thread()
			{
				public void run()
				{
					try
					{
						while(alive)
						{
							RunningState newState = null;
							synchronized(in)
							{
								if( alive && in != null && in.ready() )
								{
									String responseStr = readResponse();
									Response response = KryoUtils.fromString(responseStr, Response.class);
									if( response != null && response.getType() == ResponseType.STATE_CHANGED )
									{
										RunningStateType type = RunningStateType.fromValue(response.getData());
										if( type != RunningStateType.UNKNOWN )
											newState = new RunningState(type, getConnectionString());
									}
								}
							}
							if( newState != null )
								listener.onStateChange(newState);
							else
							{
								try {
									Thread.sleep(50);
								} catch (InterruptedException e) 
								{
									break;
								}
							}
						}
					}catch(IOException ex)
					{
					}
				}
			};
			thread.start();
			
			if (!isLocal)
			{
				//System.out.println("getting remote clock time.");
				DatagramPacket timeSocket = new DatagramPacket( new byte[8], 8, InetAddress.getByName(host), 7720);
				DatagramSocket sock = new DatagramSocket(7720);
				while(true)
				{
					long millisStart = System.currentTimeMillis();
					sock.send(timeSocket);
					sock.receive(timeSocket);
					long millisEnd = System.currentTimeMillis();
					byte[] buf = timeSocket.getData();
					ByteBuffer byteBuffer = ByteBuffer.wrap(buf,0,buf.length);
					long remoteTime = byteBuffer.getLong();
					long roundTripTime = millisEnd - millisStart;
					if( roundTripTime < 2 )
					{
						clockOffset = millisEnd - remoteTime;
						sock.close();
						break;
					}
					else
						System.out.println("While getting the remote clock time, round trip was " + roundTripTime + ", trying again.");
				}
			}
		}

		public Response send( String command )
		{
			Response response = null;
			synchronized(in)
			{
				try
				{
					out.println(command);
					out.flush();
	
					// read the response
					String responseStr = readResponse();
					response = KryoUtils.fromString(responseStr, Response.class);
					if( response == null )
						return null;
					
					while( response.getType() == ResponseType.STATE_CHANGED )
					{
						RunningStateType type = RunningStateType.fromValue(response.getData());
						RunningState newState = new RunningState(type, getConnectionString());						
						if( type != RunningStateType.UNKNOWN )
						{
							StateDelivery sd = new StateDelivery(listener,newState);
							sd.start();
						}
						
						// read some more
						responseStr = readResponse();
						response = KryoUtils.fromString(responseStr, Response.class);
					}
				} 
				catch( Exception error)
				{
					error.printStackTrace();
					String errorMsg = error.getMessage();
					response = new Response(ResponseType.ERROR, errorMsg);
				}
				
			}
			return response;
		}

		private String readResponse()
		{
			// make sure we are ready to read
			try{
				while( in != null && ! in.ready() )
				{
					try
					{	
						Thread.sleep(50);
					} 
					catch( Exception ignored)
					{
						
					}
				}
			}
			catch( Exception error)
			{
				error.printStackTrace();
			}
			
			//we need to read multiple lines efficiently
			String response = null;
			StringWriter sw = new StringWriter();
			try
			{
				char[] buffer = new char[1024 * 4];
				int n = 0;
				while (-1 != (n = in.read(buffer))) {
				    sw.write(buffer, 0, n);
				}
			}
			catch(Exception ex)
			{
				//ignore
			}
			finally
			{
				response = sw.toString();
			}
			return response;
		}
		
		private void destroy()
		{
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(socket);
			
			in = null;
			out = null;
			socket = null;
		}
		
		public String getConnectionString()
		{
			return host + ":" + port;
		}
	}
}