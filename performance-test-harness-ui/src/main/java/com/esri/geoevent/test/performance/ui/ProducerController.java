/*
  Copyright 1995-2015 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
 */
package com.esri.geoevent.test.performance.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.math.NumberUtils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.esri.geoevent.test.performance.Mode;
import com.esri.geoevent.test.performance.PerformanceCollector;
import com.esri.geoevent.test.performance.Protocol;
import com.esri.geoevent.test.performance.activemq.ActiveMQEventProducer;
import com.esri.geoevent.test.performance.kafka.KafkaEventProducer;
import com.esri.geoevent.test.performance.rabbitmq.RabbitMQEventProducer;
import com.esri.geoevent.test.performance.streamservice.StreamServiceEventProducer;
import com.esri.geoevent.test.performance.tcp.TcpServerEventProducer;
import com.esri.geoevent.test.performance.tcp.TcpEventProducer;
import com.esri.geoevent.test.performance.websocket.WebsocketEventProducer;
import com.esri.geoevent.test.performance.websocket.WebsocketServerEventProducer;

public class ProducerController extends PerformanceCollectorController
{
	// statics
	private static final int DEFAULT_COMMAND_PORT = 5010;
	private static final int DEFAULT_SERVER_PORT = 5785;

	// our producer worker
	private PerformanceCollector producer;
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{		
		super.initialize(location, resources);
		
		titleLabel.setText( Mode.Producer.toString() );
		protocol.setItems( getProtocolList());
		port.setText(String.valueOf(DEFAULT_COMMAND_PORT));
		serverPort.setText(String.valueOf(DEFAULT_SERVER_PORT));
		
		loadState();
	}
	
	@Override
	protected void start()
	{
		// stop the producer if we are not stopped already
		if( producer != null )
			stop();
		
		int serverPortInt = NumberUtils.toInt(serverPort.getText(), DEFAULT_SERVER_PORT);
		switch (protocol.getValue())
		{
			case TCP:
				producer = new TcpEventProducer();
				break;
			case TCP_SERVER:
				producer = new TcpServerEventProducer(serverPortInt);
				break;
			case WEBSOCKETS:
				producer = new WebsocketEventProducer();
				break;
			case WEBSOCKET_SERVER:
				producer = new WebsocketServerEventProducer(serverPortInt);
				break;
			case ACTIVE_MQ:
				producer = new ActiveMQEventProducer();
				break;
			case RABBIT_MQ:
				producer = new RabbitMQEventProducer();
				break;
			case STREAM_SERVICE:
				producer = new StreamServiceEventProducer();
				break;
			case KAFKA:
			  producer = new KafkaEventProducer();
			  break;
			default:
				return;
		}
		
		int portNumber = NumberUtils.toInt(port.getText(), DEFAULT_COMMAND_PORT);
		producer.listenOnCommandPort(portNumber, true);
	}
	
	@Override
	protected void stop()
	{
		if( producer != null )
		{
			producer.disconnectCommandPort();
			producer = null;
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
		list.add(Protocol.KAFKA);
		list.add(Protocol.RABBIT_MQ);
		list.add(Protocol.STREAM_SERVICE);
		list.add(Protocol.TCP);
		list.add(Protocol.TCP_SERVER);
		list.add(Protocol.WEBSOCKETS);
		list.add(Protocol.WEBSOCKET_SERVER);
		return FXCollections.observableList(list);
	}
}
