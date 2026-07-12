/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Relative Vigor Index (Donald Dorsey, 1995). Mide si el "vigor" del movimiento
 * (dónde cierra la vela relativo a dónde abre, sobre su propio rango) sostiene
 * la dirección visible del precio. Promedio ponderado de 4 barras sobre (Close-Open)
 * y (High-Low), suavizado adicionalmente con una media móvil.
 * Fuente: https://www.fidelity.com/learning-center/trading-investing/technical-analysis/technical-indicator-guide/relative-vigor-index
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator para
 * el suavizado final, acceso histórico Chart.X.get(idx) como en ChoppinessIndex.java).
 */
package SQ.Blocks.Indicators.RelativeVigorIndex;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(RVI) Relative Vigor Index", display="RelativeVigorIndex(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Relative Vigor Index (Dorsey). Mide si el cierre respecto a la apertura, sobre el rango de la vela, sostiene la tendencia. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0, min=-1, max=1, step=0.01)
@ParameterSet(set="Period=10")
public class RelativeVigorIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=200, defaultValue="10", step=1)
	public int Period;

	@Output(name = "RVI", color = Colors.Red)
	public DataSeries Value;

	@Output(name = "Signal", color = Colors.Blue)
	public DataSeries Signal;

	private AverageCalculator numAvg;
	private AverageCalculator denomAvg;

	private double prevRVI1 = 0, prevRVI2 = 0, prevRVI3 = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		numAvg = new AverageCalculator(AverageCalculator.SMA, Period);
		denomAvg = new AverageCalculator(AverageCalculator.SMA, Period);
		prevRVI1 = 0; prevRVI2 = 0; prevRVI3 = 0;
	}

	//------------------------------------------------------------------------

	private double bar(DataSeries series, int idx) throws TradingException {
		return (idx <= getCurrentBar()) ? series.get(idx) : series.get(getCurrentBar());
	}

	@Override
	protected void OnBarUpdate() throws TradingException {
		double c0 = Chart.Close.get(0), o0 = Chart.Open.get(0), h0 = Chart.High.get(0), l0 = Chart.Low.get(0);
		double c1 = bar(Chart.Close, 1), o1 = bar(Chart.Open, 1), h1 = bar(Chart.High, 1), l1 = bar(Chart.Low, 1);
		double c2 = bar(Chart.Close, 2), o2 = bar(Chart.Open, 2), h2 = bar(Chart.High, 2), l2 = bar(Chart.Low, 2);
		double c3 = bar(Chart.Close, 3), o3 = bar(Chart.Open, 3), h3 = bar(Chart.High, 3), l3 = bar(Chart.Low, 3);

		double num = ((c0 - o0) + 2 * (c1 - o1) + 2 * (c2 - o2) + (c3 - o3)) / 6.0;
		double denom = ((h0 - l0) + 2 * (h1 - l1) + 2 * (h2 - l2) + (h3 - l3)) / 6.0;

		numAvg.onBarUpdate(num, getCurrentBar());
		denomAvg.onBarUpdate(denom, getCurrentBar());

		double denomSmoothed = denomAvg.getValue();
		double rvi = (Math.abs(denomSmoothed) < 0.0000000001) ? 0 : numAvg.getValue() / denomSmoothed;

		double signal = (rvi + 2 * prevRVI1 + 2 * prevRVI2 + prevRVI3) / 6.0;

		Value.set(0, rvi);
		Signal.set(0, signal);

		prevRVI3 = prevRVI2;
		prevRVI2 = prevRVI1;
		prevRVI1 = rvi;
	}
}
