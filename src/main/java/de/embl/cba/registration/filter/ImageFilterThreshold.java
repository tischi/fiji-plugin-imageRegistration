package de.embl.cba.registration.filter;

import de.embl.cba.registration.utils.Duplicator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

import java.util.Map;

public class ImageFilterThreshold
        < R extends RealType< R > & NativeType < R > >
        implements ImageFilter< R, BitType > {

    double minValue;
    double maxValue;

    public ImageFilterThreshold( Map< String, Object > parameters )
    {
        minValue = (double) parameters.get(ImageFilterParameters.THRESHOLD_MIN_VALUE);
        maxValue = (double) parameters.get(ImageFilterParameters.THRESHOLD_MAX_VALUE);
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
