package de.embl.cba.registration.utils;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

public class ViewsUtils
{
    public static RandomAccessibleInterval insertDimension( RandomAccessibleInterval rai, int insertDim )
    {
        RandomAccessibleInterval raiWithInsertedDimension = Views.addDimension( rai, 0,0  );

        for ( int d = raiWithInsertedDimension.numDimensions() - 1; d > insertDim; --d )
        {
            raiWithInsertedDimension = Views.permute( raiWithInsertedDimension, d, d - 1  );
        }

        return raiWithInsertedDimension;
    }
}
