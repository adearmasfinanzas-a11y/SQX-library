/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Hurst Exponent, via analisis de Rango Reescalado (R/S) simplificado. H<0.5 = serie
 * con reversion a la media, H=0.5 = camino aleatorio, H>0.5 = serie con tendencia
 * persistente. Implementacion PRACTICA simplificada: calcula R/S sobre 3 tamaños de
 * sub-ventana dentro de la ventana total (N/4, N/2, N) y estima la pendiente log-log
 * por regresion de esos 3 puntos — no es un estimador R/S multi-escala completo
 * (que usaria muchas más sub-ventanas), es un compromiso practico para backtesting.
 * Fuente: https://macrosynergy.com/research/detecting-trends-and-mean-reversion-with-the-hurst-exponent/
 *
 * Escrito siguiendo el patron real de los indicadores nativos (acceso historico
 * Chart.Close.get(idx) como en ChoppinessIndex.java).
 */
package SQ.Blocks.Indicators.HurstExponent;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(HURST) Hurst Exponent", display="HurstExponent(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Hurst Exponent (R/S simplificado). H menor a 0.5 = regimen de reversion a la media, H mayor a 0.5 = regimen de tendencia. Indicador no nativo agregado a SQX_Library. Implementacion practica simplificada, ver comentario en el codigo fuente.")
@Indicator(oscillator=true, middleValue=0.5, min=0, max=1, step=0.01)
@ParameterSet(set="Period=100")
public class HurstExponent extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=20, maxValue=500, defaultValue="100", step=1)
	public int Period;

	@Output(name = "Hurst", color = Colors.Red)
	public DataSeries Value;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
	}

	//------------------------------------------------------------------------

	// R/S sobre una sub-ventana de "size" barras, empezando "size" barras atras desde el shift dado
	private double rescaledRange(int size) {
		double mean = 0;
		for (int idx = 0; idx < size; idx++) {
			mean += Chart.Close.get(idx);
		}
		mean /= size;

		double[] deviationSum = new double[size];
		double cumulative = 0;
		double sumSq = 0;
		for (int idx = size - 1; idx >= 0; idx--) {
			double dev = Chart.Close.get(idx) - mean;
			cumulative += dev;
			int pos = size - 1 - idx;
			deviationSum[pos] = cumulative;
			sumSq += dev * dev;
		}

		double maxCum = Double.NEGATIVE_INFINITY, minCum = Double.POSITIVE_INFINITY;
		for (int i = 0; i < size; i++) {
			if (deviationSum[i] > maxCum) maxCum = deviationSum[i];
			if (deviationSum[i] < minCum) minCum = deviationSum[i];
		}

		double range = maxCum - minCum;
		double stdDev = Math.sqrt(sumSq / size);

		return (stdDev < 0.0000000001) ? 0 : range / stdDev;
	}

	@Override
	protected void OnBarUpdate() throws TradingException {
		if (getCurrentBar() < Period) {
			Value.set(0, 0.5);
			return;
		}

		int n1 = Math.max(10, Period / 4);
		int n2 = Math.max(n1 + 1, Period / 2);
		int n3 = Period;

		double rs1 = rescaledRange(n1);
		double rs2 = rescaledRange(n2);
		double rs3 = rescaledRange(n3);

		if (rs1 <= 0 || rs2 <= 0 || rs3 <= 0) {
			Value.set(0, 0.5);
			return;
		}

		double x1 = Math.log(n1), y1 = Math.log(rs1);
		double x2 = Math.log(n2), y2 = Math.log(rs2);
		double x3 = Math.log(n3), y3 = Math.log(rs3);

		double meanX = (x1 + x2 + x3) / 3.0;
		double meanY = (y1 + y2 + y3) / 3.0;

		double num = (x1 - meanX) * (y1 - meanY) + (x2 - meanX) * (y2 - meanY) + (x3 - meanX) * (y3 - meanY);
		double denom = (x1 - meanX) * (x1 - meanX) + (x2 - meanX) * (x2 - meanX) + (x3 - meanX) * (x3 - meanX);

		double hurst = (denom < 0.0000000001) ? 0.5 : num / denom;

		if (hurst < 0) hurst = 0;
		if (hurst > 1) hurst = 1;

		Value.set(0, hurst);
	}
}
