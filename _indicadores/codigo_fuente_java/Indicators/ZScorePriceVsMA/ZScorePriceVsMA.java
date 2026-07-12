/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Z-Score del precio respecto a su media móvil. (Cierre - SMA) / StdDev(Cierre).
 * Cuantifica cuántas desviaciones estándar se alejó el precio de su media reciente
 * — herramienta estadística estándar en literatura de stat-arb/mean-reversion, y
 * también útil como gatillo de "movimiento inusual" para hipótesis de ruptura
 * (un Z-Score extremo antes de una ruptura sugiere un movimiento más explosivo).
 * Fuente: concepto estadístico estándar (normalización Z-Score)
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator SMA
 * + StdDevCalculator).
 */
package SQ.Blocks.Indicators.ZScorePriceVsMA;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ZSCORE) Z-Score Precio vs Media", display="ZScorePriceVsMA(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Z-Score del precio respecto a su SMA, en desviaciones estandar. Herramienta estadistica estandar para mean-reversion y para detectar movimientos inusuales. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0, min=-4, max=4, step=0.1)
@ParameterSet(set="Period=20")
public class ZScorePriceVsMA extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=250, defaultValue="20", step=1)
	public int Period;

	@Output(name = "ZScore", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator smaCalc;
	private StdDevCalculator stdDevCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		smaCalc = new AverageCalculator(AverageCalculator.SMA, Period);
		stdDevCalc = new StdDevCalculator(Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);

		smaCalc.onBarUpdate(close, getCurrentBar());
		stdDevCalc.onBarUpdate(close, getCurrentBar());

		double sma = smaCalc.getValue();
		double stdDev = stdDevCalc.getValue();

		double z = (stdDev < 0.0000000001) ? 0 : (close - sma) / stdDev;
		Value.set(0, z);
	}
}
