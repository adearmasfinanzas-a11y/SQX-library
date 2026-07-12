/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Bollinger %B. (Cierre - BandaInferior) / (BandaSuperior - BandaInferior). Mide
 * dónde está el precio DENTRO de las Bandas de Bollinger en una escala 0-1 (puede
 * exceder ese rango si el precio rompe las bandas) — distinto de operar con las
 * bandas como niveles de precio crudos.
 * Fuente: concepto estándar derivado de Bollinger Bands (John Bollinger)
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator SMA
 * + StdDevCalculator).
 */
package SQ.Blocks.Indicators.BollingerPercentB;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(%B) Bollinger Percent B", display="BollingerPercentB(@Chart@#Period#,#Deviation#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Bollinger %B. Posicion del cierre dentro de las Bandas de Bollinger en escala 0-1. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0.5, min=-0.5, max=1.5, step=0.01)
@ParameterSet(set="Period=20,Deviation=2")
public class BollingerPercentB extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=250, defaultValue="20", step=1)
	public int Period;

	@Parameter(category="Default", name="Deviation", minValue=0.5, maxValue=5, defaultValue="2", step=0.1)
	public double Deviation;

	@Output(name = "PercentB", color = Colors.Red)
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

		double middle = smaCalc.getValue();
		double stdDev = stdDevCalc.getValue();

		double upper = middle + Deviation * stdDev;
		double lower = middle - Deviation * stdDev;
		double range = upper - lower;

		double percentB = (range < 0.0000000001) ? 0.5 : (close - lower) / range;

		Value.set(0, percentB);
	}
}
