package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;

public class FilterSequence
{
    private final ArrayList< ImageFilter > imageFilters;

    public FilterSequence( FilterSettings settings )
    {
        imageFilters = new ArrayList<>( );

        for ( FilterType filterType : settings.filterTypes )
        {
            ImageFilter imageFilter = ImageFilterFactory.create( filterType, settings );
            imageFilters.add( imageFilter );
        }

    }

    public < R extends RealType< R > > RandomAccessibleInterval< R > apply( RandomAccessibleInterval< R > input )
    {
        RandomAccessibleInterval< R > output = input;

        for ( ImageFilter imageFilter : imageFilters )
        {
            output = imageFilter.filter( output );
        }

        return output;
    }

    public long[] subSampling()
    {
        for ( ImageFilter imageFilter : imageFilters )
        {
            if ( imageFilter instanceof ImageFilterSubSample )
            {
                return ( ( ImageFilterSubSample ) imageFilter ).subSampling;
            }
        }

        return null;
    }
}
