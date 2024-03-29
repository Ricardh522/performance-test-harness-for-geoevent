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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@XmlRootElement(name = "Fixture")
public class Fixture implements Applicable<Fixture>
{
	private String name;
	private DefaultConfig defaultConfig;
	private ProducerConfig producerConfig;
	private ConsumerConfig consumerConfig;
	private Simulation simulation;
	private ProvisionerConfig provisionerConfig;
	
	public Fixture()
	{
	}
	
	public Fixture(String name)
	{
		this.name = name;
	}
	
	@XmlAttribute
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	
	@XmlElement(name = "Simulation")
	public Simulation getSimulation()
	{
		return simulation;
	}
	public void setSimulation(Simulation simulation)
	{
		this.simulation = simulation;
	}
	
	@XmlElement(name = "DefaultSharedConfig", required = false)
	public DefaultConfig getDefaultConfig()
	{
		return defaultConfig;
	}
	public void setDefaultConfig(DefaultConfig defaultConfig)
	{
		this.defaultConfig = defaultConfig;
	}
	
	@XmlElement(name = "ProducerConfig")
	public ProducerConfig getProducerConfig()
	{
		return producerConfig;
	}
	public void setProducerConfig(ProducerConfig producerConfig)
	{
		this.producerConfig = producerConfig;
	}
	
	@XmlElement(name = "ConsumerConfig")
	public ConsumerConfig getConsumerConfig()
	{
		return consumerConfig;
	}
	public void setConsumerConfig(ConsumerConfig consumerConfig)
	{
		this.consumerConfig = consumerConfig;
	}
	
	@XmlElement(name = "ProvisionerConfig", required = false)
	public ProvisionerConfig getProvisionerConfig()
	{
		return provisionerConfig;
	}
	public void setProvisionerConfig(ProvisionerConfig provisionerConfig)
	{
		this.provisionerConfig = provisionerConfig;
	}
	
	@Override
	public void apply(Fixture fixture)
	{
		if( fixture == null )
			return;
		
		if( StringUtils.isEmpty( getName() ) )
			setName( fixture.getName() );
		if( fixture.getConsumerConfig() != null )
		{
			if( getConsumerConfig() == null )
				setConsumerConfig((ConsumerConfig)fixture.getConsumerConfig().copy());
			else
				getConsumerConfig().apply(fixture.getConsumerConfig());
		}
		if( fixture.getDefaultConfig() != null )
		{
			if( getDefaultConfig() == null )
				setDefaultConfig((DefaultConfig)fixture.getDefaultConfig().copy());
			else
				getDefaultConfig().apply(fixture.getDefaultConfig());
		}
		if( fixture.getProducerConfig() != null )
		{
			if( getProducerConfig() == null )
				setProducerConfig((ProducerConfig)fixture.getProducerConfig().copy());
			else
				getProducerConfig().apply(fixture.getProducerConfig());
		}
		if( fixture.getSimulation() != null )
		{
			if( getSimulation() == null )
				setSimulation(fixture.getSimulation().copy());
			else
				getSimulation().apply(fixture.getSimulation());
		}
		if( fixture.getProvisionerConfig() != null )
		{
			if( getProvisionerConfig() == null )
				setProvisionerConfig((ProvisionerConfig)fixture.getProvisionerConfig().copy());
			else
				getProvisionerConfig().apply(fixture.getProvisionerConfig());
		}
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof Fixture))
      return false;
		
		Fixture fixture = (Fixture) obj;
    if (!ObjectUtils.equals(getConsumerConfig(), fixture.getConsumerConfig()))
      return false;
    if (!ObjectUtils.equals(getDefaultConfig(), fixture.getDefaultConfig()))
      return false;
    if (!ObjectUtils.equals(getName(), fixture.getName()))
      return false;
    if (!ObjectUtils.equals(getProducerConfig(), fixture.getProducerConfig()))
      return false;
    if (!ObjectUtils.equals(getSimulation(), fixture.getSimulation()))
      return false;
    if (!ObjectUtils.equals(getProvisionerConfig(), fixture.getProvisionerConfig()))
      return false;
    
    return true;
	}
	
	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
