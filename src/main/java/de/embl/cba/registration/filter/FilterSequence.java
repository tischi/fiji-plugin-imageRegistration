package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.registration.filter.ImageFilterParameters.*;

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
            output = imageFilter.apply( output );
        }

        return output;
    }
}
