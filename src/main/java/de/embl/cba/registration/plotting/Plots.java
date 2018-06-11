package de.embl.cba.registration.plotting;

import ij.gui.Plot;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Plots
{
	public static void plot( double[] xValues , double[] yValues )
	{
		Plot plot = new Plot("title","x", "y",  xValues, yValues );
		plot.show();
	}

}

