package de.embl.cba.registration;

import net.imglib2.FinalInterval;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class OutputInterval
{

    /*
    public FinalInterval getTransformableDimensionsOutputInterval()
    {
        if ( outputIntervalType == OutputIntervalType.InputImageSize )
        {
            return transformableAxesSettings.inputInterval;
        }
        else if ( outputIntervalType == OutputIntervalType.ReferenceRegionSize )
        {
            return transformableAxesSettings.referenceInterval;
        }
        else if ( outputIntervalType == OutputIntervalType.UnionSize )
        {
            return getTransformationsUnion( new FinalInterval(input) );
        }
        else
        {
            return null;
        }
    }*/

    private FinalInterval getTransformationsUnion( FinalInterval inputInterval )
    {
        // TODO
        ArrayList< long[] > corner = getCorners( inputInterval );
        return null;
    }

    private ArrayList< long[] > getCorners( FinalInterval interval )
    {
        // initUI input for recursive corner determination
        //
        ArrayList< long [] > corners = new ArrayList<>(  );
        LinkedHashMap< Integer, String > dimensionMinMaxMap = new LinkedHashMap<>();
        for ( int d = 0; d < interval.numDimensions(); ++d )
            dimensionMinMaxMap.put( d, null );

        setCorners( dimensionMinMaxMap,
                corners, interval );

        return corners;

    }

    private void setCorners( LinkedHashMap< Integer, String > axisMinMaxMap,
                             ArrayList< long [] > corners,
                             FinalInterval interval )
    {
        int n = interval.numDimensions();

        if ( axisMinMaxMap.containsValue( null ) )
        {   // there are still dimensions with undetermined corners
            for ( int d : axisMinMaxMap.keySet() )
            {
                if( axisMinMaxMap.get( d ) == null )
                {
                    axisMinMaxMap.put( d, "min" );
                    setCorners( axisMinMaxMap,
                            corners,
                            interval );

                    axisMinMaxMap.put( d, "max" );
                    setCorners( axisMinMaxMap,
                            corners,
                            interval );

                }
            }
        }
        else
        {
            long [] corner = new long[ interval.numDimensions() ];

            for ( int axis : axisMinMaxMap.keySet() )
            {
                if ( axisMinMaxMap.get( axis ).equals( "min" ) )
                {
                    corner[ axis ] = interval.min( axis );
                }
                else if ( axisMinMaxMap.get( axis ).equals( "max" ) )
                {
                    corner[ axis ] = interval.max( axis );
                }
            }

            corners.add( corner );

        }
    }


}
