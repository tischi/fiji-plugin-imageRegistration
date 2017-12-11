package embl.almf.wizard;

import embl.almf.ImageRegistrationParameters;
import embl.almf.RegistrationAxisType;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.*;

@Plugin(type = Command.class,
        menuPath = "Plugins>Image Registration",
        initializer = "initFields")
public class SetSequenceAxis extends DynamicCommand {

    @Parameter
    private Dataset dataset;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;


    //@Parameter
    //private CommandService commandService;

    @Override
    public void run()
    {
        Map< String, Object > parameters = new HashMap<>(  );
        parameters.put( "dataset" , dataset);
        //parameters.put( "sequenceAxis" , SEQUENCE_AXIS);

        //commandService.run( GuiNextStep.class, true, parameters );
    }


    protected void initFields()
    {

        int n = dataset.numDimensions();

        // Figure out which axes we have
        //
        ArrayList< String > axisTypes
                = ImageRegistrationParameters.getAxisTypesAsStringList( dataset );

        // Create GUI
        //

        List< String > choices = RegistrationAxisType.asStringList();

        /*
        final MutableModuleItem<String> axisItem =
                addInput("message", String.class);
        axisItem.setPersisted( false );
        axisItem.setVisibility( ItemVisibility.MESSAGE );
        axisItem.setValue(this,
                "Please select the sequence dimension");
                */

        final MutableModuleItem<String> axisItem =
                addInput("axis" + 0, String.class);
        axisItem.setPersisted(false);
        axisItem.setVisibility( ItemVisibility.MESSAGE );
        axisItem.setValue(this, "aaaa");

        final MutableModuleItem<String> typeItem =
                addInput("bbb", String.class);
        typeItem.setPersisted(false);
        typeItem.setLabel("Sequence dimension");
        typeItem.setChoices( axisTypes );
        typeItem.setValue(this, ""+RegistrationAxisType.SEQUENCE_DIMENSION );

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
