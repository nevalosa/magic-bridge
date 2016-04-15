package org.bootcamp.magic.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;


/**
 * Install Default Flow Command.
 */
@Command(scope = "onos", name = "install-default-flow",description = "Install default flow.")
public class InstallDefaultFlowCommand extends AbstractShellCommand {

   @Override
    protected void execute() {
	   
	   log.info("Here we go: Install Default Flow: install-default-flow.");

    }
}
