/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Chandelier Exit (Chuck LeBeau). No es una señal de entrada, es un nivel de salida
 * "colgado" del extremo reciente: Long = Highest(N) - ATR(N)*mult ; Short = Lowest(N) + ATR(N)*mult.
 * Distinto de un SL clasico anclado al precio de entrada.
 * Fuente: https://www.barchart.com/education/technical-indicators/chandelier-exit
 *
 * Escrito siguiendo el patron real de los indicadores nativos (HighestCalculator/
 * LowestCalculator como en ChoppinessIndex.java; True Range calculado a mano como
 * tampoco existe un ATRCalculator nativo entre los Calculators disponibles).
 */
package SQ.Blocks.Indicators.ChandelierExit;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(CHEXIT) Chandelier Exit", display="ChandelierExit(@Chart@#Period#,#Multiplier#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("Chandelier Exit (LeBeau). Nivel de stop colgado del extremo reciente de N barras, ajustado por ATR. Indicador no nativo agregado a SQX_Library.")
@ParameterSet(set="Period=22,Multiplier=3")
public class ChandelierExit extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=200, defaultValue="22", step=1)
	public int Period;

	@Parameter(category="Default", name="Multiplier", minValue=0.5, maxValue=10, defaultValue="3", step=0.1)
	public double Multiplier;

	@Output(name = "LongStop", color = Colors.Green)
	public DataSeries LongStop;

	@Output(name = "ShortStop", color = Colors.Red)
	public DataSeries ShortStop;

	private HighestCalculator highestCalc;
	private LowestCalculator lowestCalc;
	private AverageCalculator atrCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		highestCalc = new HighestCalculator(Period);
		lowestCalc = new LowestCalculator(Period);
		atrCalc = new AverageCalculator(AverageCalculator.SMA, Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double h = Chart.High.get(0);
		double l = Chart.Low.get(0);

		double tr;
		if (getCurrentBar() == 0) {
			tr = h - l;
		} else {
			double prevClose = Chart.Close.get(1);
			tr = Math.max(h - l, Math.max(Math.abs(h - prevClose), Math.abs(l - prevClose)));
		}

		atrCalc.onBarUpdate(tr, getCurrentBar());
		highestCalc.onBarUpdate(h, getCurrentBar());
		lowestCalc.onBarUpdate(l, getCurrentBar());

		double atr = atrCalc.getValue();
		double longStop = highestCalc.getHighestValue() - atr * Multiplier;
		double shortStop = lowestCalc.getLowestValue() + atr * Multiplier;

		LongStop.set(0, longStop);
		ShortStop.set(0, shortStop);
	}
}
