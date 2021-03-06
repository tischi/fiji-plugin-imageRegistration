package de.embl.cba.registration;

import de.embl.cba.registration.projection.ProjectionType;
import de.embl.cba.registration.ui.Settings;
import de.embl.cba.registration.projection.Projection;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.*;

// TODO: rearranging the axes does not work for the uiService, why?
//AxisType[] transformedOutputAxisTypes = axes.inputAxisTypes().toArray( new AxisType[0] );

public class InputViews
        < R extends RealType< R > & NativeType< R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > > {

    final RandomAccessibleInterval input;
    final Axes axes;
    Map< Long, T > transforms;
    RandomAccessibleInterval< R > transformed;
    private Map< Long, T > transformsExpandedToAllTransformableDimensions;
    private FinalInterval transformedViewInterval;
    private ProjectionType projectionType;

    public InputViews( RandomAccessibleInterval inputImage, Axes axes, Settings settings )
    {
        this.input = inputImage;
        this.axes = axes;
        this.projectionType = settings.projectionType;
    }

    public ImgPlus< R > asImgPlus( RandomAccessibleInterval< R > rai, ArrayList< AxisType> axisTypes, String title )
    {
        Dataset dataset = Services.datasetService.create( Views.zeroMin( rai ) );
        ImgPlus< R > imgPlus = new ImgPlus( dataset, title, axisTypes.toArray( new AxisType[0] ) );
        return imgPlus;
    }

    public RandomAccessible< R > transformExtendingOutOfBoundsPixels( RandomAccessibleInterval< R > rai, InvertibleRealTransform transform )
    {
        ExtendedRandomAccessibleInterval erai = Views.extendBorder( rai );

        RealRandomAccessible rra = Views.interpolate( erai, new NLinearInterpolatorFactory() );
        RealRandomAccessible realTransformed = RealViews.transform( rra, transform );
        RandomAccessible transformed = Views.raster( realTransformed );

        return transformed;
    }


    public RandomAccessible< R > transformZeroingOutOfBoundsPixels( RandomAccessibleInterval< R > rai, InvertibleRealTransform transform )
    {
        ExtendedRandomAccessibleInterval erai = Views.extendZero( rai );
        RealRandomAccessible rra = Views.interpolate( erai, new NLinearInterpolatorFactory() );
        RealRandomAccessible realTransformed = RealViews.transform( rra, transform );
        RandomAccessible transformed = Views.raster( realTransformed );
        return transformed;
    }

    public static RandomAccessible transformExtendingOutOfBoundsPixels( RandomAccessible input, InvertibleRealTransform transform )
    {
        RealRandomAccessible rra = Views.interpolate( input, new NLinearInterpolatorFactory() );

        rra = RealViews.transform( rra, transform );

        RandomAccessible output = Views.raster( rra );

        return output;
    }

    public RandomAccessibleInterval transformableReferenceHyperSlice( long s )
    {
        RandomAccessibleInterval rai = fixedSequenceAxisView( s );

        rai = otherAxesProjections( rai );

        rai = Views.dropSingletonDimensions( rai );

        return rai;

    }

    private RandomAccessibleInterval otherAxesProjections( RandomAccessibleInterval rai )
    {
        ArrayList< Integer > otherAxes = axes.otherAxes();
        Collections.reverse( otherAxes );

        RandomAccessibleInterval projected = rai;

        for( int axis : otherAxes )
        {
            FinalInterval interval = axes.getReferenceIntervalForAxis( axis );
            Projection< R > projection = new Projection< R >( projected, axis, interval );

            switch( projectionType )
            {
                case Average: projected = projection.average(); break;
                case Median: projected = projection.median(); break;
                case Maximum: projected = projection.maximum(); break;
                case Minimum: projected = projection.minimum(); break;
                default: throw new IllegalArgumentException("Invalid projection: " + projectionType);
            }

        }

        return projected;

    }

    public RandomAccessibleInterval applyIntervalAndDropSingletons( FinalInterval interval )
    {
        RandomAccessibleInterval rai = Views.interval( input, interval );

        rai = Views.dropSingletonDimensions( rai );

        return rai;
    }

    private RandomAccessibleInterval fixedSequenceAxisView( long s )
    {
        FinalInterval interval = axes.fixedSequenceAxisInterval( s );
        return Views.interval( input, interval );
    }

    private RandomAccessibleInterval transformedHyperSliceZeroingOutOfBoundsPixels( FinalInterval nonTransformableSingletonsInterval, InvertibleRealTransform transform, FinalInterval viewInterval )
    {
        RandomAccessibleInterval rai = applyIntervalAndDropSingletons( nonTransformableSingletonsInterval );

        RandomAccessible ra = transformZeroingOutOfBoundsPixels( rai, transform );

        return Views.interval( ra, viewInterval );
    }

    public RandomAccessibleInterval< R > transformed( Map< Long, T > transforms, OutputIntervalSizeType outputIntervalSizeType )
    {
        setTransforms( transforms );

        setTransformedViewInterval( outputIntervalSizeType );

        createTransformedViewOfInputImage();

        rearrangeTransformedViewAxesIntoSameOrderAsInputImage();

        return transformed;
    }

    private void createTransformedViewOfInputImage()
    {
        long[] nonTransformableCoordinates = new long[ axes.nonTransformableAxes().size() ];
        transformed = transformedSequences( nonTransformableCoordinates, -1 );
    }

    private void setTransformedViewInterval( OutputIntervalSizeType outputIntervalSizeType )
    {
        transformedViewInterval = axes.transformableAxesInterval( outputIntervalSizeType, transformsExpandedToAllTransformableDimensions );
    }

    private void setTransforms( Map< Long, T > transforms )
    {
        this.transforms = transforms;
        transformsExpandedToAllTransformableDimensions = new LinkedHashMap<>( transforms );
        for ( Long s : transforms.keySet() )
        {
            T expandedTransform = ( T ) axes.expandTransformToAllSpatialDimensions( transforms.get( s ) );
            transformsExpandedToAllTransformableDimensions.put( s, expandedTransform );
        }
    }

    private RandomAccessibleInterval< R > transformedSequences( long[] nonTransformableCoordinates, int loopingDimension )
    {
        loopingDimension++;

        ArrayList< RandomAccessibleInterval<R> > transformedSequenceList = new ArrayList<>(  );

        long min = axes.nonTransformableAxesInterval().min( loopingDimension );
        long max = axes.nonTransformableAxesInterval().max( loopingDimension );

        for ( long coordinate = min; coordinate <= max; ++coordinate )
        {
            RandomAccessibleInterval transformed = null;

            nonTransformableCoordinates[ loopingDimension ] = coordinate;

            if ( loopingDimension == nonTransformableCoordinates.length - 1 )
            {
                long s = axes.sequenceCoordinate( nonTransformableCoordinates );

                if ( transformsExpandedToAllTransformableDimensions.containsKey( s ) )
                {
                    FinalInterval nonTransformableSingletonsInterval = axes.nonTransformableAxesSingletonInterval( nonTransformableCoordinates );
                    transformed = transformedHyperSliceZeroingOutOfBoundsPixels( nonTransformableSingletonsInterval, transformsExpandedToAllTransformableDimensions.get( s ), transformedViewInterval );
                }
            }
            else
            {
                transformed = transformedSequences( nonTransformableCoordinates, loopingDimension );
            }

            if ( transformed != null )
            {
                transformedSequenceList.add( transformed );
            }

        }

        RandomAccessibleInterval rai = stackAndDropSingletons( transformedSequenceList );

        return rai;
    }

    private void rearrangeTransformedViewAxesIntoSameOrderAsInputImage( )
    {
        // TODO: This code assumes that axistypes within one dataset are unique; is this true?

        ArrayList< AxisType > transformedAxisTypes = axes.transformedOutputAxisTypes();
        ArrayList< AxisType > inputAxisTypes = axes.inputAxisTypes();

        for ( int inputDimension = 0; inputDimension < inputAxisTypes.size(); ++inputDimension )
        {
            int transformedDimension = transformedAxisTypes.indexOf( inputAxisTypes.get( inputDimension ) );
            Collections.swap( transformedAxisTypes, inputDimension, transformedDimension );
            transformed = Views.permute( transformed, inputDimension, transformedDimension );
        }

    }

    public static  < R extends RealType< R > & NativeType< R > > RandomAccessibleInterval< R > stackAndDropSingletons( ArrayList< RandomAccessibleInterval< R > > transformedList )
    {
        RandomAccessibleInterval rai = Views.stack( transformedList );
        rai = Views.dropSingletonDimensions( rai );
        return rai;
    }


}
