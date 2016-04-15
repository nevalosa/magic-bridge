package org.bootcamp.magic.cli;

import org.apache.karaf.shell.commands.Argument;
import org.bootcamp.magic.MagicBridgeComponent;
import org.bootcamp.magic.OvsNode;
import org.onosproject.cli.AbstractShellCommand;

public class InstallDefaultFlowCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    private String hostname = null;

    @Override
    protected void execute() {
    	MagicBridgeComponent nodeManager = AbstractShellCommand.get(MagicBridgeComponent.class);
        OvsNode node = nodeManager.getNodes()
                .stream()
                .filter(n -> n.hostname().equals(hostname))
                .findFirst()
                .orElse(null);

        if (node == null) {
            print("Cannot find %s from registered nodes", hostname);
            return;
        }

//        print(nodeManager.checkNodeInitState(node));
    }
}
