/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * FRAMA - Fractal Adaptive Moving Average (John F. Ehlers). Media movil cuyo factor
 * de suavizado se ajusta segun la dimension fractal calculada del propio precio:
 * mas rapida en mercados con estructura "lisa" (tendencial), mas lenta en mercados
 * fractalmente "rugosos" (rango/ruido). alpha = exp(-4.6*(D-1)), D = dimension fractal.
 * Fuente: https://www.prorealcode.com/prorealtime-indicators/ehlers-fractal-adaptive-moving-average-frama/
 *
 * Escrito siguiendo el patron real de los indicadores nativos (acceso historico
 * Chart.X.get(idx) para las sub-ventanas, como en ChoppinessIndex.java).
 */
package SQ.Blocks.Indicators.FRAMA;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(FRAMA) Fractal Adaptive Moving Average", display="FRAMA(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("FRAMA (Ehlers). Media movil adaptativa por dimension fractal del precio, alternativa a KAMA con base matematica distinta. Indicador no nativo agregado a SQX_Library.")
@ParameterSet(set="Period=16")
public class FRAMA extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=4, maxValue=200, defaultValue="16", step=2)
	public int Period;

	@Output(name = "FRAMA", color = Colors.Red)
	public DataSeries Value;

	private double prevFrama = 0;
	private boolean initialized = false;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		prevFrama = 0;
		initialized = false;
	}

	//------------------------------------------------------------------------

	private double highestLowest(int fromIdx, int toIdxExclusive, boolean high) {
		double result = high ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		for (int idx = fromIdx; idx < toIdxExclusive; idx++) {
			int safeIdx = Math.min(idx, getCurrentBar());
			double v = high ? Chart.High.get(safeIdx) : Chart.Low.get(safeIdx);
			if (high) { if (v > result) result = v; } else { if (v < result) result = v; }
		}
		return result;
	}

	@Override
	protected void OnBarUpdate() throws TradingException {
		int n = Period;
		if (n % 2 != 0) n = n + 1;
		int half = n / 2;

		double close0 = Chart.Close.get(0);

		if (getCurrentBar() < n) {
			Value.set(0, close0);
			prevFrama = close0;
			initialized = true;
			return;
		}

		double h1 = highestLowest(0, half, true);
		double l1 = highestLowest(0, half, false);
		double n1 = (h1 - l1) / half;

		double h2 = highestLowest(half, n, true);
		double l2 = highestLowest(half, n, false);
		double n2 = (h2 - l2) / half;

		double h3 = highestLowest(0, n, true);
		double l3 = highestLowest(0, n, false);
		double n3 = (h3 - l3) / n;

		double dimension = 1.0;
		if (n1 + n2 > 0.0000000001 && n3 > 0.0000000001) {
			dimension = (Math.log(n1 + n2) - Math.log(n3)) / Math.log(2.0);
		}
		if (dimension < 1) dimension = 1;
		if (dimension > 2) dimension = 2;

		double alpha = Math.exp(-4.6 * (dimension - 1));
		if (alpha < 0.01) alpha = 0.01;
		if (alpha > 1) alpha = 1;

		double frama = initialized ? (alpha * close0 + (1 - alpha) * prevFrama) : close0;

		Value.set(0, frama);
		prevFrama = frama;
		initialized = true;
	}
}
