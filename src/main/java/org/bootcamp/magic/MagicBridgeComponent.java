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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Skeletal ONOS application component.
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
    
    /**
     * Highest Priority
     */
    private static final int H_PRIORITY = 65535;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;
  
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ApplicationId appId;
    /**
     * CLI admin service.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;
    
    /**
     * Ovs node cache.
     */
    private ConsistentMap<String, OvsNode> nodeStore;
    
    private static final KryoNamespace.Builder NODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KryoNamespaces.MISC)
            .register(OvsNode.class)
            .register(NodeState.class)
            .register(SshAccessInfo.class)
            .register(NetworkAddress.class);
    
    private enum NodeState implements OvsNodeState {

        INIT {
            @Override
            public void process(MagicBridgeComponent nodeManager, OvsNode node) {
                if (!nodeManager.isOvsdbConnected(node)) {
                    nodeManager.connectOvsdb(node);
                } else {
                    nodeManager.createMagicBridge(node);
                }
            }
        },
        BRIDGE_CREATED {
            @Override
            public void process(MagicBridgeComponent nodeManager, OvsNode node) {
                if (!nodeManager.isOvsdbConnected(node)) {
                    nodeManager.connectOvsdb(node);
                } 
//                else {
//                    nodeManager.createTunnelInterface(node);
//                    nodeManager.addDataPlaneInterface(node);
//                }
            }
        },
//        COMPLETE {
//            @Override
//            public void process(MagicBridgeComponent nodeManager, OvsNode node) {
//                nodeManager.postInit(node);
//            }
//        },
        INCOMPLETE {
            @Override
            public void process(MagicBridgeComponent nodeManager, OvsNode node) {
            }
        };

        public abstract void process(MagicBridgeComponent nodeManager, OvsNode node);
    }

    @Activate
    protected void activate() {
        log.info("Started");
        appId = coreService.getAppId(APP_ID);
//        localNodeId = clusterService.getLocalNode().id();
//        leadershipService.runForLeadership(appId.name());

        nodeStore = storageService.<String, OvsNode>consistentMapBuilder()
                .withSerializer(Serializer.using(NODE_SERIALIZER.build()))
                .withName("cordvtn-nodestore")
                .withApplicationId(appId)
                .build();

        ruleInstaller = new CordVtnRuleInstaller(appId, flowRuleService,
                                                 deviceService,
                                                 driverService,
                                                 groupService,
                                                 configRegistry,
                                                 MAGIC_BRIDGE);

        deviceService.addListener(deviceListener);
        configService.addListener(configListener);
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }
    
    /**
     * Returns all nodes known to the service.
     *
     * @return list of nodes
     */
    public List<OvsNode> getNodes() {
        return nodeStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if integration bridge exists and available.
     *
     * @param node cordvtn node
     * @return true if the bridge is available, false otherwise
     */
    private boolean isBrIntCreated(OvsNode node) {
        return (deviceService.getDevice(node.intBrId()) != null
                && deviceService.isAvailable(node.intBrId()));
    }
    
    /**
     * Creates an integration bridge for a given node.
     *
     * @param node cordvtn node
     */
    private void createMagicBridge(OvsNode node) {
        if (isBrIntCreated(node)) {
            return;
        }

        List<ControllerInfo> controllers = new ArrayList<>();
        Sets.newHashSet(clusterService.getNodes()).stream()
                .forEach(controller -> {
                    ControllerInfo ctrlInfo = new ControllerInfo(controller.ip(), OFPORT, "tcp");
                    controllers.add(ctrlInfo);
                });

        String dpid = node.intBrId().toString().substring(DPID_BEGIN);

        try {
            DriverHandler handler = driverService.createHandler(node.ovsdbId());
            BridgeConfig bridgeConfig =  handler.behaviour(BridgeConfig.class);
            bridgeConfig.addBridge(BridgeName.bridgeName(MAGIC_BRIDGE), dpid, controllers);
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create integration bridge on {}", node.hostname());
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
    private boolean isOvsdbConnected(OvsNode node) {
        checkNotNull(node);

        OvsdbClientService ovsdbClient = getOvsdbClient(node);
        return deviceService.isAvailable(node.ovsdbId()) &&
                ovsdbClient != null && ovsdbClient.isConnected();
    }

    /**
     * Connects to OVSDB server for a given node.
     *
     * @param node cordvtn node
     */
    private void connectOvsdb(OvsNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node.hostname())) {
            log.warn("Node {} does not exist", node.hostname());
            return;
        }

        if (!isOvsdbConnected(node)) {
            controller.connect(node.hostMgmtIp().ip(), node.ovsdbPort());
        }
    }

    
    private class OvsdbHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            OvsNode node = getNodeByOvsdbId(device.id());
            if (node != null) {
                setNodeState(node, getNodeState(node));
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
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
            OvsNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                setNodeState(node, getNodeState(node));
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            OvsNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                log.debug("Integration Bridge is disconnected from {}", node.hostname());
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }

        /**
         * Handles port added situation.
         * If the added port is tunnel or data plane interface, proceed to the remaining
         * node initialization. Otherwise, do nothing.
         *
         * @param port port
         */
        public void portAdded(Port port) {
            OvsNode node = getNodeByBridgeId((DeviceId) port.element().id());
            String portName = getPortName(port);

            if (node == null) {
                log.debug("{} is added to unregistered node, ignore it.", portName);
                return;
            }

            log.debug("Port {} is added to {}", portName, node.hostname());

            if (portName.startsWith(VPORT_PREFIX)) {
                if (isNodeStateComplete(node)) {
                    cordVtnService.addServiceVm(node, getConnectPoint(port));
                } else {
                    log.debug("VM is detected on incomplete node, ignore it.", portName);
                }
            } else if (portName.contains(DEFAULT_TUNNEL) || portName.equals(node.dpIntf())) {
                setNodeState(node, getNodeState(node));
            }
        }

        /**
         * Handles port removed situation.
         * If the removed port is tunnel or data plane interface, proceed to the remaining
         * node initialization.Others, do nothing.
         *
         * @param port port
         */
        public void portRemoved(Port port) {
            OvsNode node = getNodeByBridgeId((DeviceId) port.element().id());
            String portName = getPortName(port);

            if (node == null) {
                return;
            }

            log.debug("Port {} is removed from {}", portName, node.hostname());

            if (portName.startsWith(VPORT_PREFIX)) {
                if (isNodeStateComplete(node)) {
                    cordVtnService.removeServiceVm(getConnectPoint(port));
                } else {
                    log.debug("VM is vanished from incomplete node, ignore it.", portName);
                }
            } else if (portName.contains(DEFAULT_TUNNEL) || portName.equals(node.dpIntf())) {
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }
    }

}
