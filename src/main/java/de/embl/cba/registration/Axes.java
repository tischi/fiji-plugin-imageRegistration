package de.embl.cba.registration;

import bdv.util.AxisOrder;
import de.embl.cba.registration.util.IntervalUtils;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Axes < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{

    final private ArrayList< RegistrationAxisType > registrationAxisTypes;
    final private ArrayList< AxisType > axisTypes;

    final private FinalInterval referenceInterval;
    final private int numDimensions;
    final private RandomAccessibleInterval input;
    private FinalInterval nonSequenceNonSpatialAxesInputInterval;
    private Integer numNonTransformableDimensions;
    private ArrayList< Integer > nonTransformableAxes;
    private Integer sequenceDimensionWithinNonTransformableDimensions;
    private ArrayList< Integer > spatialAxes;
    private ArrayList< Integer > registrationAxes;
    private ArrayList< Integer > transformableAxes;
    private FinalInterval transformableAxesInputInterval;
    private FinalInterval registrationAxesReferenceInterval;
    private FinalInterval nonTransformableAxesInterval;
    private Long sequenceMin;
    private Long sequenceMax;

    public Axes( RandomAccessibleInterval input,
                 ArrayList< RegistrationAxisType > registrationAxisTypes,
                 ArrayList< AxisType > axisTypes,
                 FinalInterval referenceInterval )
    {
        this.input = input;
        this.registrationAxisTypes = registrationAxisTypes;
        this.axisTypes = axisTypes;
        this.referenceInterval = referenceInterval;
        this.numDimensions = registrationAxisTypes.size();
    }

    public static ArrayList< AxisType > getAxisTypes( ImagePlus imagePlus )
    {
        ArrayList< AxisType > axisTypes = new ArrayList<>(  );
        if ( imagePlus.getWidth() > 0 ) axisTypes.add( net.imagej.axis.Axes.X );
        if ( imagePlus.getHeight() > 0 ) axisTypes.add( net.imagej.axis.Axes.Y );
        if ( imagePlus.getNChannels() > 1 ) axisTypes.add( net.imagej.axis.Axes.CHANNEL );
        if ( imagePlus.getNSlices() > 1 ) axisTypes.add( net.imagej.axis.Axes.Z );
        if ( imagePlus.getNFrames() > 1 ) axisTypes.add( net.imagej.axis.Axes.TIME );
        return axisTypes;
    }

    public FinalInterval fixedSequenceAxisInterval( long s )
    {
        long[] min = Intervals.minAsLongArray( input );
        long[] max = Intervals.maxAsLongArray( input );
        setSequenceAxisCoordinate( s, min, max );
        return new FinalInterval( min, max );
    }

    public void setSequenceAxisCoordinate( long s, long[] min, long[] max )
    {
        min[ sequenceDimension() ] = s;
        max[ sequenceDimension() ] = s;
    }

    public FinalInterval getReferenceInterval()
    {
        return referenceInterval;
    }

    public static ArrayList< AxisType > axisTypesList( Dataset dataset )
    {
        ArrayList< AxisType > axisTypes = new ArrayList<>(  );
        for (int d = 0; d < dataset.numDimensions(); d++)
        {
            axisTypes.add( dataset.axis( d ).type() );
        }

        return axisTypes;
    }

    public ArrayList< AxisType > referenceAxisTypes()
    {
        ArrayList< AxisType > axisTypes = transformableDimensionsAxisTypes();
        axisTypes.add( sequenceDimensionAxisType() );
        return axisTypes;
    }

    public ArrayList< AxisType > transformedOutputAxisTypes()
    {
        ArrayList< AxisType > axisTypes = new ArrayList<>(  );

        for ( AxisType axisType : transformableDimensionsAxisTypes() )
        {
            axisTypes.add( axisType );
        }

        axisTypes.add( sequenceDimensionAxisType() );

        for ( AxisType axisType : nonTransformableDimensionsAxisTypes() )
        {
            if ( ! axisType.equals( sequenceDimensionAxisType() ) )
            {
                axisTypes.add( axisType );
            }
        }

        return axisTypes;
    }

    public FinalInterval registrationAxesReferenceInterval()
    {
        if ( registrationAxesReferenceInterval == null )
        {
            long[] min = new long[ registrationAxes().size() ];
            long[] max = new long[ registrationAxes().size() ];

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( registrationAxes().contains( d ) )
                {
                    min[ i ] = referenceInterval.min( d );
                    max[ i ] = referenceInterval.max( d );
                    ++i;
                }
            }

            registrationAxesReferenceInterval = new FinalInterval( min, max );
        }

        return registrationAxesReferenceInterval;
    }

    public FinalInterval nonTransformableAxesInterval()
    {
        if ( nonTransformableAxesInterval == null )
        {
            long[] min = new long[ transformableAxes().size() ];
            long[] max = new long[ transformableAxes().size() ];

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( nonTransformableAxes().contains( d ) )
                {
                    min[ i ] = input.min( d );
                    max[ i ] = input.max( d );
                    ++i;
                }
            }

            nonTransformableAxesInterval = new FinalInterval( min, max );

        }

        return nonTransformableAxesInterval;
    }

    public FinalInterval transformableAxesInterval()
    {
        if ( transformableAxesInputInterval == null )
        {
            long[] min = new long[ transformableAxes().size() ];
            long[] max = new long[ transformableAxes().size() ];

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( transformableAxes().contains( d ) )
                {
                    min[ i ] = input.min( d );
                    max[ i ] = input.max( d );
                    ++i;
                }
            }
            transformableAxesInputInterval = new FinalInterval( min, max );
        }

        return transformableAxesInputInterval;
    }


    public int numNonTransformableDimensions()
    {
        return nonTransformableAxes().size();
    }

    public long sequenceCoordinate( long[] nonTransformableCoordinates )
    {
        assert nonTransformableCoordinates.length == nonTransformableAxes().size();

        return nonTransformableCoordinates[ sequenceDimensionWithinNonTransformableDimensions ];
    }


    public int numRegistrationDimensions()
    {
        return registrationAxes().size();
    }

    public int fixedDimension( int i )
    {
        return otherAxes().get( i );
    }

    public FinalInterval fixedAxesReferenceInterval()
    {
        long[] min = new long[ numNonTransformableDimensions() ];
        long[] max = new long[ numNonTransformableDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Other ) )
            {
                min[ i ] = referenceInterval.min( d );
                max[ i ] = referenceInterval.max( d );
                ++i;
            }
        }

        return new FinalInterval( min, max );
    }


    public long[] fixedReferenceCoordinates( )
    {
        long[] fixedReferenceCoordinates = new long[ numNonTransformableDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Other ) )
            {
                // Stored as interval in input, although the code currently only supports one fixed coordinate.
                // However, one could imagine in the future to e.g. average channels or average
                // average transforms taking information from multiple channels into account...
                fixedReferenceCoordinates[ i++ ] = referenceInterval.min( d );
            }
        }

        return fixedReferenceCoordinates;
    }

    public ArrayList< Integer > otherAxes()
    {
        ArrayList< Integer > otherDimensions = new ArrayList<>(  );

        for ( int d = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Other ) )
            {
                otherDimensions.add( d );
            }
        }

        return otherDimensions;
    }

    public ArrayList< Integer > nonTransformableAxes()
    {
        if ( nonTransformableAxes == null )
        {
            nonTransformableAxes = new ArrayList<>(  );

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Sequence ) || ! axisTypes.get( d ).isSpatial() )
                {
                    nonTransformableAxes.add( d );

                    if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Sequence ) )
                    {
                        sequenceDimensionWithinNonTransformableDimensions = i;
                    }

                    i++;
                }
            }
        }

        return nonTransformableAxes;
    }

    public ArrayList< Integer > registrationAxes()
    {
        if ( registrationAxes == null )
        {
            registrationAxes = new ArrayList<>(  );

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Registration ) )
                {
                    registrationAxes.add( d );
                }
            }
        }

        return registrationAxes;
    }

    public ArrayList< Integer > transformableAxes()
    {
        if ( transformableAxes == null )
        {
            transformableAxes = new ArrayList<>(  );

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( axisTypes.get( d ).isSpatial() && d != sequenceDimension() )
                {
                    transformableAxes.add( d );
                }
            }
        }

        return transformableAxes;
    }

    public ArrayList< Integer > spatialAxes()
    {
        if ( spatialAxes == null )
        {
            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( axisTypes.get( d ).isSpatial() )
                {
                    spatialAxes.add( d );
                }
            }
        }

        return spatialAxes;
    }

    private ArrayList< AxisType > getAxisTypes( RegistrationAxisType registrationAxisType )
    {
        ArrayList< AxisType > axisTypes = new ArrayList<>();

        for ( int d = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( registrationAxisType ) )
            {
                axisTypes.add( inputAxisTypes().get( d ) );
            }
        }

        return axisTypes;
    }

    public long numSpatialDimensions( ArrayList< AxisType > axisTypes )
    {
        return axisTypes.stream().filter( x -> x.isSpatial() ).count();
    }

    public int numSpatialDimensions( )
    {
        return spatialAxes().size();
    }

    public long numTransformableDimensions()
    {
        return transformableAxes().size();
    }

    public ArrayList< AxisType > nonTransformableDimensionsAxisTypes()
    {
        ArrayList< AxisType > nonTransformableDimensionsAxisTypes = new ArrayList<>(  );

        for ( int d : nonTransformableAxes() )
        {
            nonTransformableDimensionsAxisTypes.add( axisTypes.get( d ) );
        }
        return nonTransformableDimensionsAxisTypes;
    }

    public ArrayList< AxisType > transformableDimensionsAxisTypes()
    {
        ArrayList< AxisType > transformableDimensionsAxisTypes = new ArrayList<>(  );

        for ( int d : transformableAxes() )
        {
            transformableDimensionsAxisTypes.add( axisTypes.get( d ) );

        }
        return transformableDimensionsAxisTypes;
    }

    public AxisType sequenceDimensionAxisType()
    {
        ArrayList< AxisType > axisTypes = getAxisTypes( RegistrationAxisType.Sequence );

        return axisTypes.get( 0 );
    }


    public int sequenceDimension()
    {
        return registrationAxisTypes.indexOf( RegistrationAxisType.Sequence );
    }

    public long sequenceMin()
    {
        if ( sequenceMin == null )
        {
            sequenceMin = referenceInterval.min( sequenceDimension() );
        }
        return sequenceMin;
    }

    public ArrayList< Long > sequenceCoordinates()
    {
        ArrayList< Long > sequenceCoordinates = new ArrayList<>(  );

        for ( long s = sequenceMin(); s <= sequenceMax(); s += sequenceIncrement() )
        {
            sequenceCoordinates.add( s );
        }

        return sequenceCoordinates;
    }

    public long sequenceMax()
    {
        if ( sequenceMax == null )
        {
            sequenceMax = referenceInterval.max( sequenceDimension() );
        }
        return sequenceMax;
    }

    public long sequenceIncrement()
    {
        return 1;
    }

    public FinalInterval transformableAxesInterval( OutputIntervalSizeType outputIntervalSizeType, Map< Long, T > transforms )
    {
        if ( outputIntervalSizeType.equals( OutputIntervalSizeType.InputImage ) )
        {
            return transformableAxesInterval();
        }

        if ( outputIntervalSizeType.equals( OutputIntervalSizeType.TransformationsEncompassing ) )
        {
            return transformationsEncompassingInterval( transformableAxesInterval(), transforms );
        }

        return null;

    }


    public FinalInterval transformationsEncompassingInterval( FinalInterval interval, Map< Long, T > transforms )
    {
        FinalInterval union = IntervalUtils.copy( interval );

        for ( T transform : transforms.values() )
        {
            FinalInterval bounding = boundingIntervalAfterTransformation( interval, transform );
            union = Intervals.union( bounding, union );
        }

        return union;
    }

    public FinalInterval boundingIntervalAfterTransformation( FinalInterval interval, T transform )
    {
        List< long[ ] > corners = Corners.corners( interval );

        long[] boundingMin = Intervals.minAsLongArray( interval );
        long[] boundingMax = Intervals.maxAsLongArray( interval );

        for ( long[] corner : corners )
        {
            double[] transformedCorner = transformedCorner( transform, corner );

            adjustBoundingRange( boundingMin, boundingMax, transformedCorner );
        }

        return new FinalInterval( boundingMin, boundingMax );
    }

    private void adjustBoundingRange( long[] min, long[] max, double[] transformedCorner )
    {
        for ( int d = 0; d < transformedCorner.length; ++d )
        {
            if ( transformedCorner[ d ] > max[ d ] )
            {
                max[ d ] = (long) transformedCorner[ d ];
            }

            if ( transformedCorner[ d ] < min[ d ] )
            {
                min[ d ] = (long) transformedCorner[ d ];
            }
        }
    }

    private double[] transformedCorner( T transform, long[] corner )
    {
        double[] cornerAsDouble = Arrays.stream( corner ).mapToDouble( x -> x ).toArray();
        double[] transformedCorner = new double[ corner.length ];
        transform.apply( cornerAsDouble, transformedCorner );
        return transformedCorner;
    }

    public ArrayList< AxisType > inputAxisTypes()
    {
        return axisTypes;
    }

    public String axisOrderAfterTransformation()
    {
        ArrayList< AxisType > axisTypes = transformedOutputAxisTypes();

        String axisOrder = axisOrder( axisTypes );

        return axisOrder;
    }

    /**
     * Create an AxisOrder for showing the dataset with the BigDataViewer.
     * @param axisTypes
     * @return
     */
    public static String axisOrder( ArrayList< AxisType > axisTypes )
    {
        String axisOrderString = "";
        for ( AxisType axisType : axisTypes )
        {
            axisOrderString += axisType.getLabel();
        }

        axisOrderString = axisOrderString.replace( "Time", "T" );
        axisOrderString = axisOrderString.replace( "Channel", "C" );

        return axisOrderString;
    }

    public FinalInterval getReferenceIntervalForAxis( int axis )
    {
        long[] min = new long[]{ getReferenceInterval().min( axis ) };
        long[] max = new long[]{ getReferenceInterval().max( axis ) };
        return new FinalInterval( min, max );
    }

    public T expandTransformToAllSpatialDimensions( T transform )
    {
        if ( numTransformableDimensions() > numRegistrationDimensions() )
        {
            if ( numTransformableDimensions() == 2 )
            {
                AffineTransform2D expandedTransform = new AffineTransform2D();

                if ( numRegistrationDimensions() == 1 )
                {
                    double translation = ( ( AffineTransform ) transform ).get( 0, 1 );

                    if ( registrationAxes().get( 0 ) == transformableAxes().get( 0 ) )
                    {
                        expandedTransform.translate( translation, 0 );
                    }

                    if ( registrationAxes().get( 0 ) == transformableAxes().get( 1 ) )
                    {
                        expandedTransform.translate( 0, translation );
                    }
                }

                return ( T ) expandedTransform;
            }
        }
        else
        {
            return ( T ) transform;
        }

        return null;

    }

    FinalInterval nonTransformableAxesSingletonInterval( long[] nonTransformableCoordinates )
    {
        long[] min = Intervals.minAsLongArray( input );
        long[] max = Intervals.maxAsLongArray( input );

        for ( int i = 0; i < numNonTransformableDimensions(); ++i )
        {
            int d = nonTransformableAxes.get( i );
            min[ d ] = nonTransformableCoordinates[ i ];
            max[ d ] = nonTransformableCoordinates[ i ];
        }

        return new FinalInterval( min, max );
    }

    private void setNonSequenceNonSpatialCoordinates( long[] nonSequenceNonSpatialCoordinates, long[] min, long[] max )
    {
        for ( int i = 0; i < numNonTransformableDimensions(); ++i )
        {
            int d = nonTransformableAxes.get( i );
            min[ d ] = nonSequenceNonSpatialCoordinates[ i ];
            max[ d ] = nonSequenceNonSpatialCoordinates[ i ];
        }
    }
}
