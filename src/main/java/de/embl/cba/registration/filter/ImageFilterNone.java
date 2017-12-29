package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ImageFilterNone< R extends RealType< R > & NativeType< R > >
        implements ImageFilter< R, R > {

    public ImageFilterNone( FilterSettings settings )
    {
    }

    @Override
    public RandomAccessibleInterval< R > apply( RandomAccessibleInterval< R > source )
    {
        return source;
    }
}
