package de.embl.cba.registration.tests;

import ij.ImageJ;

import ij.ImagePlus;
import ij.io.Opener;
import io.scif.img.ImgIOException;

import java.io.File;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;


/**
 * Created by tischi on 27/09/17.
 */
public class LearnImglib2 {


    // within this method we define <T> to be a RealType and a NativeType which means the
    // Type is able to map the data into an java basic type array
    public < T extends NumericType< T > & NativeType< T > > LearnImglib2()
            throws ImgIOException
    {
        // define the file to open
        File file = new File( "/Users/tischi/Desktop/mri-stack-big.tif" );
        String path = file.getAbsolutePath();

        // open a file with ImageJ
        final ImagePlus imp = new Opener().openImage( file.getAbsolutePath() );

        // display it via ImageJ
        //imagePlus.show();

        // wrap it into an ImgLib image (no copying)
        final Img< T > img = ImageJFunctions.wrap( imp );
        //ImagePlusAdapter.wrap( imagePlus );

        // display it via ImgLib using ImageJ
        //ImageJFunctions.show( img );

        // get a cropped view
        RandomAccessibleInterval< T > view =
                Views.interval(img, new long[]{1, 1, 1}, new long[]{100, 100, 100});

        int n = img.numDimensions();

        // display it via ImgLib using ImageJ
        ImageJFunctions.show(view);

    }

    public static void main( String[] args ) throws ImgIOException
    {
        // open an ImageJ window
        new ImageJ();

        // run the example
        new LearnImglib2();
    }

}
