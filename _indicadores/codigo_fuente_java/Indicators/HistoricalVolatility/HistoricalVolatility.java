/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Historical Volatility (HV). Desviación estándar de los log-retornos (cierre a
 * cierre) sobre N barras, anualizada. Distinto de ATR (basado en rango intrabarra,
 * no en retornos) y de StdDev nativo (que mide dispersión de precio absoluto, no
 * de retornos porcentuales) — es la medida de volatilidad estándar en literatura
 * de opciones/derivados.
 * Fuente: concepto estándar de volatilidad histórica (log-retornos anualizados)
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (StdDevCalculator
 * aplicado a una serie de log-retornos calculada a mano).
 */
package SQ.Blocks.Indicators.HistoricalVolatility;

import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(HV) Historical Volatility", display="HistoricalVolatility(@Chart@#Period#,#BarsPerYear#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Historical Volatility. Desviacion estandar de log-retornos, anualizada. Medida de volatilidad basada en retornos, distinta de ATR (rango) y StdDev de precio absoluto. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=10, min=0, max=100, step=0.1)
@ParameterSet(set="Period=20,BarsPerYear=252")
public class HistoricalVolatility extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=2, maxValue=250, defaultValue="20", step=1)
	public int Period;

	@Parameter(category="Default", name="BarsPerYear", minValue=1, maxValue=100000, defaultValue="252", step=1)
	public int BarsPerYear;

	@Output(name = "HV", color = Colors.Red)
	public DataSeries Value;

	private StdDevCalculator stdDevCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		stdDevCalc = new StdDevCalculator(Period);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);
		double prevClose = (getCurrentBar() > 0) ? Chart.Close.get(1) : close;

		double logReturn = (prevClose > 0 && close > 0) ? Math.log(close / prevClose) : 0;

		stdDevCalc.onBarUpdate(logReturn, getCurrentBar());
		double stdDev = stdDevCalc.getValue();

		double hv = 100.0 * stdDev * Math.sqrt(BarsPerYear);
		Value.set(0, hv);
	}
}
