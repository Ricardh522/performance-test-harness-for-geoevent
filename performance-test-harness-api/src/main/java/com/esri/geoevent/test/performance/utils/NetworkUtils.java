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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkUtils
{
	public static boolean isLocal(String hostName)
	{
		try
		{
			//get the address
			InetAddress address = InetAddress.getByName(hostName);
			
			// Check if the address is a valid special local or loop back
			if (address.isAnyLocalAddress() || address.isLoopbackAddress())
				return true;
	
			// Check if the address is defined on any interface
			try
			{
				return NetworkInterface.getByInetAddress(address) != null;
			}
			catch (SocketException e)
			{
				return false;
			}
		} catch(UnknownHostException error)
		{
			return false;
		}
	}
	
}
