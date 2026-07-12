/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Twiggs Money Flow (Colin Twiggs, refinamiento de Chaikin Money Flow). EMA del
 * volumen ponderado por la posicion del cierre dentro del Rango Verdadero (True
 * Range, no solo High-Low de la barra) — evita la distorsion por gaps de apertura
 * que afecta a Chaikin Money Flow.
 * Fuente: https://www.incrediblecharts.com/indicators/twiggs_money_flow.php
 *
 * Escrito siguiendo el patron real de los indicadores nativos (AverageCalculator EMA,
 * acceso a Chart.Volume.get(idx) como en AvgVolume.java).
 */
package SQ.Blocks.Indicators.TwiggsMoneyFlow;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(TMF) Twiggs Money Flow", display="TwiggsMoneyFlow(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Twiggs Money Flow (Twiggs). Refinamiento de Chaikin Money Flow usando True Range para evitar distorsion por gaps. Indicador no nativo agregado a SQX_Library. Requiere volumen real o tick volume como proxy.")
@Indicator(oscillator=true, middleValue=0, min=-1, max=1, step=0.01)
@ParameterSet(set="Period=21")
public class TwiggsMoneyFlow extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=200, defaultValue="21", step=1)
	public int Period;

	@Output(name = "TMF", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator numEMA;
	private AverageCalculator volEMA;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		numEMA = new AverageCalculator(AverageCalculator.EMA, Period);
		volEMA = new AverageCalculator(AverageCalculator.EMA, Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double h = Chart.High.get(0);
		double l = Chart.Low.get(0);
		double c = Chart.Close.get(0);
		double v = Chart.Volume.get(0);
		double prevClose = (getCurrentBar() > 0) ? Chart.Close.get(1) : c;

		double trueHigh = Math.max(h, prevClose);
		double trueLow = Math.min(l, prevClose);
		double trueRange = trueHigh - trueLow;

		double adRatio = (trueRange < 0.0000000001) ? 0 : (2 * c - trueHigh - trueLow) / trueRange;

		numEMA.onBarUpdate(v * adRatio, getCurrentBar());
		volEMA.onBarUpdate(v, getCurrentBar());

		double volSmoothed = volEMA.getValue();
		double tmf = (Math.abs(volSmoothed) < 0.0000000001) ? 0 : numEMA.getValue() / volSmoothed;

		Value.set(0, tmf);
	}
}
