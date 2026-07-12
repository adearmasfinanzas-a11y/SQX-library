/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Coppock Curve (Edwin Coppock). Media movil ponderada (WMA) de 10 periodos sobre
 * la suma de dos Rate-of-Change (14 y 11 periodos). Momentum de largo plazo,
 * diseñado originalmente para detectar suelos de mercado.
 * Fuente: ChartSchool (StockCharts) - ficha propia del indicador
 *
 * Escrito siguiendo el patron real de los indicadores nativos (AverageCalculator,
 * usando el tipo LWMA = Linear Weighted MA, equivalente a WMA).
 */
package SQ.Blocks.Indicators.CoppockCurve;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(COPP) Coppock Curve", display="CoppockCurve(@Chart@#RocLong#,#RocShort#,#WmaPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Coppock Curve. WMA de la suma de dos Rate-of-Change de distinto periodo. Momentum de largo plazo. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0, min=-30, max=30, step=0.1)
@ParameterSet(set="RocLong=14,RocShort=11,WmaPeriod=10")
public class CoppockCurve extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="RocLong", minValue=2, maxValue=200, defaultValue="14", step=1)
	public int RocLong;

	@Parameter(category="Default", name="RocShort", minValue=2, maxValue=200, defaultValue="11", step=1)
	public int RocShort;

	@Parameter(category="Default", name="WmaPeriod", minValue=2, maxValue=200, defaultValue="10", step=1)
	public int WmaPeriod;

	@Output(name = "Coppock", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator wmaCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		wmaCalc = new AverageCalculator(AverageCalculator.LWMA, WmaPeriod);
	}

	//------------------------------------------------------------------------

	private double rocAt(int period) {
		int back = Math.min(period, getCurrentBar());
		double past = Chart.Close.get(back);
		double now = Chart.Close.get(0);
		return (past != 0) ? 100.0 * (now - past) / past : 0;
	}

	@Override
	protected void OnBarUpdate() throws TradingException {
		double rocLongVal = rocAt(RocLong);
		double rocShortVal = rocAt(RocShort);

		double sumRoc = rocLongVal + rocShortVal;

		wmaCalc.onBarUpdate(sumRoc, getCurrentBar());
		Value.set(0, wmaCalc.getValue());
	}
}
