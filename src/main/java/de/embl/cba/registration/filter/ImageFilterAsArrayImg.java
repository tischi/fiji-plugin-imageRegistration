package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class ImageFilterAsArrayImg  < R extends RealType< R > & NativeType < R > >
        implements ImageFilter< R, R >
{

    public ImageFilterAsArrayImg( FilterSettings settings )
    { }

    @Override
    public RandomAccessibleInterval< R > filter( RandomAccessibleInterval < R > input )
    {
        ImgFactory< R > imgFactory = new ArrayImgFactory< R >();

        Img< R > output = imgFactory.create( input, Views.iterable( input ).firstElement() );

        for ( Pair< R, R > p : Views.interval( Views.pair( Views.zeroMin( input ), output ), output ) )
            p.getB().set( p.getA() );

        return Views.translate( output, Intervals.minAsLongArray( input ) );
    }

}
