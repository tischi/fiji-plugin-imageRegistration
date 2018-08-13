package de.embl.cba.registration.commands;

import de.embl.cba.registration.drosophila.DapiRegistrationCommand;
import edu.mines.jtk.util.ArrayMath;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;


@Plugin( type = InteractiveCommand.class )
public class TestInteractiveCommand extends InteractiveCommand
{
	@Parameter( label = "Run", callback = "execute" )
	private Button runButton;

	public void run()
	{
		IJ.log( "Run" );
	}


	public void execute()
	{
		IJ.log( "Execute" );
	}

	public static void main(final String... args) throws Exception
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( TestInteractiveCommand.class, true );
	}

}


