package de.embl.cba.registration;

import bdv.util.AxisOrder;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Axes
{

    final private Dataset dataset;
    final private RegistrationAxisType[] registrationAxisTypes;
    final private FinalInterval registrationAxesInterval;
    final private int numDimensions;

    public Axes( Dataset dataset,
                 RegistrationAxisType[] registrationAxisTypes,
                 FinalInterval registrationAxesInterval )
    {
        this.dataset = dataset;
        this.registrationAxisTypes = registrationAxisTypes;
        this.registrationAxesInterval = registrationAxesInterval;
        this.numDimensions = dataset.numDimensions();
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

        for ( AxisType axisType : fixedDimensionsAxisTypes() )
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
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisType.Transformable ) )
            {
                min[ i ] = registrationAxesInterval.min( d );
                max[ i ] = registrationAxesInterval.max( d );
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
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisType.Transformable ) )
            {
                min[ i ] = dataset.min( d );
                max[ i ] = dataset.max( d );
                ++i;
            }
        }
        return new FinalInterval( min, max );
    }

    public int numFixedDimensions()
    {
        int numFixedDimensions = Collections.frequency( Arrays.asList( registrationAxisTypes ), RegistrationAxisType.Fixed );

        return numFixedDimensions;
    }

    public int numTransformableDimensions()
    {
        int numTransformableDimensions = Collections.frequency( Arrays.asList( registrationAxisTypes ), RegistrationAxisType.Transformable );

        return numTransformableDimensions;
    }

    public int fixedDimension( int i )
    {
        return fixedDimensions()[ i ];
    }

    public FinalInterval fixedDimensionsInterval()
    {

        if ( numFixedDimensions() > 0 )
        {
            long[] min = new long[ numFixedDimensions() ];
            long[] max = new long[ numFixedDimensions() ];

            for ( int d = 0, i = 0; d < numDimensions; ++d )
            {
                if ( registrationAxisTypes[ d ].equals( RegistrationAxisType.Fixed ) )
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
        long[] fixedReferenceCoordinates = new long[ numFixedDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisType.Fixed ) )
            {
                // Stored as interval in input, although the code currently only supports one fixed coordinate.
                // However, one could imagine in the future to e.g. average channels or compute
                // average transformations taking information from multiple channels into account...
                fixedReferenceCoordinates[ i++ ] = registrationAxesInterval.min( d );
            }
        }

        return fixedReferenceCoordinates;
    }

    public int[] fixedDimensions()
    {
        int[] fixedDimensions = new int[ numFixedDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisType.Fixed ) )
            {
                fixedDimensions[ i++ ] = d;
            }
        }

        return fixedDimensions;
    }




    public int[] transformableDimensions()
    {
        int[] dimensions = new int[ numTransformableDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisType.Transformable ) )
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
            if ( registrationAxisTypes[ d ].equals( registrationAxisType ) )
            {
                axisTypes.add( axisTypesInputImage().get( d ) );
            }
        }

        return axisTypes;
    }

    public long numSpatialDimensions( ArrayList< AxisType > axisTypes )
    {
        return axisTypes.stream().filter( x -> x.isSpatial() ).count();
    }

    public ArrayList< AxisType > fixedDimensionsAxisTypes()
    {
        return getAxisTypes( RegistrationAxisType.Fixed );
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
        int sequenceDimension = Arrays.asList( registrationAxisTypes ).indexOf( RegistrationAxisType.Sequence );

        return sequenceDimension;
    }

    public long sequenceMin()
    {
        return registrationAxesInterval.min( sequenceDimension() );
    }

    public long sequenceMax()
    {
        return registrationAxesInterval.max( sequenceDimension() );
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

    public ArrayList< AxisType > axisTypesInputImage()
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


}
