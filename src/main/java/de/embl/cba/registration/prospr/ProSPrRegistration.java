package de.embl.cba.registration.prospr;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;

public class ProSPrRegistration
{
    public static AffineTransform3D getTransformJRotation( SpimData emData )
    {

        AffineTransform3D transformJTransform = new AffineTransform3D(  );

        transformJTransform.set(
                -0.6427876097,   0.7660444431,   0.0,   0.0,
                0.0, 0.0, 1.0, 0.0,
                0.7660444431,  0.6427876097,  0.0, 0.0 );

        FinalRealInterval boundingIntervalAfterTransformation = getBoundsAfterRotation( emData, transformJTransform );

        double[] transformJTranslation = new double[]{
                - boundingIntervalAfterTransformation.realMin( 0 ),
                - boundingIntervalAfterTransformation.realMin( 1 ) ,
                - boundingIntervalAfterTransformation.realMin( 2 ) };

        transformJTransform.translate( transformJTranslation );

        return transformJTransform;
    }

    public static AffineTransform3D getCombinedTransform( AffineTransform3D firstTransform, AffineTransform3D secondTransform )
    {
        AffineTransform3D combinedTransform = firstTransform.copy();
        combinedTransform.preConcatenate( secondTransform );
        return combinedTransform;
    }

    public static FinalRealInterval getBoundsAfterRotation( SpimData emData, AffineTransform3D transformJTransform )
    {
        RandomAccessibleInterval< ? > image = emData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ).getImage( 0 );
        return transformJTransform.estimateBounds( image );
    }

    public static double[] getTranslationInPixels( SpimData data, double[] translationInMicrometer )
    {
        VoxelDimensions voxelDimensions = getVoxelDimensions( data );

        double[] translationInPixels = new double[3];

        for( int d = 0; d < 3; ++d )
        {
            translationInPixels[ d ] = translationInMicrometer[ d ] / voxelDimensions.dimension( d );
        }

        return translationInPixels;
    }

    public static VoxelDimensions getVoxelDimensions( SpimData data )
    {
        VoxelDimensions voxelDimensions = data.getSequenceDescription().getViewDescription( 0, 0 ).getViewSetup().getVoxelSize();
        return voxelDimensions;
    }

    public static AffineTransform3D setEmSimilarityTransform( SpimData spimData )
    {

        AffineTransform3D transformJRotation = getTransformJRotation( spimData );

        AffineTransform3D elastixSimilarityTransform = getEmElastixSimilarityTransform( spimData );

        AffineTransform3D combinedTransform = getCombinedTransform( transformJRotation, elastixSimilarityTransform );

        AffineTransform3D finalTransform = adaptViewRegistration( spimData, combinedTransform );

        return finalTransform;

    }

    public static AffineTransform3D adaptViewRegistration( SpimData emData, AffineTransform3D transform )
    {
        // the ViewRegistration in the file contains the scaling relative to 1 micrometer
        ViewRegistration viewRegistration = emData.getViewRegistrations().getViewRegistration( 0, 0 );
        ViewTransform viewTransform = new ViewTransformAffine( "align",  transform );
        viewRegistration.concatenateTransform( viewTransform );

        return viewRegistration.getModel();
    }

    public static AffineTransform3D getEmElastixSimilarityTransform( SpimData data )
    {
        AffineTransform3D elastixFixedToMoving = new AffineTransform3D();

        elastixFixedToMoving.set(
                1.16299445, -0.04662481, -0.02350539, 0.0,
                0.05221191,  1.04386046,  0.51274918, 0.0,
                0.00054074, -0.51328738,  1.04490107, 0.0 );

        double[] translationInMicrometer = new double[]{ 76.46363039, -78.88425459, 233.36234024 };

        double[] translationInPixels = getTranslationInPixels( data, translationInMicrometer );

        elastixFixedToMoving.translate( translationInPixels );

        return elastixFixedToMoving.inverse();
    }
}
