package de.embl.cba.registration;

import bdv.util.AxisOrder;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;

import java.util.ArrayList;
import java.util.Collections;

public class Axes
{

    final private Dataset dataset;
    final private ArrayList< RegistrationAxisType > registrationAxisTypes;
    final private FinalInterval referenceInterval;
    final private int numDimensions;

    public Axes( Dataset dataset,
                 ArrayList< RegistrationAxisType > registrationAxisTypes,
                 FinalInterval referenceInterval )
    {
        this.dataset = dataset;
        this.registrationAxisTypes = registrationAxisTypes;
        this.referenceInterval = referenceInterval;
        this.numDimensions = dataset.numDimensions();
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

        if ( sequenceMax() - sequenceMin() > 1 )
        {
            axisTypes.add( sequenceDimensionAxisType() );
        }

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
        long[] min = new long[ numTransformableDimensions() ];
        long[] max = new long[ numTransformableDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Transformable ) )
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
        long[] min = new long[ numTransformableDimensions() ];
        long[] max = new long[ numTransformableDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Transformable ) )
            {
                min[ i ] = dataset.min( d );
                max[ i ] = dataset.max( d );
                ++i;
            }
        }
        return new FinalInterval( min, max );
    }

    public int numOtherDimensions()
    {
        return Collections.frequency( registrationAxisTypes, RegistrationAxisType.Other );
    }

    public int numTransformableDimensions()
    {
        return Collections.frequency( registrationAxisTypes, RegistrationAxisType.Transformable );
    }

    public int fixedDimension( int i )
    {
        return otherAxes().get( i );
    }

    public FinalInterval fixedAxesReferenceInterval()
    {
        long[] min = new long[ numOtherDimensions() ];
        long[] max = new long[ numOtherDimensions() ];

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

    public FinalInterval otherAxesInputInterval()
    {
        if ( numOtherDimensions() > 0 )
        {
            long[] min = new long[ numOtherDimensions() ];
            long[] max = new long[ numOtherDimensions() ];

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Other ) )
                {
                    min[ i ] = dataset.min( d );
                    max[ i ] = dataset.max( d );
                    ++i;
                }
            }

            return new FinalInterval( min, max );
        }
        else
        {
            return new FinalInterval( new long[]{0}, new long[]{0} );
        }


    }

    public long[] fixedReferenceCoordinates( )
    {
        long[] fixedReferenceCoordinates = new long[ numOtherDimensions() ];

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



    public int[] transformableDimensions()
    {
        int[] dimensions = new int[ numTransformableDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes.get( d ).equals( RegistrationAxisType.Transformable ) )
            {
                dimensions[ i++ ] = d;
            }
        }

        return dimensions;
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

    public ArrayList< AxisType > otherDimensionsAxisTypes()
    {
        return getAxisTypes( RegistrationAxisType.Other );
    }

    public ArrayList< AxisType > transformableDimensionsAxisTypes()
    {
        return getAxisTypes( RegistrationAxisType.Transformable );
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

    public long sequenceMax()
    {
        return referenceInterval.max( sequenceDimension() );
    }

    public long sequenceIncrement()
    {
        return 1;
    }

    public FinalInterval transformableDimensionsOutputInterval( OutputIntervalType outputIntervalType )
    {

        if ( outputIntervalType.equals( OutputIntervalType.InputDataSize ) )
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
        return axisTypesList( dataset );
    }

    public AxisOrder axisOrderAfterTransformation()
    {
        ArrayList< AxisType > axisTypes = transformedAxisTypes();

        AxisOrder axisOrder = axisOrder( axisTypes );

        return axisOrder;

    }

    public AxisOrder axisOrder( ArrayList< AxisType > axisTypes )
    {
        String axisOrderString = "";
        for ( AxisType axisType : axisTypes )
        {
            axisOrderString += axisType.getLabel();
        }

        axisOrderString = axisOrderString.replace( "Time", "T" );
        axisOrderString = axisOrderString.replace( "Channel", "C" );

        return AxisOrder.valueOf(  axisOrderString );
    }


    public FinalInterval getReferenceIntervalForAxis( int axis )
    {
        long[] min = new long[]{ getReferenceInterval().min( axis ) };
        long[] max = new long[]{ getReferenceInterval().max( axis ) };
        return new FinalInterval( min, max );
    }
}
