package embl.almf.wizard;

import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class)
public class GuiNextStep implements Command {

    @Parameter
    private String sequenceAxis;

    @Parameter
    private Integer transformationAxis;


    @Override
    public void run()
    {

    }
}
