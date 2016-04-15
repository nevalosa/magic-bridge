/*
 * Copyright 2015 Open Networking Laboratory
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

package org.bootcamp.magic.cli;

import org.apache.karaf.shell.commands.Command;
import org.bootcamp.magic.MagicBridgeComponent;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Lists all nodes registered to the service.
 */
@Command(scope = "onos", name = "ovs-nodes",
        description = "Lists all nodes registered magic service")
public class ListOvsNodeCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
    	if(true){
    		log.info("Here we go: ovs-nodes to list ovs nodes.");
    		return;
    	}
    	
    	MagicBridgeComponent nodeManager = AbstractShellCommand.get(MagicBridgeComponent.class);
    }
}
