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

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

import java.util.concurrent.ExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jline.internal.Log;

/**
 * A Magic Component works for OVS...
 */
@Component(immediate = true)
@Service(value = MagicBridgeComponent.class)
public class MagicBridgeComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * APP Name.
     */
    public static final String APP_ID = "org.bootcamp.magic";
    
    /**
     * Bridge Name.
     * */
    public static final String MAGIC_BRIDGE = "magic";
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;
    
    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    
    private final BridgeHandler bridgeHandler = new BridgeHandler();

    private final ExecutorService eventExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtncfg", "event-handler"));
    
    //
    private OvsRuleInstaller ruleInstaller;
    
    //
    private ApplicationId appId;
    
    /**
     * CLI admin service.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;
    
    @Activate
    protected void activate() {
        log.info("Started");
        ruleInstaller = new OvsRuleInstaller(appId, flowRuleService,deviceService,driverService,MAGIC_BRIDGE);
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
    public void createMagicBridge() {

        try {
        	Iterable<Device> devices = deviceService.getAvailableDevices(Type.SWITCH);
        	for(Device device : devices){
        		DeviceId id = device.id();
        		DriverHandler handler = driverService.createHandler(id);
        		Log.info("Device:{}, Type:{}, ID:{}, Handler:{}", device.manufacturer(), device.type(),id.toString(), handler==null);
//                BridgeConfig bridgeConfig =  handler.behaviour(BridgeConfig.class);
//                bridgeConfig.addBridge(BridgeName.bridgeName(MAGIC_BRIDGE), dpid, controllers);
        	}
           
        } catch (ItemNotFoundException e) {
            log.warn("Create magic bridge failed.");
        }
    }

    
    
    /**
     * Returns port name.
     *
     * @param port port
     * @return port name
     */
    private String getPortName(Port port) {
        return port.annotations().value("portName");
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
