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
package com.esri.geoevent.test.performance.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esri.geoevent.test.performance.Protocol;
import com.esri.geoevent.test.performance.Request;
import com.esri.geoevent.test.performance.RequestType;
import com.esri.geoevent.test.performance.Response;
import com.esri.geoevent.test.performance.ResponseType;
import com.esri.geoevent.test.performance.jaxb.AbstractConfig;
import com.esri.geoevent.test.performance.jaxb.Config;
import com.esri.geoevent.test.performance.jaxb.ConsumerConfig;
import com.esri.geoevent.test.performance.jaxb.DefaultConfig;
import com.esri.geoevent.test.performance.jaxb.Fixture;
import com.esri.geoevent.test.performance.jaxb.ProducerConfig;
import com.esri.geoevent.test.performance.jaxb.Property;
import com.esri.geoevent.test.performance.jaxb.RampTest;
import com.esri.geoevent.test.performance.jaxb.RemoteHost;
import com.esri.geoevent.test.performance.jaxb.Simulation;
import com.esri.geoevent.test.performance.jaxb.StressTest;
import com.esri.geoevent.test.performance.jaxb.Test;
import com.esri.geoevent.test.performance.jaxb.TimeTest;

/**
 * Utility class to serialize Java objects to and from Strings. This class uses {@link Kryo} for
 * all serialization.  
 * 
 * @see <a href="https://github.com/EsotericSoftware/kryo">https://github.com/EsotericSoftware/kryo</a>
 *
 */
public class KryoUtils
{
	public static <T> T fromString(String data, Class<T> type)
	{
		if( StringUtils.isEmpty(data) )
			return null;
		
		Kryo kryo = setupKryo();
		Input input = new Input(new ByteArrayInputStream(data.getBytes()));
		T returnObj = kryo.readObject(input, type);
		input.close();
		return returnObj;
	}
	
	public static <T> String toString(T t, Class<T> type)
	{
		if( t == null )
			return null;
		
		Kryo kryo = setupKryo();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Output output = new Output(baos);
		kryo.writeObject(output, t);
		output.close();
		
		return new String(baos.toByteArray());
	}
	
	/**
	 * Setup the Kryo serializer. <b>NOTE:</b> We are doing a manual registration of 
	 * all of the classes we would want the serializer to know about ahead of time. This is
	 * done for optimization reasons.
	 *   
	 * @return {@link Kryo}
	 * @see <a href="https://github.com/EsotericSoftware/kryo">https://github.com/EsotericSoftware/kryo</a>
	 */
	private static Kryo setupKryo()
	{
		// setup the serializer
		Kryo kryo = new Kryo();
		kryo.register(Fixture.class);
		kryo.register(DefaultConfig.class);
		kryo.register(ProducerConfig.class);
		kryo.register(ConsumerConfig.class);
		kryo.register(AbstractConfig.class);
		kryo.register(Config.class);
		kryo.register(Simulation.class);
		kryo.register(Protocol.class);
		kryo.register(Property.class);
		kryo.register(RemoteHost.class);
		kryo.register(RampTest.class);
		kryo.register(StressTest.class);
		kryo.register(TimeTest.class);
		kryo.register(Test.class);
		kryo.register(ArrayList.class);
		kryo.register(Request.class);
		kryo.register(Response.class);
		kryo.register(RequestType.class);
		kryo.register(ResponseType.class);
		return kryo;
	}
	
}
