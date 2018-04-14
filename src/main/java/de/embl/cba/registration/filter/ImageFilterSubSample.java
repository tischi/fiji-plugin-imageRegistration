package de.embl.cba.registration.filter;

import de.embl.cba.registration.util.Duplicator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ImageFilterSubSample< R extends RealType< R > & NativeType< R > > implements ImageFilter< R, R >
{
    public long[] subSampling;

    public ImageFilterSubSample( FilterSettings filterSettings )
    {
        this.subSampling = filterSettings.subSampling;
    }

    @Override
    public RandomAccessibleInterval< R > filter( RandomAccessibleInterval< R > source )
    {
        RandomAccessibleInterval< R > output = Views.subsample( source, subSampling );

        output = Duplicator.toArrayImg( output );

        return output;
    }


}
