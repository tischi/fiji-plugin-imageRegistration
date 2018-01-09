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
import de.embl.cba.registration.util.Enums;
import de.embl.cba.registration.util.MetaImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import net.imagej.*;
import net.imagej.axis.*;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
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
            choices = {"Translation__PhaseCorrelation"}, //, "Rotation_Translation__PhaseCorrelation"},
            persist = false )
    public String transformationTypeInput = "Translation__PhaseCorrelation";

    //@Parameter(label = "Output size",
    //        choices = {"ReferenceRegion", "InputImage"},
    //        persist = false )
    //protected String outputViewIntervalSizeTypeInput = "InputImage";

    //@Parameter( visibility = ItemVisibility.MESSAGE )
    //private String message01
    //        = "<html> "  +
    //       "<br>MAXIMAL TRANSFORMATION RANGES<br>" +
    //        "Please enter values as comma-separated list with one number per transformable dimension. <br>" +
    //        "The order must be the same as your axes appear down below.<br>" +
    //        "Maximal transformation values are between subsequent sequence coordinates.<br>";

    //@Parameter(label = "Maximal translations [pixels]", persist = false)
    //protected String transformationParametersMaximalTranslationsInput = "30,30";

    //@Parameter(label = "Maximal rotations [degrees]", persist = false)
    //protected String transformationParameterMaximalRotationsInput = "2";

    @Parameter( visibility = ItemVisibility.MESSAGE )
    private String message02
            = "<html>"  +
            "<br>IMAGE PRE-PROCESSING<br>" +
            "For finding the transformations it can help to pre-process the images.<br>" +
            "For example, the phase-correlation algorithm is very noise sensitive.<br>" +
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

    @Parameter( visibility = ItemVisibility.MESSAGE, persist = false )
    private String message03
            = "<html>"
            + "<br>AXES SET-UP<br>"
            + "<li>"
            + "Sequence axis: <b>One</b> axis along which you want to register your data, typically the time or z-axis.<br>"
            + "Min and max values chose a subset of the full dataset.<br>"
            + "</li>"
            + "<li>"
            + "Registration axes: <b>Multiple</b> axes to be transformed, e.g. the x- and y-axis.<br>"
            + "Min and max values determine the reference region that should be stabilized."
            + "</li>"
            + "<li>"
            + "Other axes: <b>Multiple</b> axes, for example the channel axis.<br>"
            + "Min and max values determine a reference range within which an average value will be computed.<br>"
            + "</li>";

    protected Img img;
    private MetaImage transformed;
    private MetaImage processedAndTransformedReference;

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
    private Button computeRegistration;

    @Parameter(label = "View registered image as ImageJ stack",
            callback = "showTransformedOutputAsIJHyperstack" )
    private Button showTransformedOutputAsIJHyperstack;

    @Parameter(label = "View processed and registered reference region as ImageJ stack",
            callback = "showProcessedAndTransformedReferenceAsIJHyperstack" )
    private Button showProcessedAndTransformedReferenceAsIJHyperstack;


    // Initialization

    private void init()
    {
        Services.setServices( this );
        Logger.setLogger( this );
        //dealWithVirtualStackInputs();
        configureAxesUI();
    }

    private void configureAxesUI()
    {
        List< String > registrationAxisTypes = Enums.asStringList( RegistrationAxisType.values() );
        ArrayList< AxisType > axisTypes = Axes.axisTypesList( dataset );
        AxisType sequenceDefault = guessSequenceAxis( axisTypes );

        boolean persist = false;

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
                typeItem.setValue( this, "" + RegistrationAxisType.Registration );
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.Y ))
                typeItem.setValue( this, "" + RegistrationAxisType.Registration );
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.Z ))
                typeItem.setValue( this, "" + RegistrationAxisType.Registration );
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

        }
    }

    private void dealWithVirtualStackInputs()
    {
        /*
        if ( imagePlus != null )
        {
            if ( imagePlus.getStack() instanceof VirtualStack )
            {
                img = ImageJFunctions.wrap( imagePlus );
            }
        }
        */
    }

    private AxisType guessSequenceAxis( ArrayList< AxisType > axisTypes )
    {
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
        return sequenceDefault;
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

                Registration registration = new Registration( settings );

                registration.run();
                registration.logTransformations();
                transformed = registration.transformedImage( OutputIntervalSizeType.InputImage );
                processedAndTransformedReference = registration.processedAndTransformedReferenceImage();

                Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions, transformed.axisOrder );

                //show( output.referenceImgPlus, output.referenceNumSpatialDimensions, output.referenceAxisOrder);

                //BDV.show( output.transformedImgPlus, output.transformedNumSpatialDimensions, output.transformedAxisOrder );

            }
        } );
        thread.start();

    }

    private void showTransformedOutputAsIJHyperstack() {

        if ( transformed == null )
        {
            Logger.statusService.warn( "No transformed output image available yet." );
            return;
        }

        Thread thread2 = new Thread(new Runnable() {
            public void run()
            {
                Viewers.showRAIAsImgPlusWithUIService( transformed.rai, datasetService, transformed.axisTypes, transformed.title, uiService );
            }
        } );
        thread2.start();

    }

    private void showProcessedAndTransformedReferenceAsIJHyperstack() {

        if ( processedAndTransformedReference == null )
        {
            Logger.statusService.warn( "No transformed reference image available yet." );
            return;
        }

        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                Viewers.showRAIWithUIService( processedAndTransformedReference.rai, uiService );
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

            if ( type == RegistrationAxisType.Registration || type == RegistrationAxisType.Other )
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

        String PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/x90-y94-c2-t3--square--translation.tif";
        //String PATH = "/Users/tischer/Documents/paolo-ronchi--em-registration/chemfix_O6_crop.tif";

        Dataset dataset = null;

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
                ij.ui().show(dataset);
            }
        }

        // invoke the plugin
        ij.command().run( RegistrationPlugin.class, true );

    }



}