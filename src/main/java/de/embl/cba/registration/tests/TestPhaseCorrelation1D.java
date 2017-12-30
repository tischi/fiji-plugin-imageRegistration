package de.embl.cba.registration.tests;

import de.embl.cba.registration.Services;
import de.embl.cba.registration.transformfinder.PhaseCorrelationUtils;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.binary.Thresholder;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2Util;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class TestPhaseCorrelation1D
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
            if ( i > 20 ) t.set( 100 );
        }

        i = 0;
        for ( final FloatType t : ( IterableInterval< FloatType > ) im2 )
        {
            ++i;
            if ( i > 30 ) t.set( 100 );
        }


        final int[] extension = new int[ im1.numDimensions() ];
        Arrays.fill( extension, 10 );

        final RandomAccessibleInterval< FloatType > pcm = PhaseCorrelation2.calculatePCM( im1, im2, extension, new ArrayImgFactory<>(), new FloatType(), new ArrayImgFactory<>(), new ComplexFloatType(), service );

        // below function does not return:
        List<PhaseCorrelationPeak2> peaks = PhaseCorrelation2Util.getPCMMaxima( pcm, Services.executorService, 2, true);

        PhaseCorrelation2Util.expandPeakListToPossibleShifts(peaks, pcm, im1, im2);

        peaks = PhaseCorrelationUtils.sensiblePeaks( peaks, pcm );

        Collections.sort(peaks, Collections.reverseOrder(new PhaseCorrelationUtils.ComparatorByPhaseCorrelation()));

        int a = 1;


    }
}
