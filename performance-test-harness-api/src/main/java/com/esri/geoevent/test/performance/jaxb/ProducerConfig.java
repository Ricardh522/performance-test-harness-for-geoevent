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
package com.esri.geoevent.test.performance.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@XmlRootElement(name = "ProducerConfig")
public class ProducerConfig extends AbstractConfig
{
	protected final int DEFAULT_NUM_OF_CONNECTIONS = 1;	
	protected final int DEFAULT_COMMAND_PORT = 5010;

	private List<RemoteHost> producers = new ArrayList<RemoteHost>();
	
	public ProducerConfig()
	{
		setCommandPort(DEFAULT_COMMAND_PORT);
	}
	
	@XmlElementWrapper(name = "Producers", required=false)
	@XmlElement(name = "Producer", required=false)
	public List<RemoteHost> getProducers()
	{
		return producers;
	}
	public void setProducers(List<RemoteHost> producers)
	{
		this.producers = producers;
	}	
	
	@XmlTransient
	public int getNumOfConnections()
	{
		return (getProducers() != null) ? getProducers().size() : DEFAULT_NUM_OF_CONNECTIONS;
	}
	
	/**
	 * This method applies all of the parameter's properties if and only if the
	 * existing object's properties are null or are using the default values.
	 * 
	 * @param config of type {@link AbstractConfig}
	 */
	@Override
	public void apply(AbstractConfig config)
	{
		if( config == null)
			return;
		
		// apply ProducerConfig related
		if( config instanceof ProducerConfig )
		{
			ProducerConfig producerConfig = (ProducerConfig) config;
			if( getCommandPort() == DEFAULT_COMMAND_PORT )
				setCommandPort( config.getCommandPort() );
			if( producerConfig.getProducers() != null && ! producerConfig.getProducers().isEmpty() )
			{
				if( getProducers().isEmpty() )
					getProducers().addAll(producerConfig.getProducers());
				else
				{
					for(RemoteHost producer : producerConfig.getProducers() )
					{
						// check if we don't have the property - if we do not then add it
						RemoteHost existingProducer = getProducerByName( producer.getHost() );
						if( existingProducer == null )
						{
							getProducers().add( producer );
						}
					}
				}
			}
		}
		super.apply(config);
	}
	
	/**
	 * Fetches the producer within the {@link ProducerConfig#getProducers()} list by host name.
	 * 
	 * @param hostName of the producer to fetch
	 * @return the {@link RemoteHost} found or <code>null</code> otherwise.
	 */
	public RemoteHost getProducerByName(String hostName)
	{
		if( getProducers() == null || StringUtils.isEmpty(hostName) )
			return null;
		
		for( RemoteHost producer : getProducers() )
		{
			if( hostName.equalsIgnoreCase( producer.getHost() ) )
				return producer;
		}
		return null;
	}
	
	@Override
	public Config copy()
	{
		ProducerConfig copy = new ProducerConfig();
		copy.setCommandPort(getCommandPort());
		copy.setProducers(new ArrayList<RemoteHost>(getProducers()));
		copy.setHost(getHost());
		copy.setProperties(new ArrayList<Property>(getProperties()));
		copy.setProtocol(getProtocol());
		return copy;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof ProducerConfig))
      return false;
		
		ProducerConfig producerConfig = (ProducerConfig) obj;
    if (!ObjectUtils.equals(getProducers(), producerConfig.getProducers()))
      return false;
    
    return super.equals(obj);
	}
}
