/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.chartsy.willr;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import org.chartsy.main.ChartFrame;
import org.chartsy.main.chart.Indicator;
import org.chartsy.main.data.DataItem;
import org.chartsy.main.data.Dataset;
import org.chartsy.main.utils.DefaultPainter;
import org.chartsy.main.utils.Range;
import org.chartsy.main.utils.SerialVersion;
import org.chartsy.talib.TaLibInit;
import org.chartsy.talib.TaLibUtilities;
import org.openide.nodes.AbstractNode;

/**
 * The Williams %R indicator
 *
 * @author joshua.taylor
 */
public class WillR extends Indicator
{

    private static final long serialVersionUID = SerialVersion.APPVERSION;

    public static final String FULL_NAME = "Williams %R";
    public static final String HASHKEY = "willr";
    private IndicatorProperties properties;

    //variables for TA-Lib utilization
    private int lookback;
    private double[] output;
    private transient MInteger outBegIdx;
    private transient MInteger outNbElement;
    private transient Core core;

    private int period = 0;
    private double[] allHighs;
    private double[] allLows;

    //the next variable is used for fast calculations
    private Dataset calculatedDataset;

    public WillR()
    {
        super();
        properties = new IndicatorProperties();
    }

    @Override
    public String getName(){ return FULL_NAME; }

    @Override
    public String getLabel()
    { return properties.getLabel() + " (" + properties.getPeriod() + ")"; }

    @Override
    public String getPaintedLabel(ChartFrame cf){ return getLabel(); }

    @Override
    public Indicator newInstance(){ return new WillR(); }

    @Override
    public LinkedHashMap getHTML(ChartFrame cf, int i)
    {
        LinkedHashMap ht = new LinkedHashMap();

        DecimalFormat df = new DecimalFormat("#,##0.00");
        double[] values = getValues(cf, i);
        String[] labels = {"Williams %R:"};

        ht.put(getLabel(), " ");
        if (values.length > 0) {
            Color[] colors = getColors();
            for (int j = 0; j < values.length; j++) {
                ht.put(getFontHTML(colors[j], labels[j]),
                        getFontHTML(colors[j], df.format(values[j])));
            }
        }

        return ht;
    }

    @Override
    public Range getRange(ChartFrame cf){ return new Range(0, -100); }

    @Override
    public void paint(Graphics2D g, ChartFrame cf, Rectangle bounds)
    {
        Dataset d = visibleDataset(cf, HASHKEY);

        if (d != null)
        {
            if (maximized)
            {
                Range range = getRange(cf);

                DefaultPainter.line(g, cf, range, bounds, d, properties.getColor(), properties.getStroke()); // paint line
                if (properties.getInsideVisibility())
                {
                    paintFill(g, cf, d, bounds, range, properties.getInsideTransparentColor(), -20d, 0d, true); // paint fill
                    paintFill(g, cf, d, bounds, range, properties.getInsideTransparentColor(), -80d, -100d, false); // paint fill
                }
            }
        }
    }

    @Override
    public boolean hasZeroLine()
    { return false; }

    @Override
    public boolean getZeroLineVisibility()
    { return false; }

    @Override
    public Color getZeroLineColor()
    { return null; }

    @Override
    public Stroke getZeroLineStroke()
    { return null; }

    @Override
    public boolean hasDelimiters()
    { return true; }

    @Override
    public boolean getDelimitersVisibility()
    { return true; }

    @Override
    public double[] getDelimitersValues()
    { return new double[] {-20d, -50d, -80d}; }

    @Override
    public Color getDelimitersColor()
    { return properties.getDelimiterColor(); }

    @Override
    public Stroke getDelimitersStroke()
    { return properties.getDelimiterLineStroke(); }

    @Override
    public Color[] getColors()
    { return new Color[] {properties.getColor()}; }

    @Override
    public double[] getValues(ChartFrame cf)
    {
        Dataset d = visibleDataset(cf, HASHKEY);
        if (d != null)
            return new double[] {d.getLastClose()};
        return new double[] {};
    }

    @Override
    public double[] getValues(ChartFrame cf, int i)
    {
        Dataset d = visibleDataset(cf, HASHKEY);
        if (d != null)
            return new double[] {d.getCloseAt(i)};
        return new double[] {};
    }

    @Override
    public boolean getMarkerVisibility()
    { return properties.getMarker(); }

    @Override
    public AbstractNode getNode() { return new IndicatorNode(properties); }

    @Override
    public Double[] getPriceValues(ChartFrame cf)
    { return new Double[] {new Double(-20), new Double(-50), new Double(-80)}; }

    @Override
    public void calculate()
    {
        Dataset initial = getDataset();
        int count = 0;
        if (initial != null && !initial.isEmpty())
            count = initial.getItemsCount();

        /**********************************************************************/
        //This entire method is basically a copy/paste action into your own
        //code. The only thing you have to change is the next few lines of code.
        //Choose the 'lookback' method and appropriate 'calculation function'
        //from TA-Lib for your needs. You'll also need to ensure you gather
        //everything for your calculation as well. Everything else should stay
        //basically the same

        //prepare ta-lib variables
        output = new double[count];
        outBegIdx = new MInteger();
        outNbElement = new MInteger();
        core = TaLibInit.getCore();//needs to be here for serialization issues

        //[your specific indicator variables need to be set first]
        period = properties.getPeriod();
        allHighs = initial.getHighValues();
        allLows = initial.getLowValues();

        //now do the calculation over the entire dataset
        //[First, perform the lookback call if one exists]
        //[Second, do the calculation call from TA-lib]
        lookback = core.willRLookback(period);
        core.willR(0, count-1, allHighs, allLows, initial.getCloseValues(), period, outBegIdx, outNbElement, output);
        //Everything between the /***/ lines is what needs to be changed.
        //Everything else remains the same. You are done with your part now.
        /**********************************************************************/

        //fix the output array's structure. TA-Lib does NOT match
        //indicator index and dataset index automatically. That's what
        //this function does for us.
        output = TaLibUtilities.fixOutputArray(output, lookback);

        calculatedDataset = Dataset.EMPTY(initial.getItemsCount());
        for (int i = 0; i < output.length; i++)
            calculatedDataset.setDataItem(i, new DataItem(initial.getTimeAt(i), output[i]));

        addDataset(HASHKEY, calculatedDataset);
    }

}
