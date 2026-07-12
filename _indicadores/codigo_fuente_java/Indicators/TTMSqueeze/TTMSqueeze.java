/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * TTM Squeeze (John Carter). Compara Bandas de Bollinger(20,2) contra Keltner
 * Channel(20, ATR x1.5): si BB queda completamente DENTRO de Keltner, hay "squeeze"
 * (contracción de volatilidad, SqueezeOn=1). El componente de momentum (histograma)
 * es la regresión lineal de (Cierre - promedio(medio Donchian(20), SMA(20))).
 * Bollinger y Keltner ya son nativos por separado; esta COMPARACIÓN específica entre
 * ambos (no cada uno por sí solo) es lo que no existe como indicador propio.
 * Fuente: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/ttm-squeeze
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator,
 * StdDevCalculator, HighestCalculator/LowestCalculator, ATR calculado a mano como
 * en ChandelierExit.java, regresión lineal calculada a mano como en
 * HalfLifeMeanReversion.java).
 */
package SQ.Blocks.Indicators.TTMSqueeze;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.StdDevCalculator;
import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(SQZ) TTM Squeeze", display="TTMSqueeze(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("TTM Squeeze (Carter). SqueezeOn=1 cuando Bollinger esta dentro de Keltner (contraccion de volatilidad). Momentum = regresion lineal del desplazamiento del precio respecto a su punto medio. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="Period=20,BBDeviation=2,KCMultiplier=1.5")
public class TTMSqueeze extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=5, maxValue=100, defaultValue="20", step=1)
	public int Period;

	@Parameter(category="Default", name="BBDeviation", minValue=0.5, maxValue=5, defaultValue="2", step=0.1)
	public double BBDeviation;

	@Parameter(category="Default", name="KCMultiplier", minValue=0.5, maxValue=5, defaultValue="1.5", step=0.1)
	public double KCMultiplier;

	@Output(name = "SqueezeOn", color = Colors.Red)
	public DataSeries SqueezeOn;

	@Output(name = "Momentum", color = Colors.Blue)
	public DataSeries Momentum;

	private AverageCalculator smaCalc;
	private StdDevCalculator stdDevCalc;
	private AverageCalculator kcMidCalc;
	private AverageCalculator atrCalc;
	private HighestCalculator highestCalc;
	private LowestCalculator lowestCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		smaCalc = new AverageCalculator(AverageCalculator.SMA, Period);
		stdDevCalc = new StdDevCalculator(Period);
		kcMidCalc = new AverageCalculator(AverageCalculator.EMA, Period);
		atrCalc = new AverageCalculator(AverageCalculator.SMA, Period);
		highestCalc = new HighestCalculator(Period);
		lowestCalc = new LowestCalculator(Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);
		double h = Chart.High.get(0), l = Chart.Low.get(0);

		smaCalc.onBarUpdate(close, getCurrentBar());
		stdDevCalc.onBarUpdate(close, getCurrentBar());
		double bbMiddle = smaCalc.getValue();
		double bbUpper = bbMiddle + BBDeviation * stdDevCalc.getValue();
		double bbLower = bbMiddle - BBDeviation * stdDevCalc.getValue();

		double tr;
		if (getCurrentBar() == 0) {
			tr = h - l;
		} else {
			double prevClose = Chart.Close.get(1);
			tr = Math.max(h - l, Math.max(Math.abs(h - prevClose), Math.abs(l - prevClose)));
		}
		atrCalc.onBarUpdate(tr, getCurrentBar());
		kcMidCalc.onBarUpdate(close, getCurrentBar());
		double kcMiddle = kcMidCalc.getValue();
		double kcUpper = kcMiddle + KCMultiplier * atrCalc.getValue();
		double kcLower = kcMiddle - KCMultiplier * atrCalc.getValue();

		boolean squeezeOn = (bbUpper < kcUpper) && (bbLower > kcLower);
		SqueezeOn.set(0, squeezeOn ? 1 : 0);

		highestCalc.onBarUpdate(h, getCurrentBar());
		lowestCalc.onBarUpdate(l, getCurrentBar());
		double donchianMid = (highestCalc.getHighestValue() + lowestCalc.getLowestValue()) / 2.0;
		double reference = (donchianMid + bbMiddle) / 2.0;

		int window = Math.min(getCurrentBar() + 1, Period);
		double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
		for (int idx = 0; idx < window; idx++) {
			double c = Chart.Close.get(idx);
			double delta = c - reference;
			double x = window - 1 - idx;
			sumX += x; sumY += delta; sumXY += x * delta; sumXX += x * x;
		}
		double n = window;
		double meanX = sumX / n, meanY = sumY / n;
		double denom = sumXX - n * meanX * meanX;
		double slope = (Math.abs(denom) < 0.0000000001) ? 0 : (sumXY - n * meanX * meanY) / denom;
		double intercept = meanY - slope * meanX;

		double momentum = intercept + slope * (window - 1);

		Momentum.set(0, momentum);
	}
}
