package de.embl.cba.registration.tests;

import de.embl.cba.registration.util.Projection;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;


public class ProjectionTest
{
    public static void main( String... args )
    {
        final ImgFactory< FloatType > factory = new ArrayImgFactory<>();
        RandomAccessibleInterval< FloatType > input = factory.create( new long[]{ 10, 10, 2 }, new FloatType() );

        for ( FloatType floatType : Views.iterable( input ) )
        {
            floatType.set( ( float ) 10.0 );
        }

        Projection projection = new Projection( input, 2 );

        RandomAccessibleInterval< FloatType > output = projection.average();

        assert output.numDimensions() == input.numDimensions() - 1;

        int a = 1;

    }

}
