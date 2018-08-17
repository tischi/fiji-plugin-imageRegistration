package de.embl.cba.registration.prospr;

import bdv.spimdata.SpimDataMinimal;
import de.embl.cba.registration.ui.ProSPrDataSource;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;

public class ProSPrRegistration
{
    public static AffineTransform3D getTransformJRotation( )
    {
        AffineTransform3D transformJTransform = new AffineTransform3D();

        transformJTransform.set(
                -0.6427876097,   0.7660444431,   0.0,   0.0,
                0.0, 0.0, 1.0, 0.0,
                0.7660444431,  0.6427876097,  0.0, 0.0 );

//        FinalRealInterval boundingIntervalAfterTransformation = getBoundsAfterRotation( source, transformJTransform );

//        double[] transformJTranslation = new double[]{
//                - boundingIntervalAfterTransformation.realMin( 0 ),
//                - boundingIntervalAfterTransformation.realMin( 1 ) ,
//                - boundingIntervalAfterTransformation.realMin( 2 ) };

        double[] transformJTranslationInMicrometer = new double[]{ 176.7, 0.0, 0.0 };

        transformJTransform.translate( transformJTranslationInMicrometer );

        return transformJTransform;
    }

    public static AffineTransform3D getCombinedTransform( AffineTransform3D firstTransform, AffineTransform3D secondTransform )
    {
        AffineTransform3D combinedTransform = firstTransform.copy();
        combinedTransform.preConcatenate( secondTransform );
        return combinedTransform;
    }

    public static FinalRealInterval getBoundsAfterRotation( ProSPrDataSource source, AffineTransform3D transformJTransform )
    {
        RandomAccessibleInterval< ? > image;

        if ( source.spimData != null )
        {
            image = source.spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ).getImage( 0 );
        }
        else
        {
            image = source.spimDataMinimal.getSequenceDescription().getImgLoader().getSetupImgLoader( 0  ).getImage( 0 );
        }

        return transformJTransform.estimateBounds( image );
    }

    public static double[] getTranslationInPixels( ProSPrDataSource source, double[] translationInMicrometer )
    {
        VoxelDimensions voxelDimensions = getVoxelDimensions( source );

        double[] translationInPixels = new double[3];

        for( int d = 0; d < 3; ++d )
        {
            translationInPixels[ d ] = translationInMicrometer[ d ] / voxelDimensions.dimension( d );
        }

        return translationInPixels;
    }

    public static VoxelDimensions getVoxelDimensions( ProSPrDataSource source )
    {

        if ( source.spimData != null )
        {
            return source.spimData.getSequenceDescription().getViewSetupsOrdered().get( 0 ).getVoxelSize();
        }
        else
        {
            return source.spimDataMinimal.getSequenceDescription().getViewSetupsOrdered().get( 0 ).getVoxelSize();
        }
    }

    public static VoxelDimensions getVoxelDimensions( SpimDataMinimal data )
    {
        VoxelDimensions voxelDimensions = data.getSequenceDescription().getViewSetupsOrdered().get( 0 ).getVoxelSize();
        return voxelDimensions;
    }


    public static AffineTransform3D setEmSimilarityTransform( ProSPrDataSource source )
    {
        AffineTransform3D emToProsprInMicrometerUnites = getTransformationFromEmToProsprInMicrometerUnites( );

        AffineTransform3D finalTransform = adaptViewRegistration( source, emToProsprInMicrometerUnites );

        return finalTransform;

    }

    public static AffineTransform3D getTransformationFromEmToProsprInMicrometerUnites( )
    {
        AffineTransform3D transformJRotation = getTransformJRotation( );

        AffineTransform3D elastixSimilarityTransform = getElastixSimilarityTransform( );

        return getCombinedTransform( transformJRotation, elastixSimilarityTransform );
    }

    public static AffineTransform3D adaptViewRegistration( ProSPrDataSource source, AffineTransform3D additionalTransform )
    {
        ViewRegistration viewRegistration;

        // the ViewRegistration in the file contains the scaling relative to 1 micrometer
        if ( source.spimData != null )
        {
            viewRegistration = source.spimData.getViewRegistrations().getViewRegistrationsOrdered( ).get( 0 );
        }
        else
        {
            viewRegistration = source.spimDataMinimal.getViewRegistrations().getViewRegistrationsOrdered( ).get( 0 );
        }

        /*
        The conventional meaning for concatenating transformations is the following:
        Let ba = b.concatenate(a).
        Applying ba to x is equivalent to first applying a to x and then applying b to the result.
         */

        final AffineTransform3D viewRegistrationAffineTransform = viewRegistration.getModel();

		viewRegistrationAffineTransform.preConcatenate( additionalTransform );

        return viewRegistrationAffineTransform;
    }

    public static AffineTransform3D getElastixSimilarityTransform( )
    {
        AffineTransform3D elastixFixedToMoving = new AffineTransform3D();

        elastixFixedToMoving.set(
                1.16299445, -0.04662481, -0.02350539, 0.0,
                0.05221191,  1.04386046,  0.51274918, 0.0,
                0.00054074, -0.51328738,  1.04490107, 0.0 );

        double[] translationInMicrometer = new double[]{ 76.46363039, -78.88425459, 233.36234024 };

//        double[] translationInPixels = getTranslationInPixels( source, translationInMicrometer );

        elastixFixedToMoving.translate( translationInMicrometer );

        return elastixFixedToMoving.inverse();
    }
}
