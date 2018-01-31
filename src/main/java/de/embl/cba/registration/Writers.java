package de.embl.cba.registration;

import de.embl.cba.registration.util.MetaImage;
import io.scif.img.ImgIOException;
import io.scif.img.ImgSaver;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.view.Views;

import java.io.File;

public class Writers
{
    public static void saveMetaImageUsingScifio( MetaImage metaImage, File file )
    {
        long start = Logger.start( "# Saving RAI image using SCIFIO to " + file.getAbsolutePath() );

        try
        {
            ImgSaver imgSaver = new ImgSaver( );
            Img img = ImgView.wrap( metaImage.rai, null );
            AxisType[] axisTypeArray = new AxisType[ metaImage.axisTypes.size() ];
            axisTypeArray = metaImage.axisTypes.toArray( axisTypeArray );
            ImgPlus imgPlus = new ImgPlus( img, "transformed",  axisTypeArray );
            imgSaver.saveImg( file.getAbsolutePath(),  imgPlus );

        }
        catch ( ImgIOException e )
        {
            e.printStackTrace();
        }
        catch ( IncompatibleTypeException e )
        {
            e.printStackTrace();
        }

        Logger.doneIn( start );

    }

}
