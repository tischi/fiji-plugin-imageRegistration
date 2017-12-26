package de.embl.cba.registration.axessettings;

import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisTypes;
import net.imagej.Dataset;
import net.imglib2.FinalInterval;

import java.util.Arrays;
import java.util.Collections;

public class AxesSettings {


    final private Dataset dataset;
    final private RegistrationAxisTypes[] registrationAxisTypes;
    final private FinalInterval registrationAxesInterval;
    final private int numDimensions;

    // TODO: simplify below code!
    private final SequenceAxisSettings sequenceAxisSettings;
    private final TransformableAxesSettings transformableAxesSettings;
    private final FixedAxesSettings fixedAxesSettings;


    public AxesSettings( Dataset dataset,
                         RegistrationAxisTypes[] registrationAxisTypes,
                         FinalInterval registrationAxesInterval )
    {
        this.dataset = dataset;
        this.registrationAxisTypes = registrationAxisTypes;
        this.registrationAxesInterval = registrationAxesInterval;
        this.numDimensions = dataset.numDimensions();

        // Configure sequence axis
        //
        int sequenceDimension = Arrays.asList( registrationAxisTypes ).indexOf( RegistrationAxisTypes.Sequence );
        long sequenceAxisReferenceCoordinate = registrationAxesInterval.min( sequenceDimension );
        sequenceAxisSettings =
                new SequenceAxisSettings(
                        sequenceDimension,
                        registrationAxesInterval.min( sequenceDimension ),
                        registrationAxesInterval.max( sequenceDimension ),
                        sequenceAxisReferenceCoordinate);

        // Configure transformable axes
        //
        int numTransformableDimensions = Collections.frequency( Arrays.asList( registrationAxisTypes ), RegistrationAxisTypes.Transformable);
        int[] transformableDimensions = new int[ numTransformableDimensions ];
        long[] referenceIntervalMin = new long[ numTransformableDimensions ];
        long[] referenceIntervalMax = new long[ numTransformableDimensions ];
        long[] transformableDimensionsInputIntervalMin = new long[ numTransformableDimensions ];
        long[] transformableDimensionsInputIntervalMax = new long[ numTransformableDimensions ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisTypes.Transformable ) )
            {
                transformableDimensions[ i ] = d;
                referenceIntervalMin[ i ] = registrationAxesInterval.min( d );
                referenceIntervalMax[ i ] = registrationAxesInterval.max( d );
                transformableDimensionsInputIntervalMin[ i ] = this.input.min( d );
                transformableDimensionsInputIntervalMax[ i ] = this.input.max( d );
                ++i;
            }
        }


        FinalInterval transformableDimensionsFullInterval =
                new FinalInterval( transformableDimensionsInputIntervalMin, transformableDimensionsInputIntervalMax );

        transformableAxesSettings =
                new TransformableAxesSettings(
                        transformableDimensions,
                        transformableDimensionsReferenceInterval,
                        transformableDimensionsFullInterval
                );


        // Configure fixed axes
        //
        int numFixedDimensions = Collections.frequency( Arrays.asList( registrationAxisTypes ), RegistrationAxisTypes.Fixed );



        FinalInterval fixedDimensionsReferenceInterval =
                new FinalInterval(
                        fixedDimensionsReferenceIntervalMin,
                        fixedDimensionsReferenceIntervalMax );

        fixedAxesSettings =
                new FixedAxesSettings(
                        fixedDimensions,
                        fixedDimensionsReferenceInterval
                );

    }

    public FinalInterval transformableAxesReferenceInterval()
    {
        long[] min = new long[ numTransformableDimensions() ];
        long[] max = new long[ numTransformableDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisTypes.Transformable ) )
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
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisTypes.Transformable ) )
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
        int numFixedDimensions = Collections.frequency( Arrays.asList( registrationAxisTypes ), RegistrationAxisTypes.Fixed );

        return numFixedDimensions;
    }

    public int numTransformableDimensions()
    {
        int numTransformableDimensions = Collections.frequency( Arrays.asList( registrationAxisTypes ), RegistrationAxisTypes.Transformable );

        return numTransformableDimensions;
    }

    public int fixedDimension( int i )
    {
        return fixedDimensions()[ i ];
    }

    public FinalInterval fixedDimensionsInterval()
    {
        long[] min = new long[ numFixedDimensions() ];
        long[] max = new long[ numFixedDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisTypes.Fixed ) )
            {
                min[ i ] = dataset.min( d );
                max[ i ] = dataset.max( d );
                ++i;
            }
        }

        FinalInterval interval = new FinalInterval( min, max );

        return interval;
    }

    public long[] fixedReferenceCoordinates( )
    {
        long[] fixedReferenceCoordinates = new long[ numFixedDimensions() ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisTypes.Fixed ) )
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
            if ( registrationAxisTypes[ d ].equals( RegistrationAxisTypes.Fixed ) )
            {
                fixedDimensions[ i++ ] = d;
            }
        }

        return fixedDimensions;
    }


    public int sequenceDimension()
    {
        int sequenceDimension = Arrays.asList( registrationAxisTypes ).indexOf( RegistrationAxisTypes.Sequence );

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


}
