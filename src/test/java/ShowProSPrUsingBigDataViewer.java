import bdv.util.*;
import de.embl.cba.registration.Axes;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import io.scif.img.ImgIOException;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.img.basictypeaccess.array.IntArray;


import java.util.Random;

public class ShowProSPrUsingBigDataViewer< T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{

    public ShowProSPrUsingBigDataViewer() throws SpimDataException
    {
        test2();
    }

    public void test()
    {

        final Random random = new Random();

        final ArrayImg< ARGBType, IntArray > img = ArrayImgs.argbs( 100, 100, 100 );
        img.forEach( t -> t.set( random.nextInt() & 0xFF00FF00 ) );
        final Bdv bdv3D = BdvFunctions.show( img, "greens" );

        final ArrayImg< ARGBType, IntArray > img2 = ArrayImgs.argbs( 100, 100, 100 );
        img2.forEach( t -> t.set( random.nextInt() & 0xFFFF0000 ) );
        BdvFunctions.show( img2, "reds", Bdv.options().addTo( bdv3D ) );


    }


    public void test2() throws SpimDataException
    {

        // open em-raw 500nm from Bdv and show in Bdv
        //
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );

        final String xmlFilename = "/Users/tischer/Documents/detlev-arendt-clem-registration/data/em-raw-500nm.xml";
        final SpimData spimData = new XmlIoSpimData().load( xmlFilename );

        RandomAccessibleInterval< ? > image = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ).getImage( 0 );

        /*
        List< BdvStackSource< ? > > bdvStackSources = BdvFunctions.show( spimData );
        BdvStackSource bdvStackSource = bdvStackSources.get( 0 );
        List< SourceAndConverter > sourceAndConverters = bdvStackSource.getSources();
        SourceAndConverter< ? > sourceAndConverter = sourceAndConverters.get( 0 );
        RandomAccessibleInterval rai = sourceAndConverter.getSpimSource().getSource( 0 ,0 );
        */


        AffineTransform3D transformJTransform = new AffineTransform3D(  );

        transformJTransform.set(
                -0.6427876097,   0.7660444431,   0.0,   0.0,
                0.0, 0.0, 1.0, 0.0,
                0.7660444431,  0.6427876097,  0.0, 0.0 );

        FinalInterval boundingIntervalAfterTransformation = Axes.boundingIntervalAfterTransformation( image, ( T ) transformJTransform );

        FinalRealInterval boundingIntervalAfterTransformation2 = transformJTransform.estimateBounds( image );

        double[] transformJTranslation = new double[]{
                - boundingIntervalAfterTransformation.min( 0 ),
                - boundingIntervalAfterTransformation.min( 1 ) ,
                - boundingIntervalAfterTransformation.min( 2 ) };

        transformJTransform.translate( transformJTranslation );


        AffineTransform3D elastixFixedToMoving = new AffineTransform3D();
        elastixFixedToMoving.set(
                1.16299445, -0.04662481, -0.02350539, 0.0,
                0.05221191,  1.04386046,  0.51274918, 0.0,
                0.00054074, -0.51328738,  1.04490107, 0.0 );

        elastixFixedToMoving.translate( new double[]{ 152.92726078, -157.76850918,  466.72468048 } );


        AffineTransform3D combined = transformJTransform.copy();

        combined.preConcatenate( elastixFixedToMoving.inverse() );

        ViewRegistration viewRegistration = spimData.getViewRegistrations().getViewRegistration( 0, 0 );
        viewRegistration.identity();
        ViewTransform viewTransform = new ViewTransformAffine( "transform",  combined );
        viewRegistration.preconcatenateTransform( viewTransform );

        Bdv bdv = BdvFunctions.show( spimData ).get( 0 );
        bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );

        //Bdv bdv3 = BdvFunctions.show( rai, "aaa", BdvOptions.options().sourceTransform( combined ) );
        //bdv3.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );

        // RandomAccessibleIntervalSource(RandomAccessibleInterval<T> img, T type, AffineTransform3D sourceTransform, String name);
        // static <T> BdvStackSource<T> 	show(Source<T> source, BdvOptions options)
        // BdvOptions 	sourceTransform(AffineTransform3D t)


        ImagePlus imp = IJ.openImage( "/Users/tischer/Documents/detlev-arendt-clem-registration/data/prospr-aligned/C2-Mitf-aligned.tif");
        Img img = ImageJFunctions.wrap( imp );

        AffineTransform3D halfMicrometerScaling = new AffineTransform3D(  );
        halfMicrometerScaling.scale( 1.0 );
        final BdvSource source = BdvFunctions.show( img, "Mitf", Bdv.options().addTo( bdv ).sourceTransform( halfMicrometerScaling ) );
        ARGBType magenta = new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) );
        source.setColor( magenta );

        // open mitf-aligned from tiff and add as a second source
        //

    }

    public static void main( String[] args ) throws ImgIOException, SpimDataException
    {
        // open an ImageJ window
        new ImageJ();

        // run the example
        new ShowProSPrUsingBigDataViewer();
    }
}
