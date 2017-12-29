package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.registration.filter.ImageFilterParameters.*;

public class FilterSequence
{
    private final ArrayList< ImageFilter > imageFilters;

    public FilterSequence( Map< String, Object > parameters )
    {
        imageFilters = new ArrayList<>( );

        ArrayList< ImageFilterType > filterTypes = (ArrayList< ImageFilterType >) parameters.get( SEQUENCE );

        for ( ImageFilterType filterType : filterTypes )
        {
            ImageFilter imageFilter = ImageFilterFactory.create( filterType, parameters );
            imageFilters.add( imageFilter );
        }

    }

    public < R extends RealType< R > > RandomAccessibleInterval< R > apply( RandomAccessibleInterval< R > input )
    {
        RandomAccessibleInterval< R > output = input;

        for ( ImageFilter imageFilter : imageFilters )
        {
            output = imageFilter.apply( output );
        }

        return output;
    }
}
