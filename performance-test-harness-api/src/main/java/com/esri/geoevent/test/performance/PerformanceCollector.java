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

import java.util.Map;

import com.esri.geoevent.test.performance.jaxb.Config;

public interface PerformanceCollector extends RunnableComponent
{
  public void init(Config config) throws TestException;
  public void validate() throws TestException;
  public void destroy();
  public void reset();
  public int getNumberOfEvents();
  public void setNumberOfEvents(int numberOfEvents);
  public void setNumberOfExpectedResults(int numberOfExpectedResults);
  public Map<Integer, Long[]> getTimeStamps();
  public void listenOnCommandPort(int port, boolean isLocal);
  public void disconnectCommandPort();
  public long getSuccessfulEvents();
  public long getSuccessfulEventBytes();
}