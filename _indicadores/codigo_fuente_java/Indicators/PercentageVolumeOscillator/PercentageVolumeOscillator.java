/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Percentage Volume Oscillator (PVO). Analogo del PPO (que es nativo, aplicado a
 * precio) pero aplicado a VOLUMEN: (EMA_rapida(Volumen) - EMA_lenta(Volumen)) /
 * EMA_lenta(Volumen) * 100. PPO nativo no puede reutilizarse directamente porque
 * su Input está atado conceptualmente a precio en el catálogo — se implementa el
 * equivalente explícito para volumen.
 * Fuente: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/percentage-volume-oscillator-pvo
 *
 * Escrito siguiendo el patrón real de los indicadores nativos (AverageCalculator EMA,
 * misma estructura que MACD.java pero acceso a Chart.Volume.get(idx)).
 */
package SQ.Blocks.Indicators.PercentageVolumeOscillator;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(PVO) Percentage Volume Oscillator", display="PercentageVolumeOscillator(@Chart@#FastPeriod#,#SlowPeriod#,#SignalPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Percentage Volume Oscillator. Version del PPO aplicada a volumen en vez de precio. Indicador no nativo agregado a SQX_Library. Requiere volumen real o tick volume como proxy.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="FastPeriod=12,SlowPeriod=26,SignalPeriod=9")
public class PercentageVolumeOscillator extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="FastPeriod", minValue=2, maxValue=200, defaultValue="12", step=1)
	public int FastPeriod;

	@Parameter(category="Default", name="SlowPeriod", minValue=2, maxValue=200, defaultValue="26", step=1)
	public int SlowPeriod;

	@Parameter(category="Default", name="SignalPeriod", minValue=2, maxValue=200, defaultValue="9", step=1)
	public int SignalPeriod;

	@Output(name = "PVO", color = Colors.Red)
	public DataSeries Value;

	@Output(name = "Signal", color = Colors.Blue)
	public DataSeries Signal;

	private AverageCalculator fastEMA;
	private AverageCalculator slowEMA;
	private AverageCalculator signalCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		fastEMA = new AverageCalculator(AverageCalculator.EMA, FastPeriod);
		slowEMA = new AverageCalculator(AverageCalculator.EMA, SlowPeriod);
		signalCalc = new AverageCalculator(AverageCalculator.SMA, SignalPeriod);
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double volume = Chart.Volume.get(0);

		fastEMA.onBarUpdate(volume, getCurrentBar());
		slowEMA.onBarUpdate(volume, getCurrentBar());

		double slow = slowEMA.getValue();
		double pvo = (Math.abs(slow) < 0.0000000001) ? 0 : 100.0 * (fastEMA.getValue() - slow) / slow;

		signalCalc.onBarUpdate(pvo, getCurrentBar());

		Value.set(0, pvo);
		Signal.set(0, signalCalc.getValue());
	}
}
