/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Klinger Volume Oscillator (Stephen Klinger). EMA(34) - EMA(55) de una "Fuerza de
 * Volumen" que pondera el volumen segun si el precio tipico subio o bajo respecto a
 * la barra anterior. Version simplificada y fiel al espiritu del indicador original
 * (las fuentes secundarias varian levemente en el detalle exacto de la formula de
 * acumulacion "cm" — se documenta explicitamente la simplificacion adoptada).
 * Fuente: https://medium.com/@blackcat1402.tradingview/klinger-oscillator-unveiling-market-pulsations-72156b3424ad
 *
 * Escrito siguiendo el patron real de los indicadores nativos (AverageCalculator EMA,
 * acceso a Chart.Volume.get(idx) como en AvgVolume.java).
 */
package SQ.Blocks.Indicators.KlingerVolumeOscillator;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(KVO) Klinger Volume Oscillator", display="KlingerVolumeOscillator(@Chart@#FastPeriod#,#SlowPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Klinger Volume Oscillator. EMA rapida - EMA lenta de una Fuerza de Volumen ponderada por la direccion del precio tipico. Indicador no nativo agregado a SQX_Library. Requiere volumen real o tick volume como proxy.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="FastPeriod=34,SlowPeriod=55")
public class KlingerVolumeOscillator extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="FastPeriod", minValue=2, maxValue=200, defaultValue="34", step=1)
	public int FastPeriod;

	@Parameter(category="Default", name="SlowPeriod", minValue=2, maxValue=200, defaultValue="55", step=1)
	public int SlowPeriod;

	@Output(name = "KVO", color = Colors.Red)
	public DataSeries Value;

	private AverageCalculator fastEMA;
	private AverageCalculator slowEMA;

	private double prevTypical = 0;
	private int prevTrend = 1;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		fastEMA = new AverageCalculator(AverageCalculator.EMA, FastPeriod);
		slowEMA = new AverageCalculator(AverageCalculator.EMA, SlowPeriod);
		prevTypical = 0;
		prevTrend = 1;
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double h = Chart.High.get(0);
		double l = Chart.Low.get(0);
		double c = Chart.Close.get(0);
		double v = Chart.Volume.get(0);

		double typical = (h + l + c) / 3.0;

		int trend = (getCurrentBar() == 0) ? 1 : ((typical > prevTypical) ? 1 : -1);

		double dm = h - l;
		double volumeForce = v * trend * dm;

		fastEMA.onBarUpdate(volumeForce, getCurrentBar());
		slowEMA.onBarUpdate(volumeForce, getCurrentBar());

		double kvo = fastEMA.getValue() - slowEMA.getValue();
		Value.set(0, kvo);

		prevTypical = typical;
		prevTrend = trend;
	}
}
