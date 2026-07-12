/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Chaikin Volatility (Marc Chaikin). Cambio porcentual de una EMA(10) del rango
 * Alto-Bajo, comparada contra su valor N barras atrás — mide la TASA DE CAMBIO de
 * la volatilidad, no su nivel absoluto (a diferencia de ATR) ni su aceleración vía
 * ratio (a diferencia de Mass Index).
 * Fuente: https://www.stockmaniacs.net/chaikin-volatility-indicator/
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator EMA
 * + un buffer circular propio para guardar el historial exacto de la EMA, en vez de
 * reconstruirla por aproximación).
 */
package SQ.Blocks.Indicators.ChaikinVolatility;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(CVOL) Chaikin Volatility", display="ChaikinVolatility(@Chart@#EmaPeriod#,#RocPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Chaikin Volatility. Tasa de cambio porcentual de una EMA del rango Alto-Bajo. Mide si la volatilidad esta acelerando o desacelerando. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="EmaPeriod=10,RocPeriod=10")
public class ChaikinVolatility extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="EmaPeriod", minValue=2, maxValue=100, defaultValue="10", step=1)
	public int EmaPeriod;

	@Parameter(category="Default", name="RocPeriod", minValue=2, maxValue=100, defaultValue="10", step=1)
	public int RocPeriod;

	@Output(name = "CVOL", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator emaCalc;
	private double[] emaHistoryBuffer;
	private int bufferSize;
	private int writeIndex = 0;
	private int filledCount = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		emaCalc = new AverageCalculator(AverageCalculator.EMA, EmaPeriod);
		bufferSize = RocPeriod + 1;
		emaHistoryBuffer = new double[bufferSize];
		writeIndex = 0;
		filledCount = 0;
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double range = Chart.High.get(0) - Chart.Low.get(0);
		emaCalc.onBarUpdate(range, getCurrentBar());
		double emaNow = emaCalc.getValue();

		// Guardar el valor actual de la EMA en el buffer circular
		emaHistoryBuffer[writeIndex] = emaNow;

		double cvol = 0;
		if (filledCount >= RocPeriod) {
			int pastIndex = (writeIndex - RocPeriod + bufferSize) % bufferSize;
			double emaPast = emaHistoryBuffer[pastIndex];
			cvol = (Math.abs(emaPast) < 0.0000000001) ? 0 : 100.0 * (emaNow - emaPast) / emaPast;
		}

		Value.set(0, cvol);

		writeIndex = (writeIndex + 1) % bufferSize;
		if (filledCount < bufferSize) filledCount++;
	}
}
