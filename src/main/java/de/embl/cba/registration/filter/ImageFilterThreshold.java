package de.embl.cba.registration.filter;

import de.embl.cba.registration.util.Duplicator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

public class ImageFilterThreshold
        < R extends RealType< R > & NativeType < R > >
        implements ImageFilter< R, BitType > {

    double minValue;
    double maxValue;

    public ImageFilterThreshold( FilterSettings settings )
    {
        minValue = settings.thresholdMin;
        maxValue = settings.thresholdMax;
    }

    @Override
    public RandomAccessibleInterval< BitType > apply( RandomAccessibleInterval< R > input )
    {

        RandomAccessibleInterval< BitType > converted =
                Converters.convert( input, ( s, t ) -> {
                    t.set( ( s.getRealDouble() >= minValue ) && ( s.getRealDouble() <= maxValue ) );
                }, new BitType() );

        RandomAccessibleInterval< BitType > output = Duplicator.toArrayImg( converted );

        return output;
    }





}
