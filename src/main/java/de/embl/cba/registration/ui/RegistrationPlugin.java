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
import de.embl.cba.registration.projection.ProjectionType;
import de.embl.cba.registration.util.Enums;
import de.embl.cba.registration.util.MetaImage;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import net.imagej.*;
import net.imagej.axis.*;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
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
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import java.awt.*;
import java.util.*;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>N-D Sequence Registration", initializer = "init")
public class RegistrationPlugin<T extends RealType<T>> extends DynamicCommand implements Interactive
{

    //@Parameter
    //public Dataset dataset;

    @Parameter ( required = true )
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

    /*
    @Parameter( visibility = ItemVisibility.MESSAGE, persist = false )
    private String message02
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

    @Parameter( visibility = ItemVisibility.MESSAGE )
    private String message03
            = "<html>"  +
            "<br>IMAGE PRE-PROCESSING<br>" +
            "For finding the transformations it can help to pre-process the images.<br>" +
            "For example, the phase-correlation algorithm is very noise sensitive.<br>" +
            "Typically, it for instance helps to threshold above the noise level.<br>";
            */

    @Parameter(label = "Transformation type and finder method",
            choices = { "Translation__PhaseCorrelation", "Translation__Maximum" } )
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
    public String space = " ";

    @Parameter( label = "Pre-processing pipeline", choices = {
            "None",
            "Threshold",
            "DifferenceOfGaussian",
            "ThresholdAndDifferenceOfGaussian",
            "ThresholdAndGradient"
    } )
    protected String imageFilterType = "None";

    @Parameter(label = "Threshold values [min,max]"  )
    protected String imageFilterThreshold = "10,230";

    //@Parameter(label = "Sub-sampling [pixels]", persist = false)
    //protected String imageFilterSubSampling = "1,1";

    @Parameter(label = "Gaussian filter size [pixels]")
    protected double gaussianFilterSize = 3.0D;


    @SuppressWarnings("unchecked")
    protected MutableModuleItem<Long> varInput(final int d, final String var) {
        return (MutableModuleItem<Long>) getInfo().getInput( varName(d, var) );
    }

    @SuppressWarnings("unchecked")
    protected MutableModuleItem<String> typeInput( final int d ) {
        return (MutableModuleItem<String>) getInfo().getInput( typeName( d ) );
    }

    // input
    public RandomAccessibleInterval rai;
    public ArrayList< AxisType > axisTypes;
    public ArrayList< Long > offsetForZeroOrOneBasedAxes;

    // output
    public MetaImage transformed;
    public MetaImage processedAndTransformedReference;

    private Registration registration;

    private void init()
    {
        setAxisTypes( imagePlus );
        setRAI( imagePlus );
        setZeroOrOneBasedAxisOffsets( rai );
        Services.setServices( this );
        Logger.setLogger( this );

        addPreProcessingUI();
        addOtherAxesProjectionTypeUI();
        addAxesSelectionUI();
        addActionButtonsUI();
    }

    private void setAxisTypes( ImagePlus imagePlus )
    {
        axisTypes = Axes.getAxisTypes( imagePlus );
    }

    private void setRAI( ImagePlus imagePlus )
    {
        if ( imagePlus.getStack() instanceof VirtualStack )
        {
            rai = VirtualStackAdapter.wrap( imagePlus );
        }
        else
        {
            rai = ImageJFunctions.wrap( imagePlus );
        }
    }

    private void setZeroOrOneBasedAxisOffsets( RandomAccessibleInterval rai )
    {
        offsetForZeroOrOneBasedAxes = new ArrayList<>( );

        for (int d = 0; d < rai.numDimensions(); d++)
        {
            if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.Z )
                    || axisTypes.get( d ).equals( net.imagej.axis.Axes.TIME )
                    || axisTypes.get( d ).equals( net.imagej.axis.Axes.CHANNEL )
                    )
            {
                offsetForZeroOrOneBasedAxes.add( 1L );
            }
            else
            {
                offsetForZeroOrOneBasedAxes.add( 0L );
            }
        }
    }

    private String getHeader( String title )
    {
        String separation = "-------------------------";
        String header = separation + " " + title + " " + separation;
        return header;
    }

    private void addAxesSelectionUI()
    {

        addSpace();

        AxisType sequenceDefault = guessSequenceAxisType( axisTypes );

        boolean persist = true;

        for (int d = 0; d < rai.numDimensions(); d++)
        {
            String var;

            // Axis type selection
            //
            final MutableModuleItem< String > typeItem = addInput( typeName( d ), String.class );
            typeItem.setPersisted( persist );
            typeItem.setLabel( axisTypes.get( d ).toString() + " axis type" );
            typeItem.setChoices( RegistrationAxisType.asStringList() );

            if ( axisTypes.get( d ).equals( sequenceDefault ) )
            {
                typeItem.setValue( this, "" + RegistrationAxisType.Sequence );
            }
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.X ) )
            {
                typeItem.setValue( this, "" + RegistrationAxisType.Registration );
            }
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.Y ) )
            {
                typeItem.setValue( this, "" + RegistrationAxisType.Registration );
            }
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.Z ) )
            {
                typeItem.setValue( this, "" + RegistrationAxisType.Registration );
            }
            else if ( axisTypes.get( d ).equals( net.imagej.axis.Axes.CHANNEL ))
            {
                typeItem.setValue( this, "" + RegistrationAxisType.Other );
            }

            // Interval minimum
            //
            var = "min";
            final MutableModuleItem<Long> minItem = addInput( varName(d, var), Long.class);
            minItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            minItem.setPersisted( persist );
            minItem.setLabel( var );
            minItem.setMinimumValue( rai.min( d ) + offsetForZeroOrOneBasedAxes.get( d ) );
            minItem.setMaximumValue( rai.max( d ) + offsetForZeroOrOneBasedAxes.get( d ) );
            minItem.setDefaultValue( rai.min( d ) + offsetForZeroOrOneBasedAxes.get( d ) );
            minItem.setCallback( "intervalChanged" );

            // Interval maximum
            //
            var = "max";
            final MutableModuleItem<Long> maxItem = addInput( varName(d, var), Long.class);
            maxItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            maxItem.setPersisted( persist );
            maxItem.setLabel( var );
            maxItem.setMinimumValue( rai.min( d ) + offsetForZeroOrOneBasedAxes.get( d ) );
            maxItem.setMaximumValue( rai.max( d ) + offsetForZeroOrOneBasedAxes.get( d ) );
            maxItem.setDefaultValue( rai.max( d ) + offsetForZeroOrOneBasedAxes.get( d ) );
            maxItem.setCallback( "intervalChanged" );

        }
    }

    private void addHeaderUI( String header )
    {
        final MutableModuleItem< String > axisSelection = addInput( "Axis selection message", String.class );
        axisSelection.setVisibility( ItemVisibility.MESSAGE );
        axisSelection.setPersisted( false );
        axisSelection.setValue(this, getHeader( header ) );
    }

    private void addSpace()
    {
        final MutableModuleItem< String > axisSelection = addInput( " ", String.class );
        axisSelection.setVisibility( ItemVisibility.MESSAGE );
        axisSelection.setLabel( " " );
        axisSelection.setPersisted( false );
        axisSelection.setValue(this, " ");
    }

    private void addActionButtonsUI()
    {
        addSpace();

        final MutableModuleItem< Button > button01 = addInput( "Compute transformations", Button.class );
        button01.setCallback( "computeTransformations" );
        button01.setRequired( false );

        final MutableModuleItem< Button > button02 = addInput( "Show transformed input with BigDataViewer", Button.class );
        button02.setCallback( "showTransformedOutputWithBigDataViewer" );
        button02.setRequired( false );

        //final MutableModuleItem< Button > button = addInput( "Save result with SCIFIO", Button.class );
        //button.setCallback( "saveResultAsICSusingSCIFIO" );
        //button.setRequired( false );

        final MutableModuleItem< Button > button03 = addInput( "Show reference region", Button.class );
        button03.setCallback( "showProcessedAndTransformedReferenceWithImageJFunctions" );
        button03.setRequired( false );
    }

    private static final String GRADIENT_AXIS_INPUT = "Gradient axis";

    private void addPreProcessingUI()
    {
        final MutableModuleItem< String > axisItem = addInput( GRADIENT_AXIS_INPUT, String.class );
        axisItem.setChoices( Axes.asStringList( axisTypes ) );
        axisItem.setValue(this, axisTypes.get( 0 ).toString() );
    }

    private static final String OTHER_AXES_PROJECTION_TYPE = "Other axes projection type";

    private void addOtherAxesProjectionTypeUI()
    {
        addSpace();

        final MutableModuleItem< String > axisItem = addInput( OTHER_AXES_PROJECTION_TYPE, String.class );
        axisItem.setChoices( ProjectionType.asStringList() );
        axisItem.setValue(this, ProjectionType.asStringList().get( 0 ) );
        axisItem.setPersisted( true );
    }

    public ProjectionType getOtherAxesProjectionType()
    {
        String name = ( String ) this.getInput( OTHER_AXES_PROJECTION_TYPE );
        ProjectionType type = ProjectionType.valueOf( name );
        return type;
    }

    private AxisType guessSequenceAxisType( ArrayList< AxisType > axisTypes )
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

    private void intervalChanged()
    {
        Settings settings = new Settings( this  );
        updateImagePlusOverlay( settings );
    }

    private void computeTransformations()
    {
        Settings settings = new Settings( this  );

        if ( ! settings.check() ) return;

        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                registration = new Registration( settings );
                registration.run();
                registration.logTransformations();
                transformed = registration.getTransformedImage( OutputIntervalSizeType.TransformationsEncompassing );
                showTransformedOutputWithImageJFunctions();
            }
        } );
        thread.start();

    }

    private void showTransformedOutputWithBigDataViewer()
    {
        if ( transformed != null )
        {
            Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions, transformed.axisOrder );
        }
    }

    private void showProcessedAndTransformedReferenceWithImageJFunctions()
    {
         MetaImage referenceRegion = registration.getProcessedAndTransformedReferenceImage();
         Viewers.showRAIUsingImageJFunctions( referenceRegion.rai, referenceRegion.axisTypes, "reference region" );
    }

    private void saveResultAsICSusingSCIFIO()
    {
        if ( transformed != null )
        {
            Writers.saveMetaImageUsingScifio( transformed, uiService.chooseFile(null, FileWidget.SAVE_STYLE  ) );
        }
    }

    private void showTransformedOutputWithImageJFunctions() {

        if ( transformed == null )
        {
            Logger.statusService.warn( "No transformed output image available yet." );
            return;
        }

        Thread thread = new Thread(new Runnable() {
            public void run()
            {
                //Viewers.showRAIAsImgPlusWithUIService( transformed.rai, datasetService, transformed.axisTypes, transformed.title, uiService );
                Viewers.showRAIUsingImageJFunctions( transformed.rai, transformed.axisTypes, transformed.title );
            }
        } );
        thread.start();

    }

    private void createAndShowProcessedAndTransformedReferenceWithImageJFunctions() {

        processedAndTransformedReference = registration.getProcessedAndTransformedReferenceImage();

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

    private void updateImagePlusOverlay( Settings settings )
    {
        long xMin = 0, xMax = 0, yMin = 0, yMax = 0;

        for (int d = 0; d < rai.numDimensions(); ++d )
        {
            RegistrationAxisType type = settings.registrationAxisTypes.get( d );

            if ( type == RegistrationAxisType.Registration || type == RegistrationAxisType.Other )
            {
                if ( settings.axisTypes.get( d ) == net.imagej.axis.Axes.X )
                {
                    xMin = varInput( d, "min" ).getValue( this );
                    xMax = varInput( d, "max" ).getValue( this );
                }
                else if ( settings.axisTypes.get( d ) == net.imagej.axis.Axes.Y )
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

    protected String typeName( final int d )
    {
        return "type" + d;
    }

    protected String varName( final int d, final String var ) {
        return "var" + d + ":" + var;
    }

    public String getGradientAxisName()
    {
        String gradientAxisName = ( String ) this.getInput( GRADIENT_AXIS_INPUT );

        return gradientAxisName;
    }


}