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
package com.esri.geoevent.test.performance;

import org.apache.commons.lang3.ObjectUtils;

public class Request
{
	private RequestType type;
	private String data;

	public Request()
	{
	}
	
	public Request(RequestType type)
	{
		this.type = type;
	}
	
	public Request(RequestType type, String data)
	{
		this.type = type;
		this.data = data;
	}
	
	public String getData()
	{
		return data;
	}
	
	public void setData(String data)
	{
		this.data = data;
	}
	
	public RequestType getType()
	{
		return type;
	}
	
	public void setType(RequestType type)
	{
		this.type = type;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof Request))
      return false;
		
		Request request = (Request) obj;
    if (!ObjectUtils.equals(getType(), request.getType()))
      return false;
    if (!ObjectUtils.equals(getData(), request.getData()))
      return false;
    
		return super.equals(obj);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append( getClass().getSimpleName() );
		builder.append( "[");
		builder.append( "type=");
		builder.append( getType() );
		if( getData() != null )
		{
			builder.append( ",data=");
			builder.append( getData() );
		}
		builder.append( "]");
		return builder.toString();
	}
}
