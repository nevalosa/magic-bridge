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
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import jline.internal.Log;

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
    public static final String APP_ID = "org.foo.app";
    
    /**
     * Bridge Name.
     * */
    public static final String MAGIC_BRIDGE = "magic";
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;
    
//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected FlowObjectiveService flowObjectiveService;
    
//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected PacketService packetService;
    
    //Installer.
    private FlowRuleInstaller ruleInstaller;
    
    /**
     * Application Id.
     */
    private ApplicationId appId;
    
    /**
     * Local node id.
     */
    private NodeId localNodeId;
    
    /**
     * CLI admin service.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;
    
    @Activate
    protected void activate() {
        log.info("Started");
        ruleInstaller = new FlowRuleInstaller(deviceService, flowRuleService);
        deviceService.addListener(ruleInstaller);
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }
    
    
    
    
    /**
     * Creates an magic bridge for a given node.
     *
     * @param node virtual switch node
     */
    public void createMagicBridge(String ovs) {
    	Log.info("Create MAGIC Bridge.");
    	
    	if(ovs == null || ovs.isEmpty()){
    		ovs = MAGIC_BRIDGE;
    	}
    	
        try {
        	Iterable<Device> devices = deviceService.getAvailableDevices(Type.CONTROLLER);
        	
        	for(Device device : devices){
        		
        		DeviceId id = device.id();
        		String dpid = id.toString().substring(DPID_BEGIN);
        		DriverHandler handler = driverService.createHandler(id);
                BridgeConfig bridgeConfig =  handler.behaviour(BridgeConfig.class);
                Collection<BridgeDescription> bridges = bridgeConfig.getBridges();
                
                final String name = ovs;
                long count = bridges.stream()
                	     .filter( bd -> bd.bridgeName().name().equals(name))
                	     .count();
                
                if(count > 1){
                	log.info("MAGIC brige is alredy exist.");
                }
                
                
                List<ControllerInfo> controllers = new ArrayList<>();
                Sets.newHashSet(clusterService.getNodes()).stream()
                        .forEach(controller -> {
                            ControllerInfo ctrlInfo = new ControllerInfo(controller.ip(), OFPORT, "tcp");
                            controllers.add(ctrlInfo);
                        });
                controllers.forEach(action -> log.info("Controller info:{}", action.toString()));
                bridgeConfig.addBridge(BridgeName.bridgeName(ovs), dpid, controllers);
                log.info("MAGIC brige is created.");
        	}
        } catch (ItemNotFoundException e) {
            log.warn("Create magic bridge failed.");
        }
    }
    

    /**
     * Returns connection state of OVSDB server for a given node.
     *
     * @param node cordvtn node
     * @return true if it is connected, false otherwise
     */
//    private boolean isOvsdbConnected(OvsNode node) {
//        checkNotNull(node);
//
//        OvsdbClientService ovsdbClient = getOvsdbClient(node);
//        return deviceService.isAvailable(node.ovsdbId()) &&
//                ovsdbClient != null && ovsdbClient.isConnected();
//    }
    
    /**
     * Returns OVSDB client for a given node.
     *
     * @param node cordvtn node
     * @return OVSDB client, or null if it fails to get OVSDB client
     */
//    private OvsdbClientService getOvsdbClient(OvsNode node) {
//        checkNotNull(node);
//
//        OvsdbClientService ovsdbClient = controller.getOvsdbClient(
//                new OvsdbNodeId(node.hostMgmtIp().ip(), node.ovsdbPort().toInt()));
//        if (ovsdbClient == null) {
//            log.trace("Couldn't find OVSDB client for {}", node.hostname());
//        }
//        return ovsdbClient;
//    }


    /**
     * Connects to OVSDB server for a given node.
     *
     * @param node cordvtn node
     */
//    private void connectOvsdb(OvsNode node) {
//        checkNotNull(node);
//
//        if (!nodeStore.containsKey(node.hostname())) {
//            log.warn("Node {} does not exist", node.hostname());
//            return;
//        }
//
//        if (!isOvsdbConnected(node)) {
//            controller.connect(node.hostMgmtIp().ip(), node.ovsdbPort());
//        }
//    }

    
    private class OvsdbHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
        }

        @Override
        public void disconnected(Device device) {
            if (!deviceService.isAvailable(device.id())) {
                log.debug("Device {} is disconnected", device.id());
                adminService.removeDevice(device.id());
            }
        }
    }

    private class BridgeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
        }

        @Override
        public void disconnected(Device device) {
        }
    }
    
}
