/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Relative Volatility Index (Donald Dorsey). Separa la desviación estándar del precio
 * en componente alcista/bajista para medir la DIRECCION de la volatilidad, no solo
 * su magnitud (a diferencia de ATR/StdDev nativos).
 * RVI = 100 * EMA(vol_alcista,N) / (EMA(vol_alcista,N) + EMA(vol_bajista,N))
 * Fuente: https://www.fmlabs.com/reference/RVI.htm
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (StdDevCalculator +
 * AverageCalculator EMA).
 */
package SQ.Blocks.Indicators.RelativeVolatilityIndex;

import SQ.Calculators.StdDevCalculator;
import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(RVOLI) Relative Volatility Index", display="RelativeVolatilityIndex(@Chart@#StdDevPeriod#,#SmoothPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Relative Volatility Index (Dorsey). Mide si la volatilidad reciente esta sesgada al alza o a la baja. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=1)
@ParameterSet(set="StdDevPeriod=10,SmoothPeriod=14")
public class RelativeVolatilityIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="StdDevPeriod", minValue=2, maxValue=200, defaultValue="10", step=1)
	public int StdDevPeriod;

	@Parameter(category="Default", name="SmoothPeriod", minValue=2, maxValue=200, defaultValue="14", step=1)
	public int SmoothPeriod;

	@Output(name = "RVOLI", color = Colors.Red)
	public DataSeries Value;

	private StdDevCalculator stdDevCalc;
	private AverageCalculator upEMA;
	private AverageCalculator downEMA;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		stdDevCalc = new StdDevCalculator(StdDevPeriod);
		upEMA = new AverageCalculator(AverageCalculator.EMA, SmoothPeriod);
		downEMA = new AverageCalculator(AverageCalculator.EMA, SmoothPeriod);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close = Chart.Close.get(0);

		stdDevCalc.onBarUpdate(close, getCurrentBar());
		double stdDev = stdDevCalc.getValue();

		double prevClose = (getCurrentBar() > 0) ? Chart.Close.get(1) : close;

		double upVol = (close > prevClose) ? stdDev : 0;
		double downVol = (close < prevClose) ? stdDev : 0;

		upEMA.onBarUpdate(upVol, getCurrentBar());
		downEMA.onBarUpdate(downVol, getCurrentBar());

		double up = upEMA.getValue();
		double down = downEMA.getValue();
		double total = up + down;

		double rvi = (total < 0.0000000001) ? 50 : 100.0 * up / total;
		Value.set(0, rvi);
	}
}
