package de.embl.cba.registration.tests;

import de.embl.cba.registration.util.Projection;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.RealSum;
import net.imglib2.view.Views;

public class ProjectionTest
{


    public static void main( String... args )
    {
        final ImgFactory< FloatType > factorySource = new ArrayImgFactory<>();
        RandomAccessibleInterval< FloatType > source = factorySource.create( new long[]{ 10, 10, 2 }, new FloatType() );

        Projection projection = new Projection( source, 2 );

        RandomAccessibleInterval< FloatType > output = projection.sum();

    }
    
}
