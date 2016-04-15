package org.bootcamp.magic.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.bootcamp.magic.MagicBridgeComponent;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Create magic bridge.
 */
@Command(scope = "onos", name = "create-magic-bridge",description = "Create Magic Bridge")
public class CreateMagicBridgeCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "br", description = "bridge name",required = true, multiValued = false)
    private String br = null;

    @Override
    protected void execute() {
    	MagicBridgeComponent nodeManager = AbstractShellCommand.get(MagicBridgeComponent.class);
 	    log.info("Here we go: create-magic-bridge");
        nodeManager.createMagicBridge(br);
        log.info("Here we go: create-magic-bridge DONE!!!");
//        print(nodeManager.checkNodeInitState(node));
    }
}
