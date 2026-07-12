/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Half-life de reversion a la media (proceso Ornstein-Uhlenbeck). Regresion lineal
 * de Delta_precio(t) contra precio(t-1) sobre una ventana movil, para estimar lambda
 * (la velocidad de reversion). half-life = ln(2)/(-lambda) si lambda<0 (mean-reverting);
 * si lambda>=0 no hay reversion detectable y se devuelve un valor "sin reversion" (0,
 * interpretado como N/A por el usuario de la plantilla).
 * Fuente: https://hudson-and-thames-arbitragelab.readthedocs-hosted.com/en/latest/cointegration_approach/half_life.html
 *
 * Escrito siguiendo el patron real de los indicadores nativos (acceso historico
 * Chart.Close.get(idx) como en ChoppinessIndex.java; regresion lineal simple
 * calculada a mano, no existe un calculador de regresion entre los Calculators nativos).
 */
package SQ.Blocks.Indicators.HalfLifeMeanReversion;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(HALFLIFE) Half-Life de Reversion a la Media", display="HalfLifeMeanReversion(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Half-life de reversion a la media (Ornstein-Uhlenbeck). Cuantas barras tarda en promedio una desviacion del precio respecto a su media en reducirse a la mitad. 0 = no se detecta reversion en la ventana actual. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=10, min=0, max=100, step=1)
@ParameterSet(set="Period=50")
public class HalfLifeMeanReversion extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=10, maxValue=500, defaultValue="50", step=1)
	public int Period;

	@Output(name = "HalfLife", color = Colors.Red)
	public DataSeries Value;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		if (getCurrentBar() < Period + 1) {
			Value.set(0, 0);
			return;
		}

		// y(t-1) = Chart.Close.get(idx+1) ; deltaY(t) = Chart.Close.get(idx) - Chart.Close.get(idx+1)
		int n = Period;

		double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
		for (int idx = 0; idx < n; idx++) {
			double yPrev = Chart.Close.get(idx + 1);
			double deltaY = Chart.Close.get(idx) - yPrev;

			sumX += yPrev;
			sumY += deltaY;
			sumXY += yPrev * deltaY;
			sumXX += yPrev * yPrev;
		}

		double meanX = sumX / n;
		double meanY = sumY / n;

		double num = sumXY - n * meanX * meanY;
		double denom = sumXX - n * meanX * meanX;

		double lambda = (Math.abs(denom) < 0.0000000001) ? 0 : num / denom;

		double halfLife;
		if (lambda < -0.0000000001) {
			halfLife = Math.log(2.0) / (-lambda);
		} else {
			halfLife = 0;
		}

		if (halfLife > 500) halfLife = 500;

		Value.set(0, halfLife);
	}
}
