/*
 * Copyright 2016 Open Networking Laboratory
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
package org.bootcamp.magic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * A Magic Component works for OVS...
 */
@Component(immediate = true)
@Service(value = MagicBridgeComponent.class)
public class MagicBridgeComponent {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/***
	 * DPID Prefix.
	 */
	private static final int DPID_BEGIN = 3;

	/**
	 * OF port.
	 */
	private static final int OFPORT = 6653;

	/**
	 * APP Name.
	 */
	public static final String APP_ID = "org.bootcamp.magic";

	/**
	 * Bridge Name.
	 */
	public static final String MAGIC_BRIDGE = "magic";

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected DriverService driverService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected DeviceService deviceService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected ClusterService clusterService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected CoreService coreService;

	private FlowRuleInstaller ruleInstaller;

	/**
	 * Application Id.
	 */
	private ApplicationId appId;

	/**
	 * CLI admin service.
	 */
	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected DeviceAdminService adminService;

	@Activate
	protected void activate() {
		appId = coreService.registerApplication(APP_ID);
		ruleInstaller = new FlowRuleInstaller(appId, deviceService, flowRuleService);
		deviceService.addListener(ruleInstaller);
		log.info("Magic bridge started.");
	}

	@Deactivate
	protected void deactivate() {
		deviceService.removeListener(ruleInstaller);
		log.info("Magic bridge stopped.");
	}

	/**
	 * Creates an magic bridge for a given node.
	 *
	 * @param node
	 *            virtual switch node
	 */
	public void createMagicBridge(String ovs) {
		log.info("Create Step 1:");
		if (ovs == null || ovs.isEmpty()) {
			ovs = MAGIC_BRIDGE;
			log.info("Create Step 2:");
		}

		Iterable<Device> devices = deviceService.getAvailableDevices(Type.CONTROLLER);

		for (Device device : devices) {
			log.info("Create Step 3:");
			DeviceId id = device.id();
			String dpid = id.toString().substring(DPID_BEGIN);
			DriverHandler handler = driverService.createHandler(id);
			log.info("Create Step 4:id={}, dpid={}", id, dpid);
			BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
			log.info("Get bridge config: bridgeconfig is null?:{}", bridgeConfig==null);
			Collection<BridgeDescription> bridges = null;
			try {
				bridges = bridgeConfig.getBridges();
			} catch (Exception e) {
				log.error("exception", e);
				continue;
			}
			
			log.info("BridgeConfig is null?:{}", bridges==null);
			if(bridges == null || bridges.isEmpty()){
				log.info("Here Bridge is empty.");
				continue;
			}

			final String name = ovs;
			long count = bridges.stream().filter(bd -> bd.bridgeName().name().equals(name)).count();

			if (count > 1) {
				log.info("MAGIC brige is alredy exist.");
				return;
			}

			try {
				List<ControllerInfo> controllers = new ArrayList<>();
				Sets.newHashSet(clusterService.getNodes()).stream().forEach(controller -> {
					ControllerInfo ctrlInfo = new ControllerInfo(controller.ip(), OFPORT, "ptcp");
					controllers.add(ctrlInfo);
				});
				log.info("Create Step 6:");
//				controllers.forEach(action -> log.info("Controller info:{}", action.toString()));
				bridgeConfig.addBridge(BridgeName.bridgeName(ovs), dpid, controllers);
				log.info("MAGIC brige is created.");
			} catch (ItemNotFoundException e) {
				log.warn("Create magic bridge .....");
			}
		}

	}
}
