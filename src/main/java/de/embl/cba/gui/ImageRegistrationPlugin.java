package de.embl.cba.gui;

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import de.embl.cba.*;
import de.embl.cba.filter.ImageFilterParameters;
import de.embl.cba.filter.ImageFilterType;
import ij.ImagePlus;
import loci.plugins.BF;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.*;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

import loci.plugins.in.ImporterOptions;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin(type = Command.class,
        menuPath = "Plugins>Image Registration",
        initializer = "init")
public class ImageRegistrationPlugin<T extends RealType<T>>
        extends DynamicCommand
        implements Interactive
{

    @Parameter
    private Dataset dataset;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter(label = "Transformation",
            choices = "Translation" )
    private String transformationTypeInput;

    @Parameter(label = "Transformation finding method",
            choices = "PhaseCorrelation" )
    private String transformationFindingMethodInput;

    @SuppressWarnings("unchecked")
    private MutableModuleItem<Long> varInput(final int d, final String var) {
        return (MutableModuleItem<Long>) getInfo().getInput( varName(d, var) );
    }

    @SuppressWarnings("unchecked")
    private MutableModuleItem<String> typeInput( final int d ) {
        return (MutableModuleItem<String>) getInfo().getInput( typeName( d ) );
    }

    @Parameter(label = "Compute registration", callback = "computeRegistration")
    private Button computeRegistrationButton;

    protected void computeRegistration() {

        int numDimensions = dataset.numDimensions();

        // Transformation type
        //
        TransformationTypes transformationType
                = TransformationTypes.valueOf( transformationTypeInput );

        TransformationFindingTypes transformationFindingMethod
                = TransformationFindingTypes.valueOf( transformationFindingMethodInput );

        // Get axis types, intervals and other coordinates from GUI
        //
        long[] min = Intervals.minAsLongArray( dataset );
        long[] max = Intervals.maxAsLongArray( dataset );
        long[] other = new long[ numDimensions ];
        RegistrationAxisTypes[] axisTypes = new RegistrationAxisTypes[ numDimensions ];

        for ( int d = 0; d < numDimensions; ++d )
        {
            axisTypes[ d ] = RegistrationAxisTypes.valueOf( typeInput( d ).getValue( this ) );
            min[ d ] = varInput( d, "min" ).getValue( this );
            max[ d ] = varInput( d, "max" ).getValue( this );
            other[ d ] = varInput( d, "other" ).getValue( this );
        }

        FinalInterval interval = new FinalInterval( min, max );

        // Configure image filtering
        //
        // TODO: Get from GUI
        Map< String, Object > imageFilterParameters = new HashMap<>();

        ImageFilterType imageFilterType = ImageFilterType.DOG_THRESHOLD;

        imageFilterParameters.put(
                ImageFilterParameters.GAUSS_SIGMA, new double[]{ 10.0D, 1.0D} );
        imageFilterParameters.put(
                ImageFilterParameters.THRESHOLD_VALUE, 20.0D );
        imageFilterParameters.put(
                ImageFilterParameters.DOG_SIGMA_SMALLER, new double[]{ 1.0D, 1.0D} );
        imageFilterParameters.put(
                ImageFilterParameters.DOG_SIGMA_LARGER, new double[]{ 10.0D, 10.0D} );


        boolean showFixedImageSequence = true;

        //imageFilterType = null;

        ImageRegistration imageRegistration =
                new ImageRegistration(
                        dataset,
                        axisTypes,
                        interval,
                        other,
                        3,
                        imageFilterType,
                        imageFilterParameters,
                        showFixedImageSequence );

        imageRegistration.run();


    }

    public void run() {
        uiService.log().info( "ImageRegistrationPlugin started" );
    }

    protected void init()
    {

        boolean persist = true;

        int n = dataset.numDimensions();

        List< String > axisTypes =
                Stream.of( RegistrationAxisTypes.values() )
                    .map( RegistrationAxisTypes::name )
                    .collect( Collectors.toList() );

        ArrayList< String > axisNames = ImageRegistrationParameters.getAxisNamesAsStringList( dataset );

        // Guess default axisType choices
        //
        String sequenceDefault = axisNames.get( 0 );
        if ( axisNames.contains( Axes.TIME.toString() ) )
        {
            sequenceDefault = Axes.TIME.toString();
        }
        else if ( axisNames.contains( Axes.Z.toString() ) )
        {
            sequenceDefault = Axes.Z.toString();
        }
        else if ( axisNames.contains( Axes.CHANNEL.toString() ) )
        {
            sequenceDefault = Axes.CHANNEL.toString();
        }

        for (int d = 0; d < dataset.numDimensions(); d++)
        {

            String var;

            // Message
            //
            final MutableModuleItem<String> axisItem =
                    addInput( axisNames.get( d ), String.class);
            axisItem.setPersisted( persist );
            axisItem.setVisibility( ItemVisibility.MESSAGE );
            axisItem.setValue(this, axisNames.get( d ) );

            // Axis type selection
            //
            var = "type";
            final MutableModuleItem<String> typeItem =
                    addInput( typeName( d ), String.class);
            typeItem.setPersisted( persist );
            typeItem.setLabel( "Type" );
            typeItem.setChoices( axisTypes );
            if ( axisNames.get( d ).equals( sequenceDefault ) )
                typeItem.setValue( this, "" + RegistrationAxisTypes.Sequence );
            else if ( axisNames.get( d ).equals( Axes.X.toString() ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Transformable );
            else if ( axisNames.get( d ).equals( Axes.Y.toString() ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Transformable );
            else if ( axisNames.get( d ).equals( Axes.Z.toString() ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Transformable );
            else if ( axisNames.get( d ).equals( Axes.CHANNEL.toString() ))
                typeItem.setValue( this, "" + RegistrationAxisTypes.Fixed );

            // Interval minimum
            //
            var = "min";
            final MutableModuleItem<Long> minItem =
                    addInput( varName(d, var), Long.class);
            minItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            minItem.setPersisted( persist );
            minItem.setLabel( var );
            minItem.setValue(this, dataset.min( d ));
            minItem.setMinimumValue( dataset.min( d ) );
            minItem.setMaximumValue( dataset.max( d ) );

            // Interval maximum
            //
            var = "max";
            final MutableModuleItem<Long> maxItem =
                    addInput( varName(d, var), Long.class);
            maxItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            maxItem.setPersisted( persist );
            maxItem.setLabel( var );
            maxItem.setValue(this, dataset.max( d ) );
            maxItem.setMinimumValue( dataset.min( d ) );
            maxItem.setMaximumValue( dataset.max( d ) );


            // Other
            // - Sequence axis: reference point
            // - Transformation axis: maximal displacement
            //
            var = "other";
            final MutableModuleItem<Long> otherItem =
                    addInput( varName(d, var), Long.class);
            otherItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            otherItem.setPersisted( persist );
            otherItem.setLabel( var );
            otherItem.setValue(this, dataset.max( d ) );
            otherItem.setMinimumValue( dataset.min( d ) );
            otherItem.setMaximumValue( dataset.max( d ) );


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

        boolean GUI = false;
        boolean TEST = false;


        ImporterOptions options = new ImporterOptions();
        options.setId( "/Users/tischi/Documents/fiji-plugin-imageRegistration--data/mri-stack/mri-stack.pattern");
        ImagePlus[] imps = BF.openImagePlus(options);
        for ( final ImagePlus imp2 : imps )
            imp2.show();


        // ask the user for a file to open
        //final File file = ij.ui().chooseFile(null, "open");

        final File file =
                new File( "/Users/tischi/Documents/fiji-plugin-imageRegistration--data/2d_t_2ch_drift_synthetic_blur.tif");

        Dataset dataset = null;
        int n = 0;
        if (file != null)
        {
            // load and show dataset
            //
            dataset = ij.scifio().datasetIO().open( file.getPath() );
            n = dataset.numDimensions();
            ij.ui().show( dataset );



//            IJ.run("Image Sequence...",
//                    "open=/Users/tischi/Documents/fiji-plugin-imageRegistration--data/mri-stack-16bit sort use");
//            ImagePlus imp = IJ.getImage(); n = 3;

            // convert of cellImg that is lazily loaded
            //
//            Img< UnsignedShortType > img = ConvertVirtualStackToCellImg.getCellImgUnsignedShort( imp );
//            ImgPlus< UnsignedShortType > imgp = new ImgPlus<>( img, "title", new AxisType[]{ Axes.X, Axes.Y, Axes.Z } );
////            ij.get(LegacyService.class).getImageMap().addMapping(  ); // but it's private...
//            //imp.hide(); ImageJFunctions.show( img );
           // ij.convert().convert( RAI, Img.class );
        }



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
            min[ i ] = 0; max[ i++ ] = 3; // sequence dimension: registration range

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

            ImageFilterType imageFilterType = ImageFilterType.DOG_THRESHOLD;

            imageFilterParameters.put(
                    ImageFilterParameters.GAUSS_SIGMA, new double[]{ 10.0D, 1.0D} );
            imageFilterParameters.put(
                    ImageFilterParameters.THRESHOLD_VALUE, 20.0D );
            imageFilterParameters.put(
                    ImageFilterParameters.DOG_SIGMA_SMALLER, new double[]{ 1.0D, 1.0D} );
            imageFilterParameters.put(
                    ImageFilterParameters.DOG_SIGMA_LARGER, new double[]{ 10.0D, 10.0D} );


            boolean showFixedImageSequence = true;

            //imageFilterType = null;

            ImageRegistration imageRegistration =
                    new ImageRegistration(
                            dataset,
                            registrationAxisTypes,
                            interval,
                            other,
                            3,
                            imageFilterType,
                            imageFilterParameters,
                            showFixedImageSequence );

            imageRegistration.run();

            ArrayList< AxisType > axisTypes = new ArrayList<>(  );
            for (int d = 0; d < dataset.numDimensions(); d++)
            {
                axisTypes.add( dataset.axis( d ).type() );
            }

            ArrayList< Integer > axesIdsFixedSequenceOutput = new ArrayList<>();
            RandomAccessibleInterval raiFSO = imageRegistration.getFixedSequenceOutput( axesIdsFixedSequenceOutput );
            showRAI(ij,
                    raiFSO,
                    "transformed reference sequence",
                    axesIdsFixedSequenceOutput,
                    axisTypes);

            ArrayList< Integer > axesIdsTransformedOutput = new ArrayList<>();
            RandomAccessibleInterval raiTO = imageRegistration.getTransformedOutput( axesIdsTransformedOutput );
            showRAI(ij,
                    raiTO,
                    "transformed input",
                    axesIdsTransformedOutput,
                    axisTypes);


        }

    }


    public static void showRAI (
            ImageJ ij,
            RandomAccessibleInterval rai,
            String title,
            ArrayList< Integer > axesIds,
            ArrayList< AxisType > axisTypesInputDataset )
    {
        Dataset dataset = ij.dataset().create( Views.zeroMin( rai ) );

        AxisType[] axisTypes = new AxisType[ axesIds.size() ];
        for ( int i = 0; i < axisTypes.length; ++i )
        {
            axisTypes[ i ] = axisTypesInputDataset.get( axesIds.get( i ) ) ;
        }

        ImgPlus img = new ImgPlus<>(
                dataset,
                title,
                axisTypes );

        ij.ui().show( img );
    }

}