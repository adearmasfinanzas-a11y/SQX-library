/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * VWAP con bandas de desviación estándar. VWAP nativo existe (referencia de sesión),
 * pero sin bandas — acá se implementa como VWAP de ventana móvil (rolling, no
 * anclado a sesión) con bandas Superior/Inferior = VWAP ± N desviaciones estándar
 * del precio respecto al propio VWAP, ponderadas por volumen. Herramienta muy usada
 * en trading institucional intradía de índices/futuros.
 * Fuente: https://gocharting.com/docs/orderflow/vwapbands
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (SumCalculator para
 * los acumulados ponderados por volumen, StdDevCalculator para la desviación).
 */
package SQ.Blocks.Indicators.VWAPBands;

import SQ.Calculators.SumCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(VWAPB) VWAP Bands", display="VWAPBands(@Chart@#Period#,#Deviation#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("VWAP con bandas de desviacion estandar (ventana movil, no anclado a sesion). Indicador no nativo agregado a SQX_Library. Requiere volumen real o tick volume como proxy.")
@ParameterSet(set="Period=20,Deviation=2")
public class VWAPBands extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=250, defaultValue="20", step=1)
	public int Period;

	@Parameter(category="Default", name="Deviation", minValue=0.5, maxValue=5, defaultValue="2", step=0.1)
	public double Deviation;

	@Output(name = "VWAP", color = Colors.Blue)
	public DataSeries VWAP;

	@Output(name = "Upper", color = Colors.Red)
	public DataSeries Upper;

	@Output(name = "Lower", color = Colors.Green)
	public DataSeries Lower;

	private SumCalculator sumPV;
	private SumCalculator sumV;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		sumPV = new SumCalculator(Period);
		sumV = new SumCalculator(Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double typical = (Chart.High.get(0) + Chart.Low.get(0) + Chart.Close.get(0)) / 3.0;
		double volume = Chart.Volume.get(0);
		if (volume < 0.0000000001) volume = 1;

		sumPV.onBarUpdate(typical * volume, getCurrentBar());
		sumV.onBarUpdate(volume, getCurrentBar());

		double totalV = sumV.getValue();
		double vwap = (totalV < 0.0000000001) ? typical : sumPV.getValue() / totalV;

		int window = Math.min(getCurrentBar() + 1, Period);
		double weightedSqDiff = 0;
		double weightSum = 0;
		for (int idx = 0; idx < window; idx++) {
			double t = (Chart.High.get(idx) + Chart.Low.get(idx) + Chart.Close.get(idx)) / 3.0;
			double v = Chart.Volume.get(idx);
			if (v < 0.0000000001) v = 1;
			weightedSqDiff += v * (t - vwap) * (t - vwap);
			weightSum += v;
		}
		double variance = (weightSum < 0.0000000001) ? 0 : weightedSqDiff / weightSum;
		double stdDev = Math.sqrt(variance);

		VWAP.set(0, vwap);
		Upper.set(0, vwap + Deviation * stdDev);
		Lower.set(0, vwap - Deviation * stdDev);
	}
}
