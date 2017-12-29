package de.embl.cba.registration.filter;

import de.embl.cba.registration.utils.Duplicator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.Map;

public class ImageFilterSubSample< R extends RealType< R > & NativeType< R > > implements ImageFilter< R, R >
{
    long[] subSampling;

    public ImageFilterSubSample( FilterSettings filterSettings )
    {
        this.subSampling = filterSettings.subSampling;
    }

    @Override
    public RandomAccessibleInterval< R > apply( RandomAccessibleInterval< R > source )
    {
        RandomAccessibleInterval< R > output = Views.subsample( source, subSampling );

        output = Duplicator.toArrayImg( output );

        return output;
    }


}
