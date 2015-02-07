package com.esri.ges.test.performance.kafka;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import com.esri.ges.test.performance.DiagnosticsCollectorBase;
import com.esri.ges.test.performance.Mode;
import com.esri.ges.test.performance.RunningState;
import com.esri.ges.test.performance.TestException;

/**
 * This class creates multiple threads to process incoming data and uses multiple Kafka producer to send message to kafka.
 * Currently the max number of threads and producers are set to 4.
 *
 */
public class KafkaEventProducerMultiThread extends DiagnosticsCollectorBase
{
  private String brokerList;
  private String topic;
  private String acks;
//  private List<Producer<String, String>> producers;
//  private Producer<String, String> producer;
  
  private int eventsPerSec = -1;
  private int staggeringInterval = 10;
  
  private ExecutorService executor;
  private int numThreads = 4;
  private BlockingQueue<Producer<String, String>> producers;
  private List<String> sync_events = Collections.synchronizedList(events);
  
  private volatile boolean more = true;
  
  @Override
  public synchronized void init(Properties props) throws TestException
  {
    try
    {
      String path = props.containsKey("simulationFilePath") ? props.getProperty("simulationFilePath").trim() : "";
      loadEvents(new File(path));
      
      brokerList = props.containsKey("brokerlist") ? props.getProperty("brokerlist").trim() : "localhost:9092";
      topic = props.containsKey("topic") ? props.getProperty("topic").trim() : "default-topic";
      acks = props.containsKey("requiredacks") ? props.getProperty("requiredacks").trim() : "1";
      
      Properties kprops = new Properties();
      
      kprops.put("metadata.broker.list", brokerList);
      kprops.put("serializer.class", "kafka.serializer.StringEncoder");
      kprops.put("request.required.acks", acks);
      kprops.put("partitioner.class", "com.esri.ges.test.performance.kafka.SimplePartitioner");
      kprops.put("producer.type", "async");
      kprops.put("queue.buffering.max.ms", "1000");
      kprops.put("batch.num.messages", "2000");
       
      ProducerConfig config = new ProducerConfig(kprops);
      
//      producer = new Producer<String, String>(config);
      
      eventsPerSec = props.containsKey("eventsPerSec") ? Integer.parseInt(props.getProperty("eventsPerSec")) : -1;
      staggeringInterval = props.containsKey("staggeringInterval") ? Integer.parseInt(props.getProperty("staggeringInterval")) : 10;

      mode = Mode.PRODUCER;
      
      executor = Executors.newFixedThreadPool(numThreads);
//      producers = new ArrayList<Producer<String, String>>();
      producers = new ArrayBlockingQueue<Producer<String, String>>(numThreads);
      for(int i=0; i<numThreads; i++)
      {
        producers.add(new Producer<String, String>(config) );
      }
      
    }
    catch (Throwable e)
    {
      e.printStackTrace();
      throw new TestException(e.getMessage());
    }
  }
  
  public String getBrokerList()
  {
    return brokerList;
  }
  
  public String getTopic()
  {
    return topic;
  }
  
  public String getAcks()
  {
    return acks;
  }
 

  @Override
  public void validate() throws TestException
  {
    if (producers == null)
      throw new TestException("Kafka producers is not created.");
    if (events.isEmpty())
      throw new TestException("KafkaEventProducer is missing events to produce.");
  }

  @Override
  public void run(AtomicBoolean alive)
  {
    if (numberOfEvents > 0)
    {
      if (runningStateListener != null)
        runningStateListener.onStateChange(RunningState.STARTED);
      int eventIx = 0;
      Long[] timeStamp = new Long[2];
      timeStamp[0] = System.currentTimeMillis();
      
//      ExecutorService executor = Executors.newFixedThreadPool(5);
      
      // send out events with delay
      if( eventsPerSec > -1 )
      {
        // determine the events to send and delay
        // use a staggering approach to
        int staggeringInterval = (this.staggeringInterval > 0) ? this.staggeringInterval : 10;
        int eventsToSend = eventsPerSec / staggeringInterval;
        long delay = 1000 / staggeringInterval;
        long targetTimeStamp = timeStamp[0];
        long sleepTime = 0;
        
        more = true;

        // loop through all events until we are finished
//        while( successfulEvents.get() < numberOfEvents )
        while( more )
        {
          //System.out.println("successful events is " + successfulEvents.get() + ".  number of events is " + numberOfEvents);
          targetTimeStamp = targetTimeStamp + delay;
          // send the events
          //sendEvents(eventIx, eventsToSend);
          Runnable worker = new WorkerThread(eventIx, eventsToSend);
          executor.execute(worker);

          sleepTime =  targetTimeStamp - System.currentTimeMillis();
          
          // add the delay
          if( sleepTime > 0 )
          {
            try
            {
              Thread.sleep(sleepTime);
            }
            catch( InterruptedException ignored )
            {
              ;
            }
          }
          
          //check if we need to break
          if (running.get() == false)
            break;
        }
        
        
      }
      // no delays just send out as fast as possible
      else
      {
//        sendEvents(eventIx, numberOfEvents);
        Runnable worker = new WorkerThread(eventIx, numberOfEvents);
        executor.execute(worker);
      }
      timeStamp[1] = System.currentTimeMillis();
//      executor.shutdown();
      synchronized(timeStamps)
      {
        timeStamps.put(timeStamps.size(), timeStamp);
      }
      running.set(false);
      long totalTime = (timeStamp[1] - timeStamp[0]) / 1000;
      System.out.println("Produced a total of: " + successfulEvents.get() + " events in " + totalTime + " secs (rate=" + ((double)numberOfEvents / (double)totalTime) + " e/s).");
      if (runningStateListener != null)
        runningStateListener.onStateChange(RunningState.STOPPED);
    }
  }
  
  @Override
  public void destroy()
  {
    super.destroy();
    events.clear();

    executor.shutdown();
    executor = null;
    
    producers.clear();
    producers = null;

  }
  
  public class WorkerThread implements Runnable {

    private int eventIndex;
    private int numEventsToSend;

    public WorkerThread(int eventIndex, int numEventsToSend){
        this.eventIndex = eventIndex;
        this.numEventsToSend = numEventsToSend;
    }

    @Override
    public void run() {
      //System.out.println("worker thread " + Thread.currentThread().getId());
        sendEvents(eventIndex, numEventsToSend);
    }

    private void sendEvents(int eventIndex, int numEventsToSend)
    {
      if(successfulEvents.get()>= numberOfEvents)
      {
        more = false;
        return;
      }
      //System.out.println("sendEvents is called.  eventIndex is " + eventIndex + "  numeventstosend is " + numEventsToSend);
      List<KeyedMessage<String, String>> messages = new ArrayList<KeyedMessage<String, String>>();
      try
      {
        for (int i=0; i < numEventsToSend; i++)
        {
          if (eventIndex == events.size())
            eventIndex = 0;
          String thisEvent = sync_events.get(eventIndex++);
          messages.add(new KeyedMessage<String, String>(topic, thisEvent.substring(0, thisEvent.indexOf(",")), thisEvent));
          successfulEvents.incrementAndGet();
          if (running.get() == false)
            break;
        }
        Producer<String, String> producer = producers.take();
        //System.out.println("producer is " + producer.toString());
        //System.out.println("Sending message.  Thread is " + Thread.currentThread().getId() + ".  Producer is " + producer.toString() + ".  Successful events is " + successfulEvents.get());
        producer.send(messages);
        producers.put(producer);
        
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }

    @Override
    public String toString(){
        return "";
    }
  }

}