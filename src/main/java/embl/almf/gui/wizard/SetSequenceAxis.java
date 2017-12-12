package embl.almf.gui.wizard;

import embl.almf.AxisTypes;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static embl.almf.ImageRegistrationParameters.*;

@Plugin(type = Command.class,
        menuPath = "Plugins>Image Registration",
        initializer = "init")
public class SetSequenceAxis extends DynamicCommand {

    @Parameter
    private Dataset dataset;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter
    private CommandService commandService2;

    @Override
    public void run()
    {

        Map< String, Object > parameters = new HashMap<>(  );
        parameters.put( "dataset" , dataset );
        parameters.put( INPUT_SEQUENCE_AXIS , getInfo().getInput( INPUT_SEQUENCE_AXIS ).getValue( this ) );

        commandService2.run( SetSequenceAxisParameters.class, true, parameters );
    }


    protected void initFields()
    {

        int n = dataset.numDimensions();

        // Figure out which axes we have
        //
        ArrayList< String > axisTypes
                = getAxisNamesAsStringList( dataset );

        // Create GUI
        //

        List< String > choices = Stream.of( AxisTypes.values() ).map( AxisTypes::name ).collect( Collectors.toList() );

        /*
        final MutableModuleItem<String> axisItem =
                addInput("message", String.class);
        axisItem.setPersisted( false );
        axisItem.setVisibility( ItemVisibility.MESSAGE );
        axisItem.setValue(this,
                "Please select the sequence dimension");
                */

        final MutableModuleItem<String> messageItem =
                addInput("Message", String.class);
        messageItem.setPersisted( false );
        messageItem.setVisibility( ItemVisibility.MESSAGE );
        messageItem.setValue(this, "aaaaaaaaaaa");

        final MutableModuleItem<String> typeItem =
                addInput( INPUT_SEQUENCE_AXIS, String.class);
        typeItem.setPersisted( false );
        typeItem.setLabel( INPUT_SEQUENCE_AXIS );
        typeItem.setChoices( axisTypes );
        typeItem.setValue(this, ""+ AxisTypes.Sequence );

    }


    public static void main( String ... args ) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Load dataset
        //
        final File file =
                new File( "/Users/tischi/Documents/fiji-plugin-imageRegistration--data/2d_t_2ch_drift_synthetic_blur.tif");

        Dataset dataset = ij.scifio().datasetIO().open( file.getPath() );
        ij.ui().show( dataset );

        ij.command().run( SetSequenceAxis.class, true );

    }


}
