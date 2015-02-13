package com.esri.ges.test.performance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.esri.ges.test.performance.jaxb.Config;
import com.esri.ges.test.performance.jaxb.ConsumerConfig;
import com.esri.ges.test.performance.jaxb.ProducerConfig;
import com.esri.ges.test.performance.utils.KryoUtils;

public abstract class DiagnosticsCollectorBase implements DiagnosticsCollector, Runnable
{
	private static final String ERROR = "ERROR:";
	private static final String STATE_LABEL = "STATE:";
	private static final String OK = "OK";

	protected int numberOfEvents;
	protected int numberOfExpectedResults;
	protected Map<Integer, Long[]> timeStamps = new ConcurrentHashMap<Integer, Long[]>();
	protected AtomicBoolean running = new AtomicBoolean(false);
	protected RunningStateListener runningStateListener;
	protected List<String> events = new ArrayList<String>();
	private CommandInterpreter commandInterpreter;
	protected AtomicLong successfulEvents = new AtomicLong();
	protected final Mode mode;
	protected long timeOutInSec = 10;
	
	public DiagnosticsCollectorBase(Mode mode)
	{
		this.mode = mode;
	}
	
	public int getNumberOfEvents()
	{
		return numberOfEvents;
	}
	public void setNumberOfEvents(int numberOfEvents)
	{
		this.numberOfEvents = numberOfEvents;
	}

	public int getNumberOfExpectedResults()
	{
		return numberOfExpectedResults;
	}
	public void setNumberOfExpectedResults(int numberOfExpectedResults)
	{
		this.numberOfExpectedResults = numberOfExpectedResults;
	}
	
	public long getSuccessfulEvents()
	{
		return successfulEvents.get();
	}

	public void setTimeOutInSec(long timeOutInSec)
	{
		this.timeOutInSec = timeOutInSec;
	}
	
	public synchronized Map<Integer, Long[]> getTimeStamps()
	{
		return timeStamps;
	}

	@Override
	public void start() throws RunningException
	{
		running = new AtomicBoolean(true);
		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	public void stop()
	{
		running.set(false);
	}

	@Override
	public boolean isRunning()
	{
		return running.get();
	}

	@Override
	public void destroy()
	{
		reset();
	}

	@Override
	public void reset()
	{
		successfulEvents.set(0);
		synchronized (timeStamps)
		{
			timeStamps.clear();
		}
	}

	@Override
	public RunningState getRunningState()
	{
		return running.get() ? RunningState.STARTED : RunningState.STOPPED;
	}

	@Override
	public String getStatusDetails()
	{
		return null;
	}

	@Override
	public void setRunningStateListener(RunningStateListener listener)
	{
		this.runningStateListener = listener;
	}

	protected void loadEvents(File file) throws TestException
	{
		if (!events.isEmpty())
			events.clear();
		
		BufferedReader input = null;
		try
		{
			input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = null;
			while ((line = input.readLine()) != null)
				events.add(line + "\n");
		}
		catch (Exception e)
		{
			throw new TestException(e.getMessage());
		}
		finally
		{
			if (input != null)
			{
				try
				{
					input.close();
					input = null;
				}
				catch (IOException e)
				{
					;
				}
			}
		}
	}
	
	public void listenOnCommandPort(int commandPort, boolean isLocal)
	{
		//System.out.println("Listening on port " + commandPort);
		commandInterpreter = new CommandInterpreter( commandPort );
		setRunningStateListener(commandInterpreter);
		Thread t = new Thread(commandInterpreter);
		t.start();
		if (!isLocal)
		{
			t = new Thread(new ClockSync());
			t.start();
		}
	}

	private class CommandInterpreter implements Runnable, RunningStateListener
	{
		int port;
		private BufferedReader in;
		private PrintWriter out;

		public CommandInterpreter( int commandPort )
		{
			this.port = commandPort;
		}

		public void run()
		{
			ServerSocket s;
			try 
			{
				s = new ServerSocket( port );
				while(true)
				{
					//System.out.println("Listening for a connection from the orchestrator.");
					Socket commandSocket = s.accept();
					try
					{
						commandSocket.setSoTimeout(50);
						in = new BufferedReader( new InputStreamReader( commandSocket.getInputStream() ) );
						out = new PrintWriter( commandSocket.getOutputStream() );
						while(in != null)
						{
							//we need to read multiple lines efficiently
							StringWriter sw = new StringWriter();
							String command = null;
							try
							{
								char[] buffer = new char[1024 * 4];
								int n = 0;
								while (-1 != (n = in.read(buffer))) {
								    sw.write(buffer, 0, n);
								}
							}
							catch(SocketTimeoutException ex)
							{
								//ignore
							}
							finally
							{
								command = sw.toString();
							}
							
							if( StringUtils.isEmpty(command))
								continue;
							
							//System.out.println("Received command \"" + command + "\"");
							synchronized(out)
							{
								if( command.startsWith("start") )
								{
									try
									{
										start();
										out.println(OK);
									}catch(RunningException ex)
									{
										out.println(ERROR+ex.getMessage());
									}
								}
								else if( command.startsWith("stop"))
								{
									stop();
									out.println(OK);
								}
								else if( command.startsWith("isRunning"))
								{
									boolean b = isRunning();
									if(b)
										out.println("TRUE");
									else
										out.println("FALSE");
								}
								else if( command.startsWith("getRunningState"))
								{
									RunningState st = getRunningState();
									out.println(st.toString());
								}
								else if( command.startsWith("getStatusDetails"))
								{
									String details = getStatusDetails();
									out.println(details);
								}
								else if( command.startsWith("init:"))
								{
									String dataStr = command.substring("init:".length());
									//System.out.println( "Incoming Data: " + dataStr);
									Config config = null;
									if( mode == Mode.CONSUMER)
										config = KryoUtils.fromString(dataStr, ConsumerConfig.class);
									else if( mode == Mode.PRODUCER)
										config = KryoUtils.fromString(dataStr, ProducerConfig.class);
									try
									{
										init(config);
										out.println(OK);
									}catch(TestException ex)
									{
										out.println(ERROR+ex.getMessage());
									}
								}
								else if( command.startsWith("validate"))
								{
									try
									{
										validate();
										out.println(OK);
									}catch(TestException ex)
									{
										out.println(ERROR+ex.getMessage());
									}
								}
								else if( command.startsWith("destroy"))
								{
									destroy();
									out.println(OK);
									out.flush();
									
									//close the input and output stream
									IOUtils.closeQuietly(in);
									in = null;
									IOUtils.closeQuietly(out);
									out= null;
									
									//continue to the top of the while loop
									continue;
								}
								else if( command.startsWith("reset"))
								{
									reset();
									out.println(OK);
								}
								else if( command.startsWith("getNumberOfEvents"))
								{
									int num = getNumberOfEvents();
									out.println(String.valueOf(num));
								}
								else if( command.startsWith("getSuccessfulEvents"))
								{
									long num = getSuccessfulEvents();
									out.println(String.valueOf(num));
								}
								else if( command.startsWith("setNumberOfEvents"))
								{
									if( command.length() > "setNumberOfEvents:".length() )
									{
										String param = command.substring("setNumberOfEvents:".length()).trim();
										setNumberOfEvents(Integer.parseInt(param));
									}
									out.println(OK);
								}
								else if( command.startsWith("setNumberOfExpectedResults"))
								{
									if( command.length() > "setNumberOfExpectedResults:".length() )
									{
										String param = command.substring("setNumberOfExpectedResults:".length()).trim();
										setNumberOfExpectedResults(Integer.parseInt(param));
									}
									out.println(OK);
								}
								else if( command.startsWith("setTimeOutInSec"))
								{
									if( command.length() > "setTimeOutInSec:".length() )
									{
										String param = command.substring("setTimeOutInSec:".length()).trim();
										setTimeOutInSec(Long.parseLong(param));
									}
									out.println(OK);
								}
								else if( command.startsWith("getTimeStamps"))
								{
									Map<Integer, Long[]> values = getTimeStamps();
									StringBuilder b = new StringBuilder();
									for( Integer key : values.keySet() )
									{
										b.append("__"+key);
										Long[] valueArray = values.get(key);
										for( Long l : valueArray )
											b.append("::"+l);
									}
									if(b.length()>2)
										out.println(b.substring(2));
									else
										out.println();
								}
								out.flush();
							}
						}
					}catch(IOException ex)
					{
						if( ex.getMessage().equals("Connection reset") )
						{
							System.out.println("The orchestrator disconnected.");
							reset();
						}
						else
							ex.printStackTrace();
					}
				}
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

		@Override
		public void onStateChange(RunningState newState)
		{
			if( out != null )
			{
				synchronized (out)
				{
					//System.out.println("Sending state change command: " + STATE_LABEL+newState);
					if( out != null && newState != null)
					{
						out.println(STATE_LABEL+newState);
						out.flush();
					}
				}
			}
		}
	}
}