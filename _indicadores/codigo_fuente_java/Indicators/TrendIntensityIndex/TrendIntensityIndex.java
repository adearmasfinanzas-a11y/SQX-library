/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Trend Intensity Index (M.H. Pee). Sobre una SMA larga (60), mide qué porcentaje
 * de los cierres de las últimas 30 barras quedó por encima (deviación alcista) vs.
 * por debajo (deviación bajista) de esa media: TII = totalUp / (totalUp + totalDown) * 100.
 * Base matemática distinta de ADX: cuenta ocurrencias relativas a una media larga,
 * no suaviza movimiento direccional.
 * Fuente: https://www.stockmaniacs.net/trend-intensity-index-indicator/
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator SMA,
 * acceso histórico Chart.Close.get(idx)).
 */
package SQ.Blocks.Indicators.TrendIntensityIndex;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(TII) Trend Intensity Index", display="TrendIntensityIndex(@Chart@#SmaPeriod#,#CountPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Trend Intensity Index (Pee). Porcentaje de cierres recientes por encima vs. por debajo de una SMA larga. Mayor a 80 = tendencia alcista fuerte, menor a 20 = bajista fuerte. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=1)
@ParameterSet(set="SmaPeriod=60,CountPeriod=30")
public class TrendIntensityIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="SmaPeriod", minValue=10, maxValue=250, defaultValue="60", step=1)
	public int SmaPeriod;

	@Parameter(category="Default", name="CountPeriod", minValue=5, maxValue=125, defaultValue="30", step=1)
	public int CountPeriod;

	@Output(name = "TII", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator smaCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		smaCalc = new AverageCalculator(AverageCalculator.SMA, SmaPeriod);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		smaCalc.onBarUpdate(Chart.Close.get(0), getCurrentBar());
		double sma = smaCalc.getValue();

		int window = Math.min(getCurrentBar() + 1, CountPeriod);

		double totalUp = 0, totalDown = 0;
		for (int idx = 0; idx < window; idx++) {
			double close = Chart.Close.get(idx);
			if (close > sma) {
				totalUp += (close - sma);
			} else {
				totalDown += (sma - close);
			}
		}

		double denom = totalUp + totalDown;
		double tii = (denom < 0.0000000001) ? 50 : 100.0 * totalUp / denom;

		Value.set(0, tii);
	}
}
