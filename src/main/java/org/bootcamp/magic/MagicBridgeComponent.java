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
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Device.Type.SWITCH;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
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
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
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
    
    /**
     * APP Name.
     */
    public static final String APP_ID = "org.bootcamp.magic";
    
    /**
     * Bridge Name.
     * */
    public static final String MAGIC_BRIDGE = "magic";
    
    private static final int DPID_BEGIN = 3;
    private static final int OFPORT = 6653;
    
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
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;
    
    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();

    private final ExecutorService eventExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtncfg", "event-handler"));
    
    private OvsRuleInstaller ruleInstaller;
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

        ruleInstaller = new OvsRuleInstaller(appId, flowRuleService,
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
     * Creates an magic bridge for a given node.
     *
     * @param node virtual switch node
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
     * Returns OVSDB client for a given node.
     *
     * @param node cordvtn node
     * @return OVSDB client, or null if it fails to get OVSDB client
     */
    private OvsdbClientService getOvsdbClient(OvsNode node) {
        checkNotNull(node);

        OvsdbClientService ovsdbClient = controller.getOvsdbClient(
                new OvsdbNodeId(node.hostMgmtIp().ip(), node.ovsdbPort().toInt()));
        if (ovsdbClient == null) {
            log.trace("Couldn't find OVSDB client for {}", node.hostname());
        }
        return ovsdbClient;
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
//                setNodeState(node, getNodeState(node));
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
//                setNodeState(node, getNodeState(node));
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            OvsNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                log.debug("Integration Bridge is disconnected from {}", node.hostname());
//                setNodeState(node, NodeState.INCOMPLETE);
            }
        }
    }

    /**
     * Returns cordvtn node associated with a given OVSDB device.
     *
     * @param ovsdbId OVSDB device id
     * @return cordvtn node, null if it fails to find the node
     */
    private OvsNode getNodeByOvsdbId(DeviceId ovsdbId) {
        return getNodes().stream()
                .filter(node -> node.ovsdbId().equals(ovsdbId))
                .findFirst().orElse(null);
    }

    /**
     * Returns cordvtn node associated with a given integration bridge.
     *
     * @param bridgeId device id of integration bridge
     * @return cordvtn node, null if it fails to find the node
     */
    private OvsNode getNodeByBridgeId(DeviceId bridgeId) {
        return getNodes().stream()
                .filter(node -> node.intBrId().equals(bridgeId))
                .findFirst().orElse(null);
    }
    
    
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {

            NodeId leaderNodeId = leadershipService.getLeader(appId.name());
//            if (!Objects.equals(localNodeId, leaderNodeId)) {
//                // only the leader processes events
//                return;
//            }

            Device device = event.subject();
            ConnectionHandler<Device> handler =
                    (device.type().equals(SWITCH) ? bridgeHandler : ovsdbHandler);

            switch (event.type()) {

                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
//                    if (deviceService.isAvailable(device.id())) {
//                        eventExecutor.submit(() -> handler.connected(device));
//                    } else {
//                        eventExecutor.submit(() -> handler.disconnected(device));
//                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Reads cordvtn nodes from config file.
     */
    private void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

//        config.cordVtnNodes().forEach(this::addNode);
        // TODO remove nodes if needed
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(CordVtnConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    eventExecutor.execute(MagicBridgeComponent.this::readConfiguration);
                    break;
                default:
                    break;
            }
        }
    }

}
