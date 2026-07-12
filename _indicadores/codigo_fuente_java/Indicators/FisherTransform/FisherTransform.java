/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Fisher Transform (John F. Ehlers). Normaliza el precio (-1 a 1) y aplica
 * y = 0.5 * ln((1+x)/(1-x)) para acercarlo a una distribución gaussiana,
 * amplificando los extremos y produciendo picos/valles nítidos en los giros de precio.
 * Fuente: https://www.mesasoftware.com/papers/UsingTheFisherTransform.pdf
 *
 * Escrito siguiendo el patrón real de los indicadores nativos de esta instalación
 * (MACD/MACD.java para múltiples salidas, ChoppinessIndex/ChoppinessIndex.java para
 * el uso de HighestCalculator/LowestCalculator).
 */
package SQ.Blocks.Indicators.FisherTransform;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(FISH) Fisher Transform", display="FisherTransform(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Fisher Transform (Ehlers). Normaliza el precio a distribución casi gaussiana para detectar giros con mayor nitidez. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0, min=-3, max=3, step=0.01)
@ParameterSet(set="Period=10")
public class FisherTransform extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=200, defaultValue="10", step=1)
	public int Period;

	@Output(name = "Fisher", color = Colors.Red)
	public DataSeries Value;

	@Output(name = "Trigger", color = Colors.Blue)
	public DataSeries Trigger;

	private HighestCalculator highestCalc;
	private LowestCalculator lowestCalc;

	private double prevValue1 = 0;
	private double prevFisher = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		highestCalc = new HighestCalculator(Period);
		lowestCalc = new LowestCalculator(Period);
		prevValue1 = 0;
		prevFisher = 0;
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double median = (Chart.High.get(0) + Chart.Low.get(0)) / 2.0;

		highestCalc.onBarUpdate(median, getCurrentBar());
		lowestCalc.onBarUpdate(median, getCurrentBar());

		double highest = highestCalc.getHighestValue();
		double lowest = lowestCalc.getLowestValue();
		double range = highest - lowest;

		double value1;
		if (range < 0.0000000001) {
			value1 = 0;
		} else {
			value1 = 0.33 * 2.0 * ((median - lowest) / range - 0.5) + 0.67 * prevValue1;
		}

		if (value1 > 0.999) value1 = 0.999;
		if (value1 < -0.999) value1 = -0.999;

		double fisher = 0.5 * Math.log((1 + value1) / (1 - value1)) + 0.5 * prevFisher;

		Value.set(0, fisher);
		Trigger.set(0, prevFisher);

		prevValue1 = value1;
		prevFisher = fisher;
	}
}
