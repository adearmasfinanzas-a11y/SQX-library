/*
 * Indicador no nativo, propuesto y aprobado para SQX_Library el 2026-07-07.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Choppiness Index (E.W. Dreiss). Mide si el mercado se mueve "eficientemente" dentro de su
 * propio rango: valores cercanos a 100 = mercado en rango (choppy); cercanos a 0 = tendencial.
 * Formula: 100 * LOG10( SUMA(TrueRange, n) / (Maximo(High, n) - Minimo(Low, n)) ) / LOG10(n)
 *
 * Escrito siguiendo el patrón real de los indicadores nativos de esta instalación
 * (SQ/Blocks/Indicators/ATR/ATR.java, CCI/CCI.java, HighestLowest/Highest.java).
 */
package SQ.Blocks.Indicators.ChoppinessIndex;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(CHOP) Choppiness Index", display="ChoppinessIndex(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Choppiness Index (E.W. Dreiss). 100 = mercado en rango/choppy, 0 = mercado tendencial. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=1)
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
public class ChoppinessIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=200, defaultValue="14", step=1)
	public int Period;

	@Output(name = "CHOP", color = Colors.Red)
	public DataSeries Value;

	private HighestCalculator highestHigh;
	private LowestCalculator lowestLow;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		highestHigh = new HighestCalculator(Period);
		lowestLow = new LowestCalculator(Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		highestHigh.onBarUpdate(Chart.High.get(0), getCurrentBar());
		lowestLow.onBarUpdate(Chart.Low.get(0), getCurrentBar());

		if (getCurrentBar() == 0) {
			Value.set(0, 0);
			return;
		}

		int window = Math.min(CurrentBar + 1, Period);

		double sumTR = 0;
		for (int idx = 0; idx < window; idx++) {
			double h = Chart.High.get(idx);
			double l = Chart.Low.get(idx);
			double tr;

			if (getCurrentBar() - idx == 0) {
				tr = h - l;
			} else {
				double prevClose = Chart.Close.get(idx + 1);
				tr = Math.max(h - l, Math.max(Math.abs(h - prevClose), Math.abs(l - prevClose)));
			}
			sumTR += tr;
		}

		double range = highestHigh.getHighestValue() - lowestLow.getLowestValue();

		if (range < 0.0000000001 || window < 2) {
			Value.set(0, 0);
		} else {
			double chop = 100.0 * Math.log10(sumTR / range) / Math.log10(window);
			Value.set(0, chop);
		}
	}
}
