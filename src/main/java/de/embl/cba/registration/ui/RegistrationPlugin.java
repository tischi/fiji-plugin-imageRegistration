package de.embl.cba.registration.ui;

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import de.embl.cba.registration.*;
import de.embl.cba.registration.Axes;
import de.embl.cba.registration.views.BDV;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import net.imagej.*;
import net.imagej.axis.*;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin(type = Command.class,
        menuPath = "Plugins>Registration>N-D Sequence Registration",
        initializer = "init")
public class RegistrationPlugin<T extends RealType<T>>
        extends DynamicCommand
        implements Interactive
{

    @Parameter
    public Dataset dataset;

    @Parameter (required = false)
    public ImagePlus imagePlus;

    @Parameter
    public UIService uiService;

    @Parameter
    public DatasetService datasetService;

    @Parameter
    public LogService logService;

    @Parameter
    public ThreadService threadService;

    @Parameter
    public OpService opService;

    @Parameter
    public StatusService statusService;

    @Parameter(label = "Transformation type and finder method",
            choices = {"Translation__PhaseCorrelation", "Rotation_Translation__PhaseCorrelation"},
            persist = false )
    public String transformationTypeInput = "Translation__PhaseCorrelation";

    @Parameter(label = "Output size",
            choices = {"ReferenceRegionSize", "InputDataSize"},
            persist = false )
    protected String outputViewIntervalSizeTypeInput = "InputDataSize";

    @Parameter( visibility = ItemVisibility.MESSAGE )
    private String message01
            = "<html> "  +
            "<br>MAXIMAL TRANSFORMATION RANGES<br>" +
            "Please enter values as comma-separated list with one number per transformable dimension. <br>" +
            "The order must be the same as your axes appear down below.<br>" +
            "Maximal transformation values are between subsequent sequence coordinates.<br>";

    @Parameter(label = "Maximal translations [pixels]",
            persist = false)
    protected String transformationParametersMaximalTranslationsInput = "30,30";

    @Parameter(label = "Maximal rotations [degrees]",
            persist = false)
    protected String transformationParameterMaximalRotationsInput = "2";

    @Parameter( visibility = ItemVisibility.MESSAGE )
    private String message02
            = "<html> "  +
            "<br>IMAGE PRE-PROCESSING<br>" +
            "For finding the transformations it can help to pre-process the images.<br>" +
            "For example, phase- and cross-correlation are very noise sensitive.<br>" +
            "Typically it helps to threshold above the noise level.<br>";

    @Parameter( label = "Image pre-processing",
            choices = {"None", "Threshold", "ThresholdAndDifferenceOfGaussian"},
            persist = false )
    protected String imageFilterType = "None";

    @Parameter(label = "Threshold values [min,max]"
            , persist = false  )
    protected String imageFilterThreshold = "10,230";

    @Parameter(label = "Sub-sampling [pixels]", persist = false)
    protected String imageFilterSubSampling = "1,1";


    @Parameter( visibility = ItemVisibility.MESSAGE )
    private String message03
            = "<html> "  +
            "<br>AXES SET-UP<br>" +
            "<li>" +
            "Sequence: <b>One</b> axis along which you want to register, e.g. time or z.<br>" +
            "Min and max values chose a subset of the full dataset.<br>" +
            "</li>" +
            "<li>" +
            "Transformable: <b>Multiple</b> axes where the transformations occur, e.g. x and y.<br>" +
            "Min and max values determine a referenceImgPlus region that should be stabilized." +
            "</li>" +
            "<li>" +
            "Other: <b>Multiple</b> axes, for example your channel axis.<br>" +
            "Currently only the min value is used to choose the, e.g., referenceImgPlus channel.<br>" +
            "The transformation is applied to all coordinates for your fixed axes." +
            "</li>";

    protected Img img;

    @SuppressWarnings("unchecked")
    protected MutableModuleItem<Long> varInput(final int d, final String var) {
        return (MutableModuleItem<Long>) getInfo().getInput( varName(d, var) );
    }

    @SuppressWarnings("unchecked")
    protected MutableModuleItem<String> typeInput( final int d ) {
        return (MutableModuleItem<String>) getInfo().getInput( typeName( d ) );
    }

    @Parameter(label = "Compute registration",
            callback = "computeRegistration" )
    private Button computeRegistrationButton;

    @Parameter(label = "Get result as ImageJ stack (can take some time...)",
            callback = "showOutputWithIJHyperstack" )
    private Button showOutputWithIJHyperstackButton;

    Output output;

    // Initialization

    private void init()
    {
        Services.setServices( this );
        Logger.setLoggers( this );

        Logger.debug( "# Initializing UI...");

        if ( imagePlus != null )
        {
            if ( imagePlus.getStack() instanceof VirtualStack )
            {
                img = ImageJFunctions.wrap( imagePlus );
            }
        }

        boolean persist = false;

        List< String > registrationAxisTypes =
                Stream.of( RegistrationAxisType.values() )
                        .map( RegistrationAxisType::name )
                        .collect( Collectors.toList() );

        ArrayList< AxisType > axisTypes = Axes.axisTypesList( dataset );

        // Guess default axisType choices
        //
        AxisType sequenceDefault = axisTypes.get( 0 );
        if ( axisTypes.contains( net.imagej.axis.Axes.TIME ) )
        {
            sequenceDefault = net.imagej.axis.Axes.TIME;
        }
        else if ( axisTypes.contains( net.imagej.axis.Axes.Z ) )
        {
            sequenceDefault = net.imagej.axis.Axes.Z;
        }
        else if ( axisTypes.contains( net.imagej.axis.Axes.CHANNEL ) )
        {
            sequenceDefault = net.imagej.axis.Axes.CHANNEL;
        }

        for (int d = 0; d < dataset.numDimensions(); d++)
        {

            String var;

            // Message
            //
            final MutableModuleItem<String> axisItem =
                    addInput( axisTypes.get( d ).toString(), String.class);
            axisItem.setPersisted( persist );
            axisItem.setVisibility( ItemVisibility.MESSAGE );
            axisItem.setValue(this, axisTypes.get( d ).toString() );

            // Axis type selection
            //
            final MutableModuleItem<String> typeItem =
                    addInput( typeName( d ), String.class);
            typeItem.setPersisted( persist );
            typeItem.setLabel( "Type" );
            typeItem.setChoices( registrationAxisTypes );

            if ( axisTypes.get( d ).equals( sequenceDefault ) )
                typeItem.setValue( this, "" + RegistrationAxisType.Sequence );
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.X ))
                typeItem.setValue( this, "" + RegistrationAxisType.Transformable );
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.Y ))
                typeItem.setValue( this, "" + RegistrationAxisType.Transformable );
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.Z ))
                typeItem.setValue( this, "" + RegistrationAxisType.Transformable );
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.CHANNEL ))
                typeItem.setValue( this, "" + RegistrationAxisType.Other );

            // Interval minimum
            //
            var = "min";
            final MutableModuleItem<Long> minItem =
                    addInput( varName(d, var), Long.class);
            minItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            minItem.setPersisted( persist );
            minItem.setLabel( var );
            minItem.setMinimumValue( dataset.min( d ) );
            minItem.setMaximumValue( dataset.max( d ) );
            minItem.setDefaultValue( dataset.min( d ) );
            minItem.setCallback( "intervalChanged" );

            // Interval maximum
            //
            var = "max";
            final MutableModuleItem<Long> maxItem =
                    addInput( varName(d, var), Long.class);
            maxItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            maxItem.setPersisted( persist );
            maxItem.setLabel( var );
            maxItem.setMinimumValue( dataset.min( d ) );
            maxItem.setMaximumValue( dataset.max( d ) );
            maxItem.setDefaultValue( dataset.max( d ) );
            maxItem.setCallback( "intervalChanged" );


            // Other
            // - Sequence axis: referenceImgPlus point
            // - Transformation axis: maximal displacement
            //
//            var = "other";
//            final MutableModuleItem<Long> otherItem =
//                    addInput( varName(d, var), Long.class);
//            otherItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
//            otherItem.setPersisted( persist );
//            otherItem.setLabel( var );
//            otherItem.setValue(this, dataset.max( d ) );
//            otherItem.setMinimumValue( dataset.min( d ) );
//            otherItem.setMaximumValue( dataset.max( d ) );

            Logger.debug( "...done.");


        }

    }

    public void run()
    {
        // do nothing
        // the plugin is executed via a callback to computeRegistration()
    }

    // Callbacks

    private void intervalChanged()
    {
        Settings settings = new Settings( this  );
        updateImagePlusOverlay( settings );
    }

    private void computeRegistration()
    {
        Settings settings = new Settings( this  );

        if ( ! settings.check() ) return;

        Thread thread = new Thread(new Runnable() {
            public void run()
            {

                Registration registration = new Registration( dataset, settings );

                registration.run();

                output = registration.output();

                //show( output.referenceImgPlus, output.referenceNumSpatialDimensions, output.referenceAxisOrder);

                BDV.show( output.transformedImgPlus, output.transformedNumSpatialDimensions, output.transformedAxisOrder );

            }
        } );
        thread.start();

    }



    // Other

    private void updateImagePlusOverlay( Settings settings )
    {
        long xMin = 0, xMax = 0, yMin = 0, yMax = 0;

        for (int d = 0; d < dataset.numDimensions(); ++d )
        {
            RegistrationAxisType type = settings.registrationAxisTypes.get( d );

            if ( type == RegistrationAxisType.Transformable || type == RegistrationAxisType.Other )
            {
                if ( dataset.axis( d ).type() == net.imagej.axis.Axes.X )
                {
                    xMin = varInput( d, "min" ).getValue( this );
                    xMax = varInput( d, "max" ).getValue( this );
                }

                if ( dataset.axis( d ).type() == net.imagej.axis.Axes.Y )
                {
                    yMin = varInput( d, "min" ).getValue( this );
                    yMax = varInput( d, "max" ).getValue( this );
                }

            }
        }

        Roi roi = new Roi( (int) xMin, (int) yMin, (int) (xMax-xMin), (int) (yMax-yMin) );
        // TODO: implement Z
        //roi.setPosition();
        roi.setStrokeColor( Color.GREEN );
        //roi.setFillColor( new Color( 0x3300FF00 ) );

        Overlay overlay = new Overlay( roi );
        imagePlus.setOverlay(overlay);


    }

    protected void showOutputWithIJHyperstack() {

        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                long startTime = Logger.start("# Preparing result for ImageJ Hyperstack display...");
                uiService.show( output.transformedImgPlus );
                Logger.doneIn( startTime );
            }
        } );
        thread.start();

    }

    protected String typeName( final int d ) {
        return "type" + d;
    }

    protected String varName( final int d, final String var ) {
        return "var" + d + ":" + var;
    }


    // Main

    public static void main(final String... args) throws Exception {
        // toArrayImg the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        boolean LOAD_IJ1_VS = false;
        boolean LOAD_IJ2_DATASET = true;
        String PATH;

        PATH = "/Users/tischer/Documents/paolo-ronchi--em-registration/chemfix_O6_crop.tif";
        PATH = "/Users/tischer/Documents/paolo-ronchi--em-registration/chemfix_O6_crop--z1-5.tif";
        //PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration/test-data/2d_t_2ch_drift_synthetic_blur.tif";
        //PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration/test-data/2d_t_1ch_drift_synthetic_line.tif";
        PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration/test-data/2d_t_1ch_drift_synthetic_edge.tif";
        PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration/test-data/2d_t_1ch_drift_synthetic_edge_noise_small.tif";

        //PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration/test-data/2d_t_1ch_drift_synthetic_shorterline.tif";

        //PATH = "/Users/tischer/Documents/henning-falk--3d-embryo-registration--data/large-jump.tif";

        Dataset dataset = null;
        int n = 0;

        // Load data
        if ( LOAD_IJ1_VS )
        {
            ImagePlus imp = IJ.openImage( PATH );
            imp.show();
        }
        else if ( LOAD_IJ2_DATASET ) {
            final File file = new File( PATH );

            if (file != null) {
                dataset = ij.scifio().datasetIO().open(file.getPath());
                n = dataset.numDimensions();
                ij.ui().show(dataset);
            }
        }

        // invoke the plugin
        ij.command().run( RegistrationPlugin.class, true );

    }



}