/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Mass Index (Donald Dorsey). Suma de 25 periodos del ratio entre una EMA(9) del
 * rango Alto-Bajo y una doble EMA(9) del mismo rango. Detecta "reversal bulges"
 * (expansiones de rango que preceden a un cambio de tendencia), sin importar direccion.
 * Fuente: https://www.barchart.com/education/technical-indicators/mass_index
 *
 * Escrito siguiendo el patron real de los indicadores nativos (AverageCalculator EMA
 * encadenado, SumCalculator para la suma final).
 */
package SQ.Blocks.Indicators.MassIndex;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.SumCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(MASSI) Mass Index", display="MassIndex(@Chart@#EmaPeriod#,#SumPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Mass Index (Dorsey). Detecta expansiones de rango que preceden a un cambio de tendencia. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=27, min=20, max=35, step=0.1)
@ParameterSet(set="EmaPeriod=9,SumPeriod=25")
public class MassIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="EmaPeriod", minValue=2, maxValue=100, defaultValue="9", step=1)
	public int EmaPeriod;

	@Parameter(category="Default", name="SumPeriod", minValue=2, maxValue=100, defaultValue="25", step=1)
	public int SumPeriod;

	@Output(name = "MassIndex", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator singleEMA;
	private AverageCalculator doubleEMA;
	private SumCalculator ratioSum;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		singleEMA = new AverageCalculator(AverageCalculator.EMA, EmaPeriod);
		doubleEMA = new AverageCalculator(AverageCalculator.EMA, EmaPeriod);
		ratioSum = new SumCalculator(SumPeriod);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double hlRange = Chart.High.get(0) - Chart.Low.get(0);

		singleEMA.onBarUpdate(hlRange, getCurrentBar());
		double single = singleEMA.getValue();

		doubleEMA.onBarUpdate(single, getCurrentBar());
		double doubleE = doubleEMA.getValue();

		double ratio = (Math.abs(doubleE) < 0.0000000001) ? 1 : single / doubleE;

		ratioSum.onBarUpdate(ratio, getCurrentBar());
		Value.set(0, ratioSum.getValue());
	}
}
