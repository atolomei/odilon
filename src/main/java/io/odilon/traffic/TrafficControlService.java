/*
 * Odilon Object Storage
 * (C) Novamens 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.odilon.traffic;


import io.odilon.model.ServerConstant;
import io.odilon.service.SystemService;


/**
 * <p>the goal of this parameters is to prevent overload of the server capacity
 * default vaule is 10. constant -> {@link ServerConstant.DEFAULT_TRAFFIC_TOKENS} ,
 * </p>
 */
public interface TrafficControlService extends SystemService {
	public TrafficPass getPass();
	public void release(TrafficPass pass);
}
