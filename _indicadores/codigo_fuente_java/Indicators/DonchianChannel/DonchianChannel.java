/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda,
 * orientada a otras familias de hipótesis: ruptura/tendencia, no solo reversión).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Donchian Channel (Richard Donchian, base del sistema Turtle Trading). Superior =
 * máximo de N barras; Inferior = mínimo de N barras; Media = promedio de ambos.
 * Fuente: https://admiralmarkets.com/education/articles/forex-indicators/what-everyone-should-know-about-the-donchian-channel-indicator
 *
 * Nota de no-redundancia: Highest/Lowest ya existen como Price Levels para SL/PT en
 * esta instalación, pero no están expuestos como Indicador con salidas Superior/Media/
 * Inferior utilizable directamente en condiciones de ENTRADA (comparadores tipo
 * "precio cruza por encima del canal") — es esa exposición como indicador de entrada
 * lo que se propone acá, no el cálculo de máximo/mínimo en sí (que sí es nativo).
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (HighestCalculator/
 * LowestCalculator, como en ChoppinessIndex.java).
 */
package SQ.Blocks.Indicators.DonchianChannel;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(DONCH) Donchian Channel", display="DonchianChannel(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("Donchian Channel (base del sistema Turtle Trading). Canal de maximo/minimo de N barras, usable directamente en condiciones de entrada. Indicador no nativo agregado a SQX_Library.")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=55")
public class DonchianChannel extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=250, defaultValue="20", step=1)
	public int Period;

	@Output(name = "Upper", color = Colors.Red)
	public DataSeries Upper;

	@Output(name = "Middle", color = Colors.Blue)
	public DataSeries Middle;

	@Output(name = "Lower", color = Colors.Green)
	public DataSeries Lower;

	private HighestCalculator highestCalc;
	private LowestCalculator lowestCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		highestCalc = new HighestCalculator(Period);
		lowestCalc = new LowestCalculator(Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		highestCalc.onBarUpdate(Chart.High.get(0), getCurrentBar());
		lowestCalc.onBarUpdate(Chart.Low.get(0), getCurrentBar());

		double upper = highestCalc.getHighestValue();
		double lower = lowestCalc.getLowestValue();

		Upper.set(0, upper);
		Lower.set(0, lower);
		Middle.set(0, (upper + lower) / 2.0);
	}
}
