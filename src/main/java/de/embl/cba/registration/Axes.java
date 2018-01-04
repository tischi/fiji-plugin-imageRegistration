package de.embl.cba.registration;

import bdv.util.AxisOrder;
import de.embl.cba.registration.transform.Translation1D;
import de.embl.cba.registration.util.Transforms;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.Collections;

public class Axes
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

    public ArrayList< AxisType > transformedAxisTypes()
    {
        ArrayList< AxisType > axisTypes = new ArrayList<>(  );

        for ( AxisType axisType : transformableDimensionsAxisTypes() )
        {
            axisTypes.add( axisType );
        }

        axisTypes.add( sequenceDimensionAxisType() );

        for ( AxisType axisType : otherDimensionsAxisTypes() )
        {
            axisTypes.add( axisType );
        }

        return axisTypes;
    }

    public FinalInterval transformableAxesReferenceInterval()
    {
        long[] min = new long[ numRegistrationDimensions() ];
        long[] max = new long[ numRegistrationDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Registration ) )
            {
                min[ i ] = referenceInterval.min( d );
                max[ i ] = referenceInterval.max( d );
                ++i;
            }
        }

        return new FinalInterval( min, max );
    }

    public FinalInterval transformableAxesInputInterval()
    {
        long[] min = new long[ numRegistrationDimensions() ];
        long[] max = new long[ numRegistrationDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Registration ) )
            {
                min[ i ] = input.min( d );
                max[ i ] = input.max( d );
                ++i;
            }
        }
        return new FinalInterval( min, max );
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

    public FinalInterval nonTransformableAxesInterval()
    {
        if ( nonSequenceNonSpatialAxesInputInterval == null )
        {
            int numNonSequenceNonSpatialDimensions = numNonTransformableDimensions();

            if ( numNonSequenceNonSpatialDimensions > 0 )
            {
                long[] min = new long[ numNonSequenceNonSpatialDimensions ];
                long[] max = new long[ numNonSequenceNonSpatialDimensions ];

                for ( int d = 0, i = 0; d < numDimensions; ++d )
                {
                    if ( ! registrationAxisTypes.get( d ).equals( RegistrationAxisType.Sequence ) && !axisTypes.get( d ).isSpatial() )
                    {
                        min[ i ] = input.min( d );
                        max[ i ] = input.max( d );
                        ++i;
                    }
                }
                nonSequenceNonSpatialAxesInputInterval = new FinalInterval( min, max );
            }
            else
            {
                nonSequenceNonSpatialAxesInputInterval = new FinalInterval( new long[]{ 0 }, new long[]{ 0 } );
            }
        }

        return nonSequenceNonSpatialAxesInputInterval;

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
                // average transformations taking information from multiple channels into account...
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

    public ArrayList< AxisType > otherDimensionsAxisTypes()
    {
        return getAxisTypes( RegistrationAxisType.Other );
    }

    public ArrayList< AxisType > transformableDimensionsAxisTypes()
    {
        return getAxisTypes( RegistrationAxisType.Registration );
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
        return referenceInterval.min( sequenceDimension() );
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
        return referenceInterval.max( sequenceDimension() );
    }

    public long sequenceIncrement()
    {
        return 1;
    }

    public FinalInterval transformableAxesInterval( OutputIntervalType outputIntervalType )
    {
        if ( outputIntervalType.equals( OutputIntervalType.InputImageSize ) )
        {
            return transformableAxesInputInterval();
        }
        else if ( outputIntervalType.equals( OutputIntervalType.ReferenceRegionSize ) )
        {
            return transformableAxesReferenceInterval();
        }
        else
        {
            return null; // TODO
        }

    }

    public ArrayList< AxisType > inputAxisTypes()
    {
        return axisTypes;
    }

    public AxisOrder axisOrderAfterTransformation()
    {
        ArrayList< AxisType > axisTypes = transformedAxisTypes();

        AxisOrder axisOrder = axisOrder( axisTypes );

        return axisOrder;
    }

    /**
     * Create an AxisOrder for showing the dataset with the BigDataViewer.
     * @param axisTypes
     * @return
     */
    public static AxisOrder axisOrder( ArrayList< AxisType > axisTypes )
    {
        String axisOrderString = "";
        for ( AxisType axisType : axisTypes )
        {
            axisOrderString += axisType.getLabel();
        }

        axisOrderString = axisOrderString.replace( "Time", "T" );
        axisOrderString = axisOrderString.replace( "Channel", "C" );

        AxisOrder axisOrder;

        try
        {
            axisOrder = AxisOrder.valueOf(  axisOrderString );
        }
        catch ( Exception e ) // BDV does not support all axis orders
        {
            axisOrder = AxisOrder.DEFAULT;
        }

        return axisOrder;
    }


    public FinalInterval getReferenceIntervalForAxis( int axis )
    {
        long[] min = new long[]{ getReferenceInterval().min( axis ) };
        long[] max = new long[]{ getReferenceInterval().max( axis ) };
        return new FinalInterval( min, max );
    }


    public InvertibleRealTransform expandTransformToAllSpatialDimensions( InvertibleRealTransform transform )
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

                return expandedTransform;
            }
        }
        else
        {
            return transform;
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
