package de.embl.cba.registration.tests;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhaseCorrelationTest {


    public PhaseCorrelationTest()
    {
    }


    public static void main ( String... args )
    {

        ExecutorService service = Executors.newFixedThreadPool( 1 );
        final ImgFactory< FloatType > factory = new ArrayImgFactory< >();

        RandomAccessibleInterval< FloatType > im1 =
                factory.create( new long[]{100,100}, new FloatType(  ) );
        RandomAccessibleInterval< FloatType > im2 =
                factory.create( new long[]{100,100}, new FloatType(  ) );

        final int[] extension = new int[ im1.numDimensions() ];
        Arrays.fill( extension, 0 );


        /*
         Below offset leads to an error in the shiftPeak method
        */

        long offset = 10; // putting this to 0 => no error
        RandomAccessibleInterval im1withOffset = Views.translate( im1, new long[]{offset, offset } );
        RandomAccessibleInterval im2withOffset = Views.translate( im2, new long[]{offset, offset } );

        final RandomAccessibleInterval< FloatType > pcm =
                PhaseCorrelation2.calculatePCM(
                        im1withOffset,
                        im2withOffset,
                        extension,
                        new ArrayImgFactory< FloatType >(),
                        new FloatType(),
                        new ArrayImgFactory< ComplexFloatType >(),
                        new ComplexFloatType(),
                        service );

        final PhaseCorrelationPeak2 shiftPeak =
                PhaseCorrelation2.getShift(
                        pcm,
                        im1withOffset,
                        im2withOffset,
                        5,
                        100,
                        true,
                        true,
                        service );

    }


}
