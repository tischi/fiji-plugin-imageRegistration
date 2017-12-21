package de.embl.cba.registration.gui;

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
import de.embl.cba.registration.filter.ImageFilterParameters;
import de.embl.cba.registration.filter.ImageFilterType;
import de.embl.cba.registration.transformationfinders.TransformationFinderParameters;
import de.embl.cba.registration.transformationfinders.TransformationFinderType;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import net.imagej.*;
import net.imagej.axis.*;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
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

public class ImageRegistrationPlugin<T extends RealType<T>>
        extends DynamicCommand
        implements Interactive
{

    @Parameter
    private Dataset dataset;

    @Parameter
    private ImagePlus imagePlus;

    @Parameter
    private UIService uiService;

    @Parameter
    private DatasetService datasetService;

    @Parameter
    private LogService logService;

    @Parameter
    private OpService opService;

    @Parameter
    private StatusService statusService;

    @Parameter(label = "Transformation type and finder method",
            choices = {"Translation__PhaseCorrelation", "Rotation_Translation__PhaseCorrelation"},
            persist = false )
    private String transformationTypeInput = "Translation__PhaseCorrelation";

    @Parameter(label = "Output size",
            choices = {"ReferenceRegionSize", "InputDataSize"},
            persist = false )
    private String outputViewIntervalSizeTypeInput = "InputDataSize";

    @Parameter( visibility = ItemVisibility.MESSAGE )
    private String message01
            = "<html> "  +
            "<br>MAXIMAL TRANSFORMATION RANGES<br>" +
            "Please enter values as comma-separated list with one number per transformable dimension. <br>" +
            "The order must be the same as your axes appear down below.<br>" +
            "Maximal transformation values are between subsequent sequence coordinates.<br>";

    @Parameter(label = "Maximal translations [pixels]",
            persist = false)
    private String transformationParametersMaximalTranslationsInput = "30,30";

    @Parameter(label = "Maximal rotations [degrees]",
            persist = false)
    private String transformationParameterMaximalRotationsInput = "2";

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
    private String imageFilterTypeInput = "None";

    @Parameter(label = "Threshold values [min,max]"
            , persist = false  )
    private String imageFilterParameterThresholdInput = "0,255";

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

    Img img;

    Img transformedImg;

    public static ArrayList< AxisType > getAxisTypeList( Dataset dataset )
    {
        ArrayList< AxisType > axisTypes = new ArrayList<>(  );
        for (int d = 0; d < dataset.numDimensions(); d++)
        {
            axisTypes.add( dataset.axis( d ).type() );
        }

        return axisTypes;

    }

    @SuppressWarnings("unchecked")
    private MutableModuleItem<Long> varInput(final int d, final String var) {
        return (MutableModuleItem<Long>) getInfo().getInput( varName(d, var) );
    }

    @SuppressWarnings("unchecked")
    private MutableModuleItem<String> typeInput( final int d ) {
        return (MutableModuleItem<String>) getInfo().getInput( typeName( d ) );
    }

    @Parameter(label = "Compute registration",
            callback = "computeRegistration" )
    private Button computeRegistrationButton;

    @Parameter(label = "Get result",
            callback = "showTransformedImageWithLegacyUI" )
    private Button showWithLegacyUIButton;

    private void minMaxChanged()
    {
        UIInput uiInput = getUIValues();
        updateImagePlusReferenceRegionOverlay( uiInput );
    }

    private void updateImagePlusReferenceRegionOverlay( UIInput uiInput )
    {
        long xMin = 0, xMax = 0, yMin = 0, yMax = 0;

        for (int d = 0; d < dataset.numDimensions(); ++d )
        {
            if ( uiInput.registrationAxisTypes[ d ] == RegistrationAxisTypes.Transformable )
            {
                if ( dataset.axis( d ).type() == Axes.X )
                {
                    xMin = varInput( d, "min" ).getValue( this );
                    xMax = varInput( d, "max" ).getValue( this );
                }

                if ( dataset.axis( d ).type() == Axes.Y )
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

    private class UIInput
    {
        RegistrationAxisTypes[] registrationAxisTypes;
        FinalInterval interval;
    }


    private UIInput getUIValues()
    {
        UIInput uiInput = new UIInput();

        long[] min = Intervals.minAsLongArray( dataset );
        long[] max = Intervals.maxAsLongArray( dataset );
        // long[] other = new long[ numDimensions ];
        uiInput.registrationAxisTypes = new RegistrationAxisTypes[ dataset.numDimensions() ];

        for ( int d = 0; d < dataset.numDimensions(); ++d )
        {
            uiInput.registrationAxisTypes[ d ] = RegistrationAxisTypes.valueOf( typeInput( d ).getValue( this ) );
            min[ d ] = varInput( d, "min" ).getValue( this );
            max[ d ] = varInput( d, "max" ).getValue( this );
            // other[ d ] = varInput( d, "other" ).getValue( this );
        }

        uiInput.interval = new FinalInterval( min, max );

        return uiInput;
    }

    private void registrationThread()
    {

        int numDimensions = dataset.numDimensions();

        // Output view size type
        //
        OutputViewIntervalSizeTypes outputViewIntervalSizeType
                = OutputViewIntervalSizeTypes.valueOf( outputViewIntervalSizeTypeInput );

        // Get axis types, intervals and other coordinates from GUI
        //
        UIInput uiInput = getUIValues();

        // Configure image filtering
        //
        Map< String, Object > imageFilterParameters = new HashMap<>();

        ImageFilterType imageFilterType = ImageFilterType.valueOf( imageFilterTypeInput );

        imageFilterParameters.put(
                ImageFilterParameters.FILTER_TYPE,
                imageFilterType );

        String[] thresholdMinMax = imageFilterParameterThresholdInput.split( "," );
        imageFilterParameters.put(
                ImageFilterParameters.THRESHOLD_MIN_VALUE,
                Double.parseDouble( thresholdMinMax[0].trim() ) );
        imageFilterParameters.put(
                ImageFilterParameters.THRESHOLD_MAX_VALUE,
                Double.parseDouble( thresholdMinMax[1].trim() ) );


        // below are currently hard coded and cannot be changed from the GUI
        imageFilterParameters.put(
                ImageFilterParameters.DOG_SIGMA_SMALLER, new double[]{ 2.0D, 2.0D} );
        imageFilterParameters.put(
                ImageFilterParameters.DOG_SIGMA_LARGER, new double[]{ 5.0D, 5.0D} );
        imageFilterParameters.put(
                ImageFilterParameters.GAUSS_SIGMA, new double[]{ 10.0D, 1.0D} );


        // Configure transformation
        //
        String[] tmp;
        Map< String, Object > transformationParameters = new HashMap<>();
        boolean showFixedImageSequence = true;

        transformationParameters.put(
                TransformationFinderParameters.TRANSFORMATION_FINDER_TYPE,
                TransformationFinderType.valueOf( transformationTypeInput ) );

        tmp = transformationParametersMaximalTranslationsInput.split( "," );
        double[] transformationParametersMaximalTranslations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalTranslations[ i ] = Double.parseDouble( tmp[i].trim() );
        }
        transformationParameters.put(
                TransformationFinderParameters.MAXIMAL_TRANSLATIONS,
                transformationParametersMaximalTranslations );


        tmp = transformationParameterMaximalRotationsInput.split( "," );
        double[] transformationParametersMaximalRotations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalRotations[ i ] = Double.parseDouble( tmp[i] );
        }
        transformationParameters.put(
                TransformationFinderParameters.MAXIMAL_ROTATIONS,
                transformationParametersMaximalRotations );


        ImageRegistration imageRegistration =
                new ImageRegistration(
                        dataset,
                        datasetService,
                        uiInput.registrationAxisTypes,
                        uiInput.interval,
                        imageFilterParameters,
                        transformationParameters,
                        3,
                        outputViewIntervalSizeType,
                        showFixedImageSequence,
                        logService );



        imageRegistration.run();


        transformedImg = imageRegistration.getTransformedImg();
        BdvFunctions.show( transformedImg, "Transformed Image", Bdv.options().is2D() );


    }

    protected void computeRegistration() {

        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                registrationThread();
            }
        } );
        thread.start();

    }

    protected void showTransformedImageWithLegacyUI() {

        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                long startTime = LogServiceImageRegistration.start("# Preparing result for export...");
                uiService.show(  transformedImg );
                LogServiceImageRegistration.doneInDuration( startTime );
            }
        } );
        thread.start();

    }

    public void run() {


    }

    protected void init()
    {

        LogServiceImageRegistration.statusService = statusService;
        LogServiceImageRegistration.logService = logService;
        LogServiceImageRegistration.statusService = statusService;
        LogServiceImageRegistration.debug( "# Initializing UI...");

        String a = imagePlus.getTitle();

        if ( imagePlus.getStack() instanceof VirtualStack )
        {
            img = ImageJFunctions.wrap( imagePlus );
        }

        boolean persist = false;

        int n = dataset.numDimensions();

        List< String > registrationAxisTypes =
                Stream.of( RegistrationAxisTypes.values() )
                    .map( RegistrationAxisTypes::name )
                    .collect( Collectors.toList() );

        ArrayList< AxisType > axisTypes = getAxisTypeList( dataset );

        // Guess default axisType choices
        //
        AxisType sequenceDefault = axisTypes.get( 0 );
        if ( axisTypes.contains( Axes.TIME ) )
        {
            sequenceDefault = Axes.TIME;
        }
        else if ( axisTypes.contains( Axes.Z ) )
        {
            sequenceDefault = Axes.Z;
        }
        else if ( axisTypes.contains( Axes.CHANNEL ) )
        {
            sequenceDefault = Axes.CHANNEL;
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
                typeItem.setValue( this, "" + RegistrationAxisTypes.Sequence );
            else if ( axisTypes.get( d ).equals( Axes.X ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Transformable );
            else if ( axisTypes.get( d ).equals( Axes.Y ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Transformable );
            else if ( axisTypes.get( d ).equals( Axes.Z ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Transformable );
            else if ( axisTypes.get( d ).equals( Axes.CHANNEL ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Fixed );

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
            minItem.setCallback( "minMaxChanged" );

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
            maxItem.setCallback( "minMaxChanged" );


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

            LogServiceImageRegistration.debug( "...done.");


        }

    }

    // -- Helper methods --
    private String typeName( final int d ) {
        return "type" + d;
    }

    private String varName( final int d, final String var ) {
        return "var" + d + ":" + var;
    }

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        boolean GUI = true;
        boolean TEST = false;
        boolean LOAD_IJ1_VS = true;
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
            ij.command().run( ImageRegistrationPlugin.class, true );
        }
        else if ( TEST )
        {

            int i;

            RegistrationAxisTypes[] registrationAxisTypes = new RegistrationAxisTypes[ n ];
            i = 0;
            registrationAxisTypes[ i++ ] = RegistrationAxisTypes.Transformable;
            registrationAxisTypes[ i++ ] = RegistrationAxisTypes.Transformable;
            registrationAxisTypes[ i++ ] = RegistrationAxisTypes.Fixed;
            registrationAxisTypes[ i++ ] = RegistrationAxisTypes.Sequence;

            long[] min = Intervals.minAsLongArray( dataset );
            long[] max = Intervals.maxAsLongArray( dataset );
            i = 0;
            min[ i ] = 50; max[ i++ ] = 220; // transformable dimension: reference range
            min[ i ] = 50; max[ i++ ] = 220; // transformable dimension: reference range
            min[ i ] = -1; max[ i++ ] = -1; // fixed dimension: not used
            min[ i ] = 0; max[ i++ ] = 3; // sequence dimension: transformationfinders range

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

            ImageFilterType imageFilterType = ImageFilterType.DifferenceOfGaussianAndThreshold;

            imageFilterParameters.put(
                    ImageFilterParameters.GAUSS_SIGMA, new double[]{ 10.0D, 1.0D} );
            imageFilterParameters.put(
                    ImageFilterParameters.THRESHOLD_MIN_VALUE, 20.0D );
            imageFilterParameters.put(
                    ImageFilterParameters.DOG_SIGMA_SMALLER, new double[]{ 2.0D, 2.0D} );
            imageFilterParameters.put(
                    ImageFilterParameters.DOG_SIGMA_LARGER, new double[]{ 5.0D, 5.0D} );

            /*
            ImageRegistration imageRegistration =
                    new ImageRegistration(
                            dataset,
                            registrationAxisTypes,
                            interval,
                            other,
                            3,
                            imageFilterType,
                            imageFilterParameters,
                            OutputViewIntervalSizeTypes.ReferenceRegionSize,
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
            RandomAccessibleInterval raiTO = imageRegistration.getTransformedImg( axesIdsTransformedOutput );
            showRAI(ij.ui(),
                    ij.dataset(),
                    raiTO,
                    "transformed input",
                    axesIdsTransformedOutput,
                    axisTypes); */


        }

    }


}