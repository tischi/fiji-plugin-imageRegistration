package de.embl.cba.registration;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class ImageRegistrationUtils  {

    public static < R extends RealType< R > & NativeType< R > >
        RandomAccessible getRAIasTransformedRA( RandomAccessibleInterval rai,
                                                InvertibleRealTransform transform )
    {
        RealRandomAccessible rra
                = RealViews.transform(
                        Views.interpolate( Views.extendBorder( rai ), new NLinearInterpolatorFactory() ),
                                    transform );

        RandomAccessible transformedRA = Views.raster( rra );

        return transformedRA;
    }

    public static < R extends RealType< R > & NativeType< R > >
    RandomAccessible getRAasTransformedRA( RandomAccessible ra,
                                             InvertibleRealTransform transform )
    {
        RealRandomAccessible rra
                = RealViews.transform(
                        Views.interpolate( ra, new NLinearInterpolatorFactory() ),
                            transform );

        RandomAccessible transformedRA = Views.raster( rra );

        return transformedRA;
    }


}
