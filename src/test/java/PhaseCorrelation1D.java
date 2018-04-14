package de.embl.cba.registration.tests;

import de.embl.cba.registration.util.PhaseCorrelationUtils;
import de.embl.cba.registration.util.RandomAccessibleIntervalUtils;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2Util;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class PhaseCorrelation1D
{



    public static void main ( String... args )
    {
        ExecutorService service = Executors.newFixedThreadPool( 1 );
        final ImgFactory< FloatType > factory = new ArrayImgFactory< >();

        RandomAccessibleInterval< FloatType > im1 =
                factory.create( new long[]{50}, new FloatType(  ) );

        RandomAccessibleInterval< FloatType > im2 =
                factory.create( new long[]{50}, new FloatType(  ) );


        int i = 0;
        for ( final FloatType t : ( IterableInterval< FloatType > ) im1 )
        {
            ++i;
            if ( i > 20 && i < 25 ) t.set( 100 );
        }

        i = 0;
        for ( final FloatType t : ( IterableInterval< FloatType > ) im2 )
        {
            ++i;
            if ( i > 40 && i < 45  ) t.set( 100 );
        }


        final int[] extension = new int[ im1.numDimensions() ];
        Arrays.fill( extension, 0 );

        final RandomAccessibleInterval< FloatType > pcm = PhaseCorrelation2.calculatePCM( im1, im2, extension, new ArrayImgFactory<>(), new FloatType(), new ArrayImgFactory<>(), new ComplexFloatType(), service );

        PhaseCorrelationPeak2 peak = RandomAccessibleIntervalUtils.getMaximum( pcm );
        PhaseCorrelation2Util.expandPeakListToPossibleShifts( PhaseCorrelationUtils.asPeakList( peak ), pcm, im1, im2);

        // below function does not return:
        //List<PhaseCorrelationPeak2> peaks = PhaseCorrelation2Util.getPCMMaxima( pcm, Services.executorService, 2, true);



        //peaks = PhaseCorrelationUtils.sensiblePeaks( peaks, pcm );

        //Collections.sort(peaks, Collections.reverseOrder(new PhaseCorrelationUtils.ComparatorByPhaseCorrelation()));

        int a = 1;

    }

}
