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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.bootcamp.magic.MagicBridgeComponent;

/**
 * Check OVS node status.
 */
@Command(scope = "onos", name = "magic-node-check",description = "Shows detailed node init state")
public class StateCheckCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    private String hostname = null;

    @Override
    protected void execute() {
    	log.info("Here we go: magic-node-check {}", hostname);
//    	MagicBridgeComponent nodeManager = AbstractShellCommand.get(MagicBridgeComponent.class);

//        print(nodeManager.checkNodeInitState(node));
    }
}
