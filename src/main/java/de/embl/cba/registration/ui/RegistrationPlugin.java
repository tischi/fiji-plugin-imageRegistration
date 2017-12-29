package de.embl.cba.registration.ui;

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.cba.registration.*;
import de.embl.cba.registration.Axes;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.filter.ImageFilterParameters;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import net.imagej.*;
import net.imagej.axis.*;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
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

import static bdv.viewer.DisplayMode.GROUP;

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
            "For finding the transformations it can help to preprocess the images.<br>" +
            "For example, phase- and cross-correlation are very noise sensitive.<br>" +
            "Typically it helps to threshold above the noise level.<br>";

    @Parameter( label = "Image pre-processing",
            choices = {"None", "Threshold", "DifferenceOfGaussianAndThreshold"},
            persist = false )
    protected String imageFilterType = "None";

    @Parameter(label = "Threshold values [min,max]"
            , persist = false  )
    protected String imageFilterThreshold = "0,255";

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
            "Min and max values determine a reference region that should be stabilized." +
            "</li>" +
            "<li>" +
            "Fixed: <b>Multiple</b> axes, for example your channel axis.<br>" +
            "Currently only the min value is used to choose the, e.g., reference channel.<br>" +
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
                typeItem.setValue( this, "" + RegistrationAxisType.Fixed );

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
            // - Sequence axis: reference point
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

    public void run() { }

    // Callbacks

    private void intervalChanged()
    {
        Settings settings = new Settings( this  );
        updateImagePlusOverlay( settings );
    }

    private void computeRegistration()
    {
        Settings settings = new Settings( this  );

        Thread thread = new Thread(new Runnable() {
            public void run()
            {

                Registration registration = new Registration( dataset, settings );

                registration.run();

                output = registration.output();

                showOutputWithBdv();


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
            if ( settings.registrationAxisTypes[ d ] == RegistrationAxisType.Transformable )
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
                uiService.show( output.imgPlus );
                Logger.doneIn( startTime );
            }
        } );
        thread.start();

    }

    private void showOutputWithBdv()
    {
        Bdv bdv = null;

        if ( output.numSpatialDimensions == 2 )
        {
            bdv = BdvFunctions.show(
                output.imgPlus,
                output.imgPlus.getName(),
                Bdv.options().is2D().axisOrder( output.axisOrder) );

        }
        else if ( output.numSpatialDimensions == 3 )
        {
            bdv = BdvFunctions.show(
                    output.imgPlus,
                    output.imgPlus.getName(),
                    Bdv.options().axisOrder( output.axisOrder) );
        }

        bdv.getBdvHandle().getViewerPanel().setDisplayMode( GROUP );

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

        boolean GUI = true;
        boolean TEST = false;
        boolean LOAD_IJ1_VS = false;
        boolean LOAD_IJ2_DATASET = true;

        Dataset dataset = null;
        int n = 0;

        // Load data
        if ( LOAD_IJ1_VS )
        {
            //IJ.run("Image Sequence...", "open=/Users/tischer/Documents/fiji-plugin-imageRegistration--data/mri-stack sort use");
            ImagePlus imp = IJ.openImage("/Users/tischer/Documents/paolo-ronchi--em-registration/chemfix_O6_crop.tif");
            imp.show();
        }
        else if ( LOAD_IJ2_DATASET ) {
            final File file = new File(
                    "/Users/tischer/Documents/fiji-plugin-imageRegistration/test-data/2d_t_2ch_drift_synthetic_blur.tif");

            if (file != null) {
                dataset = ij.scifio().datasetIO().open(file.getPath());
                n = dataset.numDimensions();
                ij.ui().show(dataset);
            }
        }


//            IJ.run("Image Sequence...",
//                    "open=/Users/tischi/Documents/fiji-plugin-imageRegistration--data/mri-stack-16bit sort use");
//            ImagePlus imp = IJ.getImage(); n = 3;

            // convert of cellImg that is lazily loaded
            //
//            Img< UnsignedShortType > img = ConvertVirtualStackToCellImg.getCellImgUnsignedShort( imp );
//            ImgPlus< UnsignedShortType > imgp = new ImgPlus<>( img, "title", new AxisType[]{ Axes.X, Axes.Y, Axes.Z } );
////            ij.get(LegacyService.class).getImageMap().addMapping(  ); // but it's private...
//            //imp.hide(); ImageJFunctions.show( img );
           // ij.convert().convert( RAI, Img.class )

        if ( GUI )
        {
            // invoke the plugin
            ij.command().run( RegistrationPlugin.class, true );
        }
        else if ( TEST )
        {

            int i;

            RegistrationAxisType[] registrationAxisTypes = new RegistrationAxisType[ n ];
            i = 0;
            registrationAxisTypes[ i++ ] = RegistrationAxisType.Transformable;
            registrationAxisTypes[ i++ ] = RegistrationAxisType.Transformable;
            registrationAxisTypes[ i++ ] = RegistrationAxisType.Fixed;
            registrationAxisTypes[ i++ ] = RegistrationAxisType.Sequence;

            long[] min = Intervals.minAsLongArray( dataset );
            long[] max = Intervals.maxAsLongArray( dataset );
            i = 0;
            min[ i ] = 50; max[ i++ ] = 220; // transformable dimension: reference range
            min[ i ] = 50; max[ i++ ] = 220; // transformable dimension: reference range
            min[ i ] = -1; max[ i++ ] = -1; // fixed dimension: not used
            min[ i ] = 0; max[ i++ ] = 3; // sequence dimension: transformfinder range

            FinalInterval interval = new FinalInterval( min, max );

            long[] other = Intervals.minAsLongArray( dataset );
            i = 0;
            other[ i++ ] = 30; // transformable dimension: maximal displacement
            other[ i++ ] = 30; // transformable dimension: maximal displacement
            other[ i++ ] = 0; // fixed dimension, chosen reference
            other[ i++ ] = 0; // sequence dimension: reference


            // Configure image filtering
            //
            Map< String, Object > imageFilterParameters = new HashMap<>();

            FilterType filterType = FilterType.DifferenceOfGaussianAndThreshold;

            imageFilterParameters.put(
                    ImageFilterParameters.GAUSS_SIGMA, new double[]{ 10.0D, 1.0D} );
            imageFilterParameters.put(
                    ImageFilterParameters.THRESHOLD_MIN_VALUE, 20.0D );
            imageFilterParameters.put(
                    ImageFilterParameters.DOG_SIGMA_SMALLER, new double[]{ 2.0D, 2.0D} );
            imageFilterParameters.put(
                    ImageFilterParameters.DOG_SIGMA_LARGER, new double[]{ 5.0D, 5.0D} );

            /*
            Registration imageRegistration =
                    new Registration(
                            dataset,
                            registrationAxisTypes,
                            interval,
                            other,
                            3,
                            filterType,
                            filterParameters,
                            OutputIntervalType.ReferenceRegionSize,
                            true );

            imageRegistration.run();

            */

            /*
            ArrayList< AxisType > axisTypes = new ArrayList<>(  );
            for (int d = 0; d < dataset.numDimensions(); d++)
            {
                axisTypes.add( dataset.axis( d ).type() );
            }

            ArrayList< Integer > axesIdsFixedSequenceOutput = new ArrayList<>();
            RandomAccessibleInterval raiFSO = imageRegistration.getFixedSequenceOutput( axesIdsFixedSequenceOutput );
            showRAI(ij.ui(),
                    ij.dataset(),
                    raiFSO,
                    "transformed reference sequence",
                    axesIdsFixedSequenceOutput,
                    axisTypes);

            ArrayList< Integer > axesIdsTransformedOutput = new ArrayList<>();
            RandomAccessibleInterval raiTO = imageRegistration.transformedImgPlus( axesIdsTransformedOutput );
            showRAI(ij.ui(),
                    ij.dataset(),
                    raiTO,
                    "transformed input",
                    axesIdsTransformedOutput,
                    axisTypes); */


        }

    }



}