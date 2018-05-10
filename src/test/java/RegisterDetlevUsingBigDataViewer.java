import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import de.embl.cba.registration.Axes;
import de.embl.cba.registration.util.IntervalUtils;
import ij.ImageJ;
import io.scif.img.ImgIOException;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.img.basictypeaccess.array.IntArray;

import java.util.List;
import java.util.Random;

public class RegisterDetlevUsingBigDataViewer < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{

    public RegisterDetlevUsingBigDataViewer() throws SpimDataException
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
        final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( xmlFilename );


        RandomAccessibleInterval< ? > image = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ).getImage( 0 );

        List< BdvStackSource< ? > > bdvStackSources = BdvFunctions.show( spimData );
        BdvStackSource bdvStackSource = bdvStackSources.get( 0 );
        List< SourceAndConverter > sourceAndConverters = bdvStackSource.getSources();
        SourceAndConverter< ? > sourceAndConverter = sourceAndConverters.get( 0 );
        RandomAccessibleInterval rai = sourceAndConverter.getSpimSource().getSource( 0 ,0 );

        AffineTransform3D elastixSimilarityTransform = new AffineTransform3D(  );

        elastixSimilarityTransform.set(
                0.8581196 ,  0.03852475,  0.00039899, 0.0,
                -0.03440228,  0.77021616, -0.37873092, 0.0,
                -0.01734354,  0.37833381,  0.77098398, 0.0 );


        AffineTransform3D transformJTransform = new AffineTransform3D(  );

        transformJTransform.set(
                -0.6427876097,   0.7660444431,   0.0,   0.0,
                0.0, 0.0, 1.0, 0.0,
                0.7660444431,  0.6427876097,  0.0, 0.0 );


        FinalInterval boundingIntervalAfterTransformation = Axes.boundingIntervalAfterTransformation( image, ( T ) transformJTransform );

        double[] transformJTranslation = new double[]{
                - boundingIntervalAfterTransformation.min( 0 ),
                - boundingIntervalAfterTransformation.min( 1 ) ,
                - boundingIntervalAfterTransformation.min( 2 ) };

        transformJTransform.translate( transformJTranslation );

        //AffineTransform3D combined = transformJTransform.preConcatenate( elastixSimilarityTransform );

        //combined.translate( new double[]{ 550/ 2.0, 0.0, 0.0 } );


        Bdv bdv2 = BdvFunctions.show( rai, "aaa", BdvOptions.options().sourceTransform( transformJTransform ) );


        ViewerPanel viewerPanel = bdv2.getBdvHandle().getViewerPanel();
        viewerPanel.setCurrentViewerTransform( new AffineTransform3D() );


        // RandomAccessibleIntervalSource(RandomAccessibleInterval<T> img, T type, AffineTransform3D sourceTransform, String name);
        // static <T> BdvStackSource<T> 	show(Source<T> source, BdvOptions options)
        // BdvOptions 	sourceTransform(AffineTransform3D t)

        /*
        ImagePlus imp = IJ.openImage( "/Users/tischer/Documents/detlev-arendt-clem-registration/data/prospr-aligned/C2-Mitf-aligned.tif");
        Img img = ImageJFunctions.wrap( imp );

        AffineTransform3D halfMicrometerScaling = new AffineTransform3D(  );

        halfMicrometerScaling.scale( 1.0 );

        BdvFunctions.show( img, "Mitf", Bdv.options().addTo( bdv2 ).sourceTransform( halfMicrometerScaling ) );

        ViewerPanel viewerPanel = bdv.getBdvHandle().getViewerPanel();

        viewerPanel.setCurrentViewerTransform( affineTransform3D );
        */


        // open mitf-aligned from tiff and add as a second source
        //

    }

    public static void main( String[] args ) throws ImgIOException, SpimDataException
    {
        // open an ImageJ window
        new ImageJ();

        // run the example
        new RegisterDetlevUsingBigDataViewer();
    }
}
