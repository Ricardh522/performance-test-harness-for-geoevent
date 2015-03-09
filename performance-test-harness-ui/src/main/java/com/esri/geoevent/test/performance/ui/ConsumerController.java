package com.esri.geoevent.test.performance.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.apache.commons.lang3.math.NumberUtils;

import com.esri.geoevent.test.performance.Mode;
import com.esri.geoevent.test.performance.PerformanceCollector;
import com.esri.geoevent.test.performance.Protocol;
import com.esri.geoevent.test.performance.activemq.ActiveMQEventConsumer;
import com.esri.geoevent.test.performance.rabbitmq.RabbitMQEventConsumer;
import com.esri.geoevent.test.performance.streamservice.StreamServiceEventConsumer;
import com.esri.geoevent.test.performance.tcp.ClusterableTcpEventConsumer;
import com.esri.geoevent.test.performance.tcp.TcpEventConsumer;
import com.esri.geoevent.test.performance.websocket.WebsocketEventConsumer;

public class ConsumerController extends PerformanceCollectorController
{
	// statics
	private static final int DEFAULT_COMMAND_PORT = 5020;
	private static final int DEFAULT_SERVER_PORT = 5775;
	
	// our producer worker
	private PerformanceCollector consumer;
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{		
		super.initialize(location, resources);
		
		titleLabel.setText( Mode.Consumer.toString() );
		protocol.setItems( getProtocolList());
		port.setText(String.valueOf(DEFAULT_COMMAND_PORT));
		serverPort.setText(String.valueOf(DEFAULT_SERVER_PORT));
		
		loadState();
	}
	
	@Override
	protected void start()
	{
		// stop the producer if we are not stopped already
		if( consumer != null )
			stop();
		
		switch (protocol.getValue())
		{
			case TCP:
				consumer = new TcpEventConsumer();
				break;
			case TCP_SERVER:
				int connectionPort = NumberUtils.toInt(serverPort.getText(), DEFAULT_SERVER_PORT);
				consumer = new ClusterableTcpEventConsumer(connectionPort);
			break;
			case WEBSOCKETS:
				consumer = new WebsocketEventConsumer();
				break;
			case ACTIVE_MQ:
				consumer = new ActiveMQEventConsumer();
				break;
			case RABBIT_MQ:
				consumer = new RabbitMQEventConsumer();
				break;
			case STREAM_SERVICE:
				consumer = new StreamServiceEventConsumer();
				break;
			default:
				return;
		}
		
		int portNumber = NumberUtils.toInt(port.getText(), DEFAULT_COMMAND_PORT);
		consumer.listenOnCommandPort(portNumber, true);
	}
	
	@Override
	protected void stop()
	{
		if( consumer != null )
		{
			consumer.disconnectCommandPort();
			consumer = null;
		}
	}
	
	@Override
	protected void saveState()
	{
		Protocol selectedProtocol = protocol.getValue();
		int portNumber = NumberUtils.toInt(port.getText(), DEFAULT_COMMAND_PORT);
		int serverPortNumber = NumberUtils.toInt(serverPort.getText(), DEFAULT_SERVER_PORT);
		
		Preferences preferences = Preferences.userNodeForPackage(ConsumerController.class);
		preferences.put("protocol", selectedProtocol.toString());
		preferences.put("port", String.valueOf(portNumber));
		preferences.put("serverPort", String.valueOf(serverPortNumber));
	}
	
	@Override
	protected void loadState()
	{
		Preferences preferences = Preferences.userNodeForPackage(ConsumerController.class);
		Protocol selectedProtocol = Protocol.fromValue(preferences.get("protocol", Protocol.TCP.toString()));
		int portNumber = NumberUtils.toInt(preferences.get("port", String.valueOf(DEFAULT_COMMAND_PORT)));
		int serverPortNumber = NumberUtils.toInt(preferences.get("serverPort", String.valueOf(DEFAULT_SERVER_PORT)));
		
		protocol.setValue(selectedProtocol);
		port.setText(String.valueOf(portNumber));
		serverPort.setText(String.valueOf(serverPortNumber));
	}
	
	private ObservableList<Protocol> getProtocolList()
	{
		ArrayList<Protocol> list = new ArrayList<Protocol>();
		list.add(Protocol.ACTIVE_MQ);
		list.add(Protocol.RABBIT_MQ);
		list.add(Protocol.STREAM_SERVICE);
		list.add(Protocol.TCP);
		list.add(Protocol.TCP_SERVER);
		list.add(Protocol.WEBSOCKETS);
		return FXCollections.observableList(list);
	}
}
